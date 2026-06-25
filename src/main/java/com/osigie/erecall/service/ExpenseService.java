package com.osigie.erecall.service;

import java.util.UUID;

public interface ExpenseService {
    String query(String text, UUID userId);
}
