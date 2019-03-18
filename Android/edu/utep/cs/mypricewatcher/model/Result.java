package edu.utep.cs.mypricewatcher.model;

public class Result<T> {

    public final T value;
    public final String error;
    public final int code;
    public Object tag;

    public Result(T value, String error) {
        this(value, error, 0);
    }

    public Result(String error, int code) {
        this(null, error, code);
    }

    public Result(T value, String error, int code) {
        this.value = value;
        this.error = error;
        this.code = code;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isValue() {
        return error == null;
    }

    public boolean hasErrorCode() {
        return code != 0;
    }

    public static <T> Result<T> value(T value) {
        return new Result(value, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result(null, msg);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result(msg, code);
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object tag() {
        return tag;
    }

    public boolean hasTag() {
        return tag != null;
    }
}
