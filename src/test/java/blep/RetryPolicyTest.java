package blep;

import io.vavr.concurrent.Future;
import org.junit.Test;

import static blep.Retryable.Status.TO_TRY;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

public class RetryPolicyTest {


    @Test
    public void should_process_immediately() {
        RetryPolicy<Integer, Long> immediate = RetryPolicy.immediate();

        long beforeExec = currentTimeMillis();
        long executionDelay = immediate.evaluateAndProcess(
                payload -> Future.successful(currentTimeMillis()),
                new Retryable<>(1, 3, 3, TO_TRY, 1)
        ).get() - beforeExec;

        //Immediately
        assertThat(
                executionDelay
        ).isGreaterThanOrEqualTo(0).isLessThan(100);
    }

    @Test
    public void should_process_immediately_at_first() {
        RetryPolicy<Integer, Long> immediate = RetryPolicy.fixed(1000);

        long beforeExec = currentTimeMillis();
        long executionDelay = immediate.evaluateAndProcess(
                payload -> Future.successful(currentTimeMillis()),
                Retryable.init(1, 3).doTry()
        ).get() - beforeExec;

        assertThat(
                executionDelay
        ).isLessThan(200).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void should_process_after_a_fixed_delay() throws InterruptedException {
        RetryPolicy<Integer, Long> immediate = RetryPolicy.fixed(1000);

        long beforeExec = currentTimeMillis();
        long executionDelay = immediate.evaluateAndProcess(
                payload -> Future.successful(currentTimeMillis()),
                new Retryable<>(1, 3, 2, TO_TRY, beforeExec)

        ).get() - beforeExec;

        assertThat(
                executionDelay
        ).isGreaterThan(900);

    }

    @Test
    public void should_retry_immediately_because_its_late() {
        RetryPolicy<Integer, Long> immediate = RetryPolicy.fixed(1000);

        long beforeExec = currentTimeMillis();
        Long executionDelay = immediate.evaluateAndProcess(
                payload -> Future.successful(currentTimeMillis()),
                new Retryable<>(1, 3, 2, TO_TRY, beforeExec - 1500)

        ).get() - beforeExec;

        assertThat(
                executionDelay
        ).isLessThan(500).isPositive();
    }

    @Test
    public void should_process_after_a_backoff_delay() {
        RetryPolicy<Integer, Long> immediate = RetryPolicy.backoff(500);

        long beforeExec = currentTimeMillis();
        long executionDelay = immediate.evaluateAndProcess(
                payload -> Future.successful(currentTimeMillis()),
                new Retryable<>(1, 3, 3, TO_TRY, beforeExec)

        ).get() - beforeExec;

        assertThat(
                executionDelay
        ).isGreaterThan(
                700
        );

    }




}