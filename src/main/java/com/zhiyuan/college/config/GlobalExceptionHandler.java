package com.zhiyuan.college.config;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> response = baseResponse("VALIDATION_ERROR", "Invalid request parameters", request);
        response.put("details", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex,
                                                                HttpServletRequest request) {
        Map<String, Object> response = baseResponse("CONSTRAINT_VIOLATION", ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex,
                                                                    HttpServletRequest request) {
        Map<String, Object> response = baseResponse(
                "REQUEST_FAILED",
                ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                request);
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Unique constraint violations (duplicate username, duplicate cutoff row, ...) are client
     * errors, not server faults, so they must not fall through to the generic 500 handler.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateKey(DuplicateKeyException ex,
                                                                 HttpServletRequest request) {
        log.warn("Duplicate key rejected: {}", ex.getMostSpecificCause().getMessage());
        Map<String, Object> response = baseResponse("DUPLICATE_KEY", "记录已存在，请勿重复提交", request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex,
                                                               HttpServletRequest request) {
        Map<String, Object> response = baseResponse(
                "BAD_REQUEST",
                "Invalid request parameters",
                request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex,
                                                                HttpServletRequest request) {
        String resourcePath = ex.getResourcePath();
        if ("favicon.ico".equals(resourcePath) || "/favicon.ico".equals(resourcePath)) {
            return ResponseEntity.noContent().build();
        }
        Map<String, Object> response = baseResponse("RESOURCE_NOT_FOUND", "Resource not found: " + resourcePath, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex,
                                                               HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        Map<String, Object> response = baseResponse(
                "INTERNAL_SERVER_ERROR",
                "Internal server error",
                request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private Map<String, Object> baseResponse(String code, String message, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("requestId", request == null ? null : request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE));
        return response;
    }
}
