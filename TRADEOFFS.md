# Technical Tradeoffs & Design Decisions

## Language & Framework: Java 21 + Spring Boot 4.x

I chose Java 21 for its modern language features (records, pattern matching `instanceof`, text blocks) combined with the maturity of the Spring ecosystem. Spring Boot 4.x provides virtual threads support, improved observability, and a streamlined configuration model.

The downside is startup time and memory footprint compared to lighter frameworks. Spring Boot applications are not cold-start friendly, and the dependency overhead is significant for what is fundamentally a CRUD + AI integration service. Micronaut or Quarkus would offer faster startup and lower memory, but Spring's ecosystem depth — particularly Spring AI and Spring Security — made it the pragmatic choice.

## Architecture: Async Document Processing with Events

I designed the expense submission flow to return immediately with a `PROCESSING` status while Gemini AI processes the content in the background via `@TransactionalEventListener` and `@Async`. The client polls `/status` to check completion.

This decouples API responsiveness from AI inference latency. Gemini calls can take 5–30 seconds depending on payload size and model load. Blocking the HTTP thread would exhaust connection pools and degrade throughput. The tradeoff is state management complexity: documents exist in an intermediate state (`PROCESSING`) and clients must implement polling. An alternative would be webhook callbacks, but that adds infrastructure requirements (callback endpoint, retry logic) unsuitable for the current deployment model.

## AI Integration: Tool-based Architecture via Spring AI

Instead of parsing free-form Gemini output, the model calls back into the application through Spring AI tools (`saveExpense`, `searchDatabase`, `searchVector`, `getExpensesByDateRange`). The AI decides when to invoke each tool based on the system prompt.

This shifts parsing responsibility from application code to the model. It handles variations in user input naturally — "dinner 50" and "I spent 50 dollars on dinner" both result in the same `saveExpense` call with structured parameters. The tradeoff is dependency on Gemini's tool-calling reliability. If the model fails to call the right tool or passes incorrect parameters, the extraction fails. The system prompt and tool descriptions must be carefully crafted, and the `FAILED` status exists precisely because AI calls can produce unexpected results.

## Storage: Vector + Relational Hybrid

I use Postgres for structured queries (filter by category, merchant, amount, date range) and Qdrant for semantic search over expense descriptions. This dual-storage approach provides both precise filtering and fuzzy natural-language search.

The tradeoff is operational complexity. Two databases mean two connection pools, two schema migrations, and two backup strategies. There's also an eventual consistency concern: when an expense is saved, it's written to Postgres in the same transaction, but the Qdrant vector upsert happens after the transaction commits. A crash between the commit and the vector write would leave the expense in Postgres but missing from semantic search results. For the expense tracking use case, this is acceptable — structured search still works, and the inconsistency is temporary.

## File Storage: S3 with Pre-signed URLs

Files (receipts, invoices) are uploaded to a private S3 bucket. When Gemini needs to read a file, the application generates a pre-signed URL with a 5-minute expiration.

This keeps the bucket private while granting time-limited access to the AI model. The tradeoff is extra latency: the AI must fetch the file over HTTP before analyzing it. An alternative would be to base64-encode the file and include it in the Gemini API call directly, avoiding the network hop. I chose pre-signed URLs because base64 encoding large files (up to 5MB) inflates payload size by ~33% and complicates the Gemini call structure. The pre-signed URL approach also keeps the application layer agnostic of file content — it never handles raw bytes beyond validation.

## Authentication: Stateless JWT

I use JWT for stateless authentication. The token encodes the user ID, email, and role, verified by the application's signing key on every request. No server-side session storage is needed.

Stateless auth scales horizontally without session replication. Any instance can verify any token. The tradeoff is that token revocation requires additional infrastructure (a denylist or short expiry). Currently, tokens expire after 7 days — no revocation mechanism exists. For the current scale this is acceptable, but it's a known gap. Password rotation, account suspension, or compromised-token scenarios are not handled. A token refresh flow with short-lived access tokens (15 minutes) and longer-lived refresh tokens would mitigate this, at the cost of increased complexity on the client side.

## Containerization: Jib over Dockerfile

I use Google Jib to build Docker images directly from Maven, without a Dockerfile. Jib optimizes image layering — dependencies, resources, and classes each get their own layer — producing smaller, faster-to-build images.

Jib eliminates the need to manage a separate Dockerfile and avoids the "build JAR locally, copy into Docker" workflow. The tradeoff is reduced transparency. Jib's image structure is implicit; debugging layer issues requires understanding Jib's internal conventions. A Dockerfile is more explicit and universally understood. For teams familiar with Jib, this is a non-issue — for newcomers, it's an additional concept to learn.

## Spring Boot 4.x: Jackson 3 Migration

Spring Boot 4.x ships with Jackson 3.x under the `tools.jackson` package, while Jackson 2.x (`com.fasterxml.jackson`) remains available for backward compatibility. The application uses Jackson 3.x for its primary serialization, matching Spring Boot's auto-configuration.

The tradeoff is ecosystem fragmentation. Libraries like `jjwt-jackson` still depend on Jackson 2.x, resulting in both versions on the classpath. This works but adds complexity — developers must be aware of which Jackson version each component uses. Test configurations must explicitly create `ObjectMapper` instances rather than relying on auto-configuration, since Spring Boot only auto-configures its primary Jackson version.

## Testing: Testcontainers with Shared Containers

Integration tests use Testcontainers to spin up Postgres and Qdrant containers. Test classes share a single pair of containers via static fields in a common base class (`AbstractIntegrationTest`).

Sharing containers across test classes reduces total test time dramatically. Starting Postgres and Qdrant for each test class would add 30–60 seconds per class. The tradeoff is test isolation: state leaks between test classes since they share the same database. Each `@BeforeEach` method must clean up its own data in the correct order (expense documents before users, to respect foreign keys). With fully isolated containers, cleanup would be automatic but much slower.

## AI Integration: Natural Language Q&A over Expenses

Beyond extraction, the system answers free-form questions about spending. The user submits a question like "how much did I spend on groceries last month?" and Gemini translates it into tool calls — `getExpensesByDateRange` for the date filter, `searchVector` for semantic matching, and `searchDatabase` for structured lookups.

This creates a conversational interface over structured data. Users don't need to learn a query language or navigate a dashboard. The tradeoff is latency: each question requires a Gemini round-trip, typically 3–8 seconds. The answer is also non-deterministic — the same question may produce slightly different formulations, though the underlying data is consistent. For quick lookup use cases, a traditional API with query parameters would be faster but less flexible.

The question is processed through the same async document pipeline as expense submissions. This means questions are also subject to the PROCESSING → PROCESSED/FAILED lifecycle and require status polling. An alternative would be a dedicated synchronous endpoint for queries, which would simplify the client experience but block on AI latency. For now, the unified pipeline keeps the architecture consistent, even though simple lookups would benefit from a direct path.

## Error Handling: Service-layer Validation

I validate expense submissions (at least one of `rawText` or `fileUrl` must be provided) in the service layer, not in the DTO or controller. The DTO is a plain record with no validation annotations.

This keeps the controller thin and the validation logic co-located with the business rules that depend on it. The tradeoff is that validation errors surface as 400 responses with a generic error body rather than the detailed field-level errors that `@Valid` + `MethodArgumentNotValidException` would provide. For a single-field conditional rule ("at least one of A or B"), this is appropriate. For complex multi-field validation, a dedicated validator class would be cleaner.
