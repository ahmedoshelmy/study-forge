package dev.studyforge.learning.domain;

/** The requested roadmap, item or card does not exist. */
public class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(String what, Object id) {
    super(what + " " + id + " not found");
  }
}
