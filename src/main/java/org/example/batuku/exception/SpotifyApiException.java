package org.example.batuku.exception;

public class SpotifyApiException extends RuntimeException {

    private final int httpStatus;
    private final long retryAfterMs;

    public SpotifyApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.retryAfterMs = 0;
    }

    public SpotifyApiException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryAfterMs = 0;
    }

    public SpotifyApiException(String message, int httpStatus, long retryAfterMs, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryAfterMs = retryAfterMs;
    }

    public int getHttpStatus() { return httpStatus; }
    public long getRetryAfterMs() { return retryAfterMs; }
}
