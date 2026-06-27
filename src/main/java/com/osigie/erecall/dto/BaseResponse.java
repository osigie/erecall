package com.osigie.erecall.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BaseResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder().data(data).message("Success").success(true).build();
    }


    public static <T> BaseResponse<T> failure(String message) {
        return BaseResponse.<T>builder().message(message).success(false).build();
    }
}
