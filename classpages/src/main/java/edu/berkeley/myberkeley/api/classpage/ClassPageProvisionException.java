package edu.berkeley.myberkeley.api.classpage;

public class ClassPageProvisionException extends RuntimeException {

  private static final long serialVersionUID = -5991810393303427569L;

  public ClassPageProvisionException() {
  }

  public ClassPageProvisionException(String message) {
    super(message);
  }

  public ClassPageProvisionException(Throwable cause) {
    super(cause);
  }

  public ClassPageProvisionException(String message, Throwable cause) {
    super(message, cause);
  }

}
