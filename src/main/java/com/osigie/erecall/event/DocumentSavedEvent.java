package com.osigie.erecall.event;

import java.util.UUID;

public record DocumentSavedEvent(UUID documentId, UUID userId) {}
