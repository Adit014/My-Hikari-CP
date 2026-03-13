package com.example.myhikaricp;

public class ConnectionNotAvailableException extends RuntimeException {
  public ConnectionNotAvailableException(String message) {
    super(message);
  }
}
