package backend.academy.scrapper.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FinanceTrackerException.class)
    public ResponseEntity<ApiErrorResponse> handleFinanceTrackerException(FinanceTrackerException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .description("Business logic error occurred")
                .code("FINANCE_TRACKER_ERROR")
                .exceptionName(ex.getClass().getSimpleName())
                .exceptionMessage(ex.getMessage())
                .stacktrace(Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toList())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .description("Unexpected internal error")
                .code("UNHANDLED_EXCEPTION")
                .exceptionName(ex.getClass().getSimpleName())
                .exceptionMessage(ex.getMessage())
                .stacktrace(Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toList())
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
