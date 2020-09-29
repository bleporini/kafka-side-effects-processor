package blep;

import io.vavr.concurrent.Future;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SideEffectProcessor<K,P,V> {

    @FunctionalInterface
    public interface AsyncPayloadProcessor<P, V>{
        Future<V> process(P payload);
    }
    @FunctionalInterface
    public interface AsyncRejector<K, P, R>{
        Future<R> reject(K id, Retryable<P> rejected);
    }
    @FunctionalInterface
    public interface AsyncFailureProcessor<K, P, R>{
        Future<R> failed(K id, Retryable<P> failed);
    }
    @FunctionalInterface
    public interface AsyncSuccessProcessor<K, P, V, R>{
        Future<R> success(K id, Retryable<P> success, V returnedValue);
    }

    @FunctionalInterface
    public interface ReturnedValueChecker<K, P, V>{
        boolean test(K id, Retryable<P> retryable, V returnedValue);
    }


    private final AsyncPayloadProcessor<P,V> payloadProcessor;

    private final AsyncSuccessProcessor<K,P,V,?> successProcessor;
    private final AsyncFailureProcessor<K,P,?> failureProcessor;
    private final AsyncRejector<K,P,?> rejector;
    private final ReturnedValueChecker<K,P,V> valueChecker;
    private final RetryPolicy<P, V> retryPolicy;

    public SideEffectProcessor(
            AsyncPayloadProcessor<P, V> payloadProcessor,
            AsyncSuccessProcessor<K, P, V, Object> successProcessor,
            AsyncFailureProcessor<K, P, Object> failureProcessor,
            AsyncRejector<K, P, Object> rejector,
            ReturnedValueChecker<K, P, V> valueChecker,
            RetryPolicy<P, V> retryPolicy) {
        this.payloadProcessor = payloadProcessor;
        this.successProcessor = (id,tryable, value)->{
          log.trace("Notifying success for request #{}", id);
            return successProcessor.success(id, tryable, value);
        } ;
        this.failureProcessor = (id, failed) -> {
            log.info("Call for request #{} Failed", id);
            return failureProcessor.failed(id, failed);
        };
        this.rejector = (id, tryable) -> {
            log.info("Notifying rejection for request #{}", id);
            return rejector.reject(id, tryable);
        };
        this.valueChecker = (id, triable, value) -> {
            boolean result = valueChecker.test(id, triable, value);
            log.trace("Verification for request #{}: {}", id, result ? "PASSED" : "FAILED");
            return result;
        };
        this.retryPolicy = retryPolicy;
    }
    public SideEffectProcessor(
            AsyncPayloadProcessor<P, V> payloadProcessor,
            AsyncSuccessProcessor<K, P, V, Object> successProcessor,
            AsyncFailureProcessor<K, P, Object> failureProcessor,
            AsyncRejector<K, P, Object> rejector,
            ReturnedValueChecker<K, P, V> valueChecker) {
        this(
                payloadProcessor,
                successProcessor,
                failureProcessor,
                rejector,
                valueChecker,
                RetryPolicy.immediate()
        );
    }

    public Future<?> process(K id, Retryable<P> retryable) {

        Retryable<P> tr1 = retryable.doTry();

        return tr1.canTry() ?
                retryPolicy.evaluateAndProcess(payloadProcessor, tr1)
                        .onFailure(e -> {
                            log.error("Exception raised while processing request", e);
                            failureProcessor.failed(id, tr1);
                        }).flatMap(v->
                        valueChecker.test(id,tr1,v) ?
                            successProcessor.success(id, tr1.success(), v):
                            failureProcessor.failed(id,tr1)
                    ):rejector.reject(id, tr1.failed());
    }



}
