package com.pathfinder.calbak.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage();

        if ("존재하지 않는 유저입니다.".equals(message)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        if ("이미 사용 중인 닉네임입니다.".equals(message)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
        }
        return ResponseEntity.badRequest().body(message);
    }
}
