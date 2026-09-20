package com.example.demo.exception;

public class PersonImportException extends RuntimeException {

    public PersonImportException(String message) {
        super(message);
    }

    public PersonImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
