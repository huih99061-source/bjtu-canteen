package com.bjtu.canteen.model;

/**
 * 统一API响应格式
 */
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }
}
