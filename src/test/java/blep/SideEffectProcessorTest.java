package blep;

import blep.Tryable.TryOnce;
import io.vavr.concurrent.Future;
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

        processor.process(1l, TryOnce.build("yes"))
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

        processor.process(1l, TryOnce.build("yes"))
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

        processor.process(1L, TryOnce.build("yes").failed())
                .get();

        assertThat(
                rejectedNotified
        ).isTrue();
    }

}