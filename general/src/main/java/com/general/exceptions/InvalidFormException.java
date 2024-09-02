package com.general.exceptions;

/**
 * Выбрасывается, если в форме создан невалидный объект.
 */

public class InvalidFormException extends Exception {
    public InvalidFormException() {
        super();
    }

    public InvalidFormException(String message) {
        super(message);
    }
}