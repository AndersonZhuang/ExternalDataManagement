package com.diit.ExternelDataManagement.common;

import java.time.LocalDateTime;

public class APIResponse<T> {

    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public APIResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public APIResponse(ResponseCode responseCode) {
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
        this.timestamp = LocalDateTime.now();
    }

    public APIResponse(ResponseCode responseCode, T data) {
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public APIResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public APIResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> APIResponse<T> ok() {
        return new APIResponse<>(ResponseCode.OK);
    }

    public static <T> APIResponse<T> ok(T data) {
        return new APIResponse<>(ResponseCode.OK, data);
    }

    public static <T> APIResponse<T> ok(String message, T data) {
        return new APIResponse<>(ResponseCode.OK.getCode(), message, data);
    }

    public static <T> APIResponse<T> failed(String message) {
        return new APIResponse<>(ResponseCode.FAILED.getCode(), message);
    }

    public static <T> APIResponse<T> error(String message) {
        return new APIResponse<>(ResponseCode.ERROR.getCode(), message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "APIResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}