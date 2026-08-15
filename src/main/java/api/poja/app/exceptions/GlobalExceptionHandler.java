package api.poja.app.exceptions;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password"));
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ErrorResponse> handleDisabled() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Account is disabled"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(ErrorResponse.of(ex.getStatusCode().value(), ex.getReason()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid input data: " + details));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedJson() {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Malformed JSON request body"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String targetType =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown type";
    String message =
        "Parameter '%s' with value '%s' could not be converted to '%s'"
            .formatted(ex.getName(), ex.getValue(), targetType);
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error"));
  }
}
