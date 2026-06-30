package com.osigie.erecall.util;

import com.osigie.erecall.dto.AuthDTO.*;

public class TestDataFactory {

    public static RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("osigie");
        request.setPassword("Osigie@88");
        request.setEmail("kenosagie88@gmail.com");
        return request;

    }
}
