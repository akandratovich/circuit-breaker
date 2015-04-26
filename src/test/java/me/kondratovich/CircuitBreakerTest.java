package me.kondratovich;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CircuitBreakerTest {
  private ScheduledExecutorService executor;

  @Before
  public void before() {
    executor = Executors.newSingleThreadScheduledExecutor();
  }

  @After
  public void after() {
    executor.shutdown();
  }

  @Test
  public void check() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker(1000, 2, executor);

    assertOk(cb);
    assertFail(cb);
    assertOk(cb);
    assertFail(cb);
    assertFail(cb);

    long time0 = System.currentTimeMillis() + 1000;
    while (System.currentTimeMillis() < time0)
      assertOpen(cb);

    Thread.sleep(10);

    assertFail(cb);

    long time1 = System.currentTimeMillis() + 1000;
    while (System.currentTimeMillis() < time1)
      assertOpen(cb);

    Thread.sleep(10);

    assertOk(cb);
    assertFail(cb);
    assertOk(cb);
    assertOk(cb);
  }

  private void assertOk(CircuitBreaker cb) {
    try {
      long value = cb.execute(okOperation);
      Assert.assertEquals(value, 1L);
    } catch (Exception e) {
      Assert.fail("Got unexpected exception: " + e.getClass());
    }
  }

  private void assertFail(CircuitBreaker cb) {
    try {
      cb.execute(failOperation);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), RuntimeException.class);
    }
  }

  private void assertOpen(CircuitBreaker cb) {
    try {
      cb.execute(okOperation);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), CircuitBreakerOpenException.class);
    }
  }

  private final Operation<Void> failOperation = new Operation<Void>() {
    @Override
    public Void invoke() {
      throw new RuntimeException();
    }
  };

  private final Operation<Long> okOperation = new Operation<Long>() {
    @Override
    public Long invoke() {
      return 1L;
    }
  };
}