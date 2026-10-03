package com.Ledger_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

    @RestControllerAdvice
    public class GlobalExceptionHandler {

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<Map<String, Object>> handleApiException(
                ApiException ex) {

            Map<String, Object> response = Map.of(
                    "timestamp", Instant.now(),
                    "status", ex.getStatus().value(),
                    "error", ex.getStatus().getReasonPhrase(),
                    "message", ex.getMessage()
            );

            return ResponseEntity
                    .status(ex.getStatus())
                    .body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationException(
                MethodArgumentNotValidException ex) {

            String message = ex.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .findFirst()
                    .orElse("Invalid request");

            Map<String, Object> response = Map.of(
                    "timestamp", Instant.now(),
                    "status", 400,
                    "error", "Bad Request",
                    "message", message
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

