package io.bugreaper.modules.minio.exceptions;

/**
 * Exceptions that can be thrown during Minio helper use
 */
public class MinioHelperException extends RuntimeException {

    /**
     * Constructs a new exception with the specified cause
     *
     * @param cause the cause
     */
    public MinioHelperException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message
     *
     * @param message String with message
     */
    public MinioHelperException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message String with message
     * @param cause the cause
     */
    public MinioHelperException(String message, Throwable cause) {
        super(message, cause);
    }

}
