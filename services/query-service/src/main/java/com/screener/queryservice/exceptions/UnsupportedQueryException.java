package com.screener.queryservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedQueryException extends RuntimeException {
    public UnsupportedQueryException(String message) {
        super(message);
    }
}
