package com.examplatform.asset.validation;

/**
 * Thrown when an uploaded asset fails security or format validation.
 */
public class AssetValidationException extends RuntimeException {

    public AssetValidationException(String message) {
        super(message);
    }

    public AssetValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
