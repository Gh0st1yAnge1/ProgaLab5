package org.example.exceptions;

public class InputCancelledException extends RuntimeException {
    public InputCancelledException(String message) {
        super(message);
    }
}
