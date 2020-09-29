package blep;

import blep.SideEffectProcessor.AsyncPayloadProcessor;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import lombok.AllArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface RetryPolicy<P,V> {

    Future<V> evaluateAndProcess(AsyncPayloadProcessor<P, V> processor, Retryable<P> retryable);

    static <P,V> RetryPolicy<P,V> immediate() {
        return new ImmediateRetryPolicy<>();
    }

    static <P, V> RetryPolicy<P, V> fixed(long delay) {
        return new FixedDelayRetryPolicy<>(delay);
    }

    static <P, V> RetryPolicy<P, V> backoff(long delay) {
        return new BackoffRetryPolicy<>(delay);
    }

    class ImmediateRetryPolicy<P,V> implements RetryPolicy<P,V>{
        @Override
        public Future<V> evaluateAndProcess(AsyncPayloadProcessor<P, V> processor, Retryable<P> retryable) {
            return processor.process(retryable.getPayload());
        }
    }

    abstract class AbstractDelayedRetryPolicy <P, V> implements RetryPolicy<P, V>{
        private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        @Override
        public Future<V> evaluateAndProcess(AsyncPayloadProcessor<P, V> processor, Retryable<P> retryable) {
            if(retryable.getTries() == 1)
                return processor.process(retryable.getPayload());

            Promise<V> promise = Promise.make();
            executor.schedule(
                    () -> processor.process(retryable.getPayload())
                            .onFailure(promise::failure)
                            .forEach(promise::success),
                    processIn(retryable),
                    TimeUnit.MILLISECONDS
            );

            return promise.future();
        }

        public abstract long processIn(Retryable<P> retryable) ;
    }

    @AllArgsConstructor
    class FixedDelayRetryPolicy<P, V> extends AbstractDelayedRetryPolicy<P, V> {
        private final long delay;

        @Override
        public long processIn(Retryable<P> retryable) {
            return  delay - retryable.elapsedMsSinceLastEvent();
        }
    }

    @AllArgsConstructor
    class BackoffRetryPolicy<P, V> extends AbstractDelayedRetryPolicy<P, V> {
        private final long delay;

        @Override
        public long processIn(Retryable<P> retryable) {
            long rawDelay = delay * (retryable.getTries() - 1);
            return rawDelay - retryable.elapsedMsSinceLastEvent();
        }
    }

}
