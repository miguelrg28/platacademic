package edu.pucmm.icc352.events.shared.error;

public final class AppException extends RuntimeException {
    private final int statusCode;
    private final String code;

    private AppException(int statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public static AppException badRequest(String message) {
        return new AppException(400, "bad_request", message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, "unauthorized", message);
    }

    public static AppException forbidden(String message) {
        return new AppException(403, "forbidden", message);
    }

    public static AppException notFound(String message) {
        return new AppException(404, "not_found", message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, "conflict", message);
    }

    public int statusCode() {
        return statusCode;
    }

    public String code() {
        return code;
    }
}
