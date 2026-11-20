package dev.studyforge.learning.adapter.web;

import dev.studyforge.learning.domain.DomainException;
import dev.studyforge.learning.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Domain errors as RFC 9457 problem details. Validation errors use Spring's built-in handler. */
@RestControllerAdvice
class ApiErrors {

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail notFound(NotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail badRequest(DomainException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}
