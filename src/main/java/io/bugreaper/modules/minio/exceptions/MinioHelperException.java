package io.bugreaper.modules.minio.exceptions;

public class MinioHelperException extends RuntimeException {

    public MinioHelperException(Throwable cause) {
        super(cause);
    }

    public MinioHelperException(String message) {
        super(message);
    }

    public MinioHelperException(String message, Throwable cause) {
        super(message, cause);
    }

}
