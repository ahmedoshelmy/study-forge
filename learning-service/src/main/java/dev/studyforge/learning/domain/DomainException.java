package dev.studyforge.learning.domain;

/** A rule of the domain was broken by the caller's input (maps to HTTP 400/409). */
public class DomainException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DomainException(String message) {
    super(message);
  }
}
