package me.kondratovich;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker {
  private final ScheduledExecutorService executor;
  private final long timeout;
  private final int failLimit;

  private final AtomicReference<State> state;

  public CircuitBreaker(long timeout, int failLimit, ScheduledExecutorService executor) {
    this.timeout = timeout;
    this.failLimit = failLimit;
    this.executor = executor;

    this.state = new AtomicReference<>(closed);
  }

  public <T> T execute(Operation<T> operation) {
    State current = state.get();
    try {
      T value = current.invoke(operation);
      current.success();

      return value;
    } catch (CircuitBreakerOpenException e) {
      throw e;
    } catch (Exception e) {
      current.fail();

      throw e;
    }
  }

  private void transition(State from, State to) {
    if (state.compareAndSet(from, to))
      to.enter();
    else
      throw new IllegalStateException("Illegal transition attempted from: " + from + " to " + to);
  }

  private final State closed = new State() {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public <T> T invoke(Operation<T> delegate) {
      return delegate.invoke();
    }

    @Override
    public void success() {
      counter.set(0);
    }

    @Override
    public void fail() {
      if (counter.incrementAndGet() == failLimit)
        transition(closed, open);
    }

    @Override
    public void enter() {
      counter.set(0);
    }
  };

  private final State halfOpen = new State() {
    private final AtomicBoolean state = new AtomicBoolean(true);

    @Override
    public <T> T invoke(Operation<T> delegate) throws CircuitBreakerOpenException {
      if (state.compareAndSet(true, false))
        return delegate.invoke();

      throw new CircuitBreakerOpenException();
    }

    @Override
    public void success() {
      transition(halfOpen, closed);
    }

    @Override
    public void fail() {
      transition(halfOpen, open);
    }

    @Override
    public void enter() {
      state.set(true);
    }
  };

  private final State open = new State() {
    @Override
    public <T> T invoke(Operation<T> delegate) throws CircuitBreakerOpenException {
      throw new CircuitBreakerOpenException();
    }

    @Override
    public void success() {
      // noop
    }

    @Override
    public void fail() {
      // noop
    }

    @Override
    public void enter() {
      executor.schedule(new Runnable() {
        @Override
        public void run() {
          transition(open, halfOpen);
        }
      }, timeout, TimeUnit.MILLISECONDS);
    }
  };
}