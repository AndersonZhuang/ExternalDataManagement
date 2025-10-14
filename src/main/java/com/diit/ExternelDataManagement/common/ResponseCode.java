package com.diit.ExternelDataManagement.common;

public enum ResponseCode {

    OK(200, "操作成功"),
    FAILED(400, "操作失败"),
    ERROR(500, "系统错误");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}