package server.exceptions;

public class InvalidResponseException extends RuntimeException {
    public InvalidResponseException(String message) {
      super(message);
    }
}
