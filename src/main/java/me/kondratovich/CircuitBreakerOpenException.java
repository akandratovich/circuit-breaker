package me.kondratovich;

public class CircuitBreakerOpenException extends RuntimeException {
  private static final long serialVersionUID = -3648579412064641795L;

  CircuitBreakerOpenException() {
    super("Circuit Breaker is open; calls are failing fast");
  }
}