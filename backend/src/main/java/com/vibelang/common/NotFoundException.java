package com.vibelang.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, Long id) {
        return new NotFoundException(entity + " " + id + " not found");
    }
}
