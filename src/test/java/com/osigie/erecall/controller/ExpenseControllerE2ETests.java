package com.osigie.erecall.controller;

import com.osigie.erecall.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ExpenseControllerE2ETests extends AbstractIntegrationTest {
}
