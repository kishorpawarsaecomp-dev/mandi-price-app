package com.mandi.exception;

import com.mandi.dto.ErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponse> badRequest(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> badParam(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing or invalid request parameter.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).findFirst().orElse("Invalid request.");
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg);
    }

    @ExceptionHandler(ExternalApiException.class)
    ResponseEntity<ErrorResponse> upstream(ExternalApiException e) {
        log.error("External API failure: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE",
                "Mandi data is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> status(ResponseStatusException e) {
        return build(HttpStatus.valueOf(e.getStatusCode().value()), "ERROR",
                e.getReason() == null ? "Request failed." : e.getReason());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> other(Exception e) {
        log.error("Unhandled error", e);   // stack trace stays in logs only
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again later.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String code, String msg) {
        return ResponseEntity.status(s).body(new ErrorResponse(code, msg, Instant.now()));
    }
}
