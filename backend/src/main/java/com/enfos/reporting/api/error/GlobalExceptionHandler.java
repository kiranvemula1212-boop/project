package com.enfos.reporting.api.error;

import com.enfos.reporting.application.InvalidQueryException;
import com.enfos.reporting.application.ReportNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC 9457 problem+json responses. Every response carries a
 * traceId, logged alongside the exception, so a support request can be traced back to the
 * server-side log entry without leaking stack traces or internals to the client.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReportNotFoundException.class)
    ResponseEntity<ProblemDetail> handleReportNotFound(ReportNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Report not found");
        problem.setType(URI.create("/problems/report-not-found"));
        return respond(problem, HttpStatus.NOT_FOUND, request, ex);
    }

    @ExceptionHandler(InvalidQueryException.class)
    ResponseEntity<ProblemDetail> handleInvalidQuery(InvalidQueryException ex, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request query is invalid.");
        problem.setTitle("Invalid query");
        problem.setType(URI.create("/problems/invalid-query"));
        problem.setProperty("errors", ex.violations());
        return respond(problem, HttpStatus.BAD_REQUEST, request, ex);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal server error");
        return respond(problem, HttpStatus.INTERNAL_SERVER_ERROR, request, ex);
    }

    private static ResponseEntity<ProblemDetail> respond(
            ProblemDetail problem, HttpStatus status, HttpServletRequest request, Exception ex) {
        String traceId = UUID.randomUUID().toString();
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("traceId", traceId);
        log.error("traceId={} path={}", traceId, request.getRequestURI(), ex);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
