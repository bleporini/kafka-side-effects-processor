package blep;

import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SideEffectProcessorTest {


    @Test
    public void should_send_success() {
        AtomicBoolean successNotified = new AtomicBoolean(false);


        SideEffectProcessor<Long, String, Integer> processor = new SideEffectProcessor<>(
                s -> Future.successful(1),
                (id, success, returnedValue) -> Future.successful(successNotified.getAndSet(true)),
                (k, t) -> {
                    throw new RuntimeException("should not happen");
                },
                (k, t) -> {
                    throw new RuntimeException("should not happen");
                },
                (k,t,v) -> true
        );

        processor.process(1l, Retryable.init("yes", 1))
                .get();

        assertThat(
                successNotified
        ).isTrue();
    }

    @Test
    public void should_fail() {
        AtomicBoolean failureNotified = new AtomicBoolean(false);


        SideEffectProcessor<Long, String, Integer> processor = new SideEffectProcessor<>(
                s -> Future.successful(1),
                (id, success, returnedValue) -> {
                    throw new RuntimeException("should not happen");
                },
                (k, t) -> Future.successful(failureNotified.getAndSet(true)),
                (k, t) -> {
                    throw new RuntimeException("should not happen");
                },
                (k,t,v) -> false
        );

        processor.process(1l, Retryable.init("yes", 1))
                .get();

        assertThat(
                failureNotified
        ).isTrue();

    }

    @Test
    public void should_reject() {
        AtomicBoolean rejectedNotified = new AtomicBoolean(false);


        SideEffectProcessor<Long, String, Integer> processor = new SideEffectProcessor<>(
                s -> Future.successful(1),
                (id, success, returnedValue) -> {
                    throw new RuntimeException("should not happen");
                },
                (k, t) -> {
                    throw new RuntimeException("should not happen");
                },
                (k, t) -> Future.successful(rejectedNotified.getAndSet(true)),
                (k,t,v) -> false
        );

        processor.process(1L, Retryable.init("yes", 1).failed())
                .get();

        assertThat(
                rejectedNotified
        ).isTrue();
    }

    @Test
    public void should_manage_exceptions() {
        AtomicBoolean failureNotified = new AtomicBoolean(false);
        Promise<Object> promise = Promise.make();


        SideEffectProcessor<Long, String, Integer> processor = new SideEffectProcessor<>(
                s -> Future.of(() -> {
                    throw new RuntimeException("bang!");
                }),
                (id, success, returnedValue) -> {
                    throw new RuntimeException("should not happen");
                },
                (k, t) -> {
                    Future<Object> future = Future.successful(failureNotified.getAndSet(true));
                    future.await();
                    promise.success("ok");
                    return future;
                },
                (k, t) -> {
                    throw new RuntimeException("should not happen");
                },
                (k,t,v) -> false
        );

        processor.process(1L, Retryable.init("yes", 1));

        promise.future().await(); //Otherwise the test may end before the flag has been set.

        assertThat(
                failureNotified
        ).isTrue();

    }
}