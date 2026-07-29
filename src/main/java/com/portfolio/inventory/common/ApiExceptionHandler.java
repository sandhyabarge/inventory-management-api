package com.portfolio.inventory.common;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ProblemDetail> badRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> credentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = base(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", fields);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformed(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON or unsupported field value",
                request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(base(status, message, request));
    }

    private ProblemDetail base(
            HttpStatus status, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
