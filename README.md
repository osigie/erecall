# erecall

An intelligent expense tracking system powered by Gemini AI. Submit expenses via text or document (PDF/JPEG/PNG), and the AI automatically extracts merchant, amount, category, and description. Ask questions about your spending in plain English — "how much did I spend on dining last month?" or "what did I buy at Amazon?" — and get answers instantly.

## How It Works

The system works in three phases:

1. **Submit**: Send a text description ("dinner 50 at McDonald's") or upload a receipt file — the system returns a document ID immediately
2. **Process**: Gemini AI extracts structured expense data in the background. Poll the status endpoint to check when processing completes
3. **Ask**: Query your expenses in natural language. Gemini translates your question into structured searches across Postgres (filters, date ranges) and Qdrant (semantic vector search) and returns a plain-English answer

Expenses are searchable both by structured fields (merchant, amount, category, date range) and by natural language ("things I bought at the grocery store"), using a hybrid of Postgres queries and Qdrant vector similarity search.

## Prerequisites

- Java 21
- Maven 3.9+
- Google Gemini API key ([get one here](https://ai.google.dev/))
- An S3-compatible storage bucket
- Docker (for local development with testcontainers)

## Installation

```bash
# Clone and build
git clone https://github.com/osigie/erecall.git
cd erecall
./mvnw clean package -DskipTests
```

## Configuration

### Environment Variables

```bash
# Database (Postgres)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/erecall"
export SPRING_DATASOURCE_USERNAME="erecall"
export SPRING_DATASOURCE_PASSWORD="your-password"

# Gemini AI
export GEMINI_API_KEY="your-gemini-api-key"
export GEMINI_PROJECT_ID="your-gcp-project-id"

# JWT
export JWT_SECRET="your-256-bit-secret"

# S3-compatible storage
export S3_ENDPOINT="https://your-s3-endpoint"
export S3_REGION="us-east-1"
export S3_ACCESS_KEY="your-access-key"
export S3_SECRET_KEY="your-secret-key"
export S3_BUCKET_NAME="erecall-uploads"
```

## Usage

### Start the Application

```bash
# Using Maven
./mvnw spring-boot:run
```

Or with Docker:

```bash
# 1. Copy and fill in environment variables
cp .env.example .env

# 2. Build the Docker image with Jib
./mvnw compile jib:dockerBuild

# 3. Start all services
docker compose up
```

### API Endpoints

#### Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Pass123!", "username": "jdoe"}'
```

#### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Pass123!"}'
```

Returns a JWT access token used to authenticate subsequent requests.

#### Upload a File

```bash
curl -X POST http://localhost:8080/api/v1/expenses/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@receipt.pdf"
```

Returns a file key to use in the submit endpoint. Accepted formats: PDF, JPEG, PNG. Max size: 5MB.

#### Submit an Expense

```bash
# Text-only
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rawText": "dinner 50 at McDonald\'s"}'

# With a file
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rawText": "lunch receipt", "fileUrl": "uploads/<key>.pdf"}'
```

Returns a document ID and initial status (`PROCESSING`).

#### Check Status

```bash
curl http://localhost:8080/api/v1/expenses/<documentId>/status \
  -H "Authorization: Bearer <token>"
```

Returns the current processing status (`PROCESSING`, `PROCESSED`, `FAILED`) and the extracted expense data when complete.

#### Get Current User

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <token>"
```

### Ask Questions About Your Spending

Once expenses are processed, you can ask questions in natural language. Submit a text description and Gemini will search your expenses and return an answer:

```bash
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"rawText": "how much did I spend on groceries last month?"}'
```

The AI understands queries like:
- "how much did I spend on dining out?"
- "what did I buy at Shell?"
- "show me my entertainment expenses from January"
- "things I bought at the grocery store"

It uses structured database queries for filters and date ranges, and vector search for semantic matching — returning a plain-English summary.

## Development

### Running Tests and Checks

```bash
# Run tests, formatting checks, and code style in one command
./mvnw clean verify
```

`verify` runs the full lifecycle: formatting checks (Spotless), code style (Checkstyle), unit tests, integration tests, and JAR packaging.

To auto-fix formatting:

```bash
./mvnw spotless:apply
```

### Code Standards

This implementation follows:

- Java 21 with modern language features (records, pattern matching `instanceof`, text blocks)
- Spring Boot 4.x with constructor-based dependency injection
- Interface-based service design for testability
- Tool-based AI integration via Spring AI
- 2-space indentation (enforced by Spotless)
- Google Java Style via Checkstyle

### CI/CD

GitHub Actions (`.github/workflows/ci.yml`) runs on every push/PR to `main`:

- **verify**: `./mvnw -B verify` — runs Spotless, Checkstyle, tests, and packaging on JDK 21
- **publish**: On push to main (after verify passes), builds and pushes Docker image to Docker Hub via Jib

### Docker

This project uses Google Jib for containerization — no Dockerfile required:

```bash
# Build to local Docker daemon
./mvnw compile jib:dockerBuild

# Build and run with docker-compose
./mvnw compile jib:dockerBuild && docker compose up
```

## Architecture

The system uses a layered architecture with async document processing:

- **Controller layer**: REST endpoints for auth, expense submission, file upload, and status polling
- **Service layer**: Business logic — `AuthServiceImpl` (JWT auth), `ExpenseServiceImpl` (document management), `ExpenseToolServiceImpl` (AI tool functions), `S3FileStorageService` (file storage)
- **AI layer**: Gemini integration via Spring AI tool-calling — the model calls `saveExpense`, `getExpensesByDateRange`, `searchDatabase`, and `searchVector` tools
- **Storage layer**: Postgres (structured data), Qdrant (vector embeddings for semantic search), S3 (file storage)
- **Event layer**: Async processing via `DocumentSavedEvent` → `DocumentProcessingService` — the submit endpoint returns immediately, processing happens in the background

### Data Flow

```
POST /expenses → save document (PROCESSING) → publish DocumentSavedEvent → return
                                                    ↓ (async)
                                         DocumentProcessingService
                                           ├─ text → Gemini LLM
                                           └─ file → pre-signed URL → Gemini Vision
                                                    ↓
                                         save expense to Postgres + Qdrant
                                         update status to PROCESSED / FAILED
```

See [TRADEOFFS.md](TRADEOFFS.md) for detailed architectural decisions and design rationale.

## License

Proprietary. All rights reserved.
