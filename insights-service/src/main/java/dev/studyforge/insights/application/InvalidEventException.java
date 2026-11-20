package dev.studyforge.insights.application;

/** The message can never be applied (bad JSON, missing ids): skip retries, go to the DLT. */
public class InvalidEventException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
