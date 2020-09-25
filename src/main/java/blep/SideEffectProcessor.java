package blep;

import io.vavr.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;


@Slf4j
public class SideEffectProcessor<K,P,V> {

    @FunctionalInterface
    public interface AsyncPayloadProcessor<P, V>{
        Future<V> process(P payload);
    }
    @FunctionalInterface
    public interface AsyncRejector<K, P, R>{
        Future<R> reject(K id, Tryable<P> rejected);
    }
    @FunctionalInterface
    public interface AsyncFailureProcessor<K, P, R>{
        Future<R> failed(K id, Tryable<P> failed);
    }
    @FunctionalInterface
    public interface AsyncSuccessProcessor<K, P, V, R>{
        Future<R> success(K id, Tryable<P> success, V returnedValue);
    }

    @FunctionalInterface
    public interface ReturnedValueChecker<K, P, V>{
        boolean test(K id, Tryable<P> tryable, V returnedValue);
    }


    private final AsyncPayloadProcessor<P,V> payloadProcessor;

    private final AsyncSuccessProcessor<K,P,V,?> successProcessor;
    private final AsyncFailureProcessor<K,P,?> failureProcessor;
    private final AsyncRejector<K,P,?> rejector;
    private final ReturnedValueChecker<K,P,V> valueChecker;

    public SideEffectProcessor(
            AsyncPayloadProcessor<P, V> payloadProcessor,
            AsyncSuccessProcessor<K, P, V, Object> successProcessor,
            AsyncFailureProcessor<K, P, Object> failureProcessor,
            AsyncRejector<K, P, Object> rejector,
            ReturnedValueChecker<K,P,V> valueChecker) {
        this.payloadProcessor = payloadProcessor;
        this.successProcessor = (id,tryable, value)->{
          log.trace("Notifying success for request #{}", id);
            return successProcessor.success(id, tryable, value);
        } ;
        this.failureProcessor = (id, failed) -> {
            log.trace("Call for request {} Failed", id);
            return failureProcessor.failed(id, failed);
        };
        this.rejector = (id, tryable) -> {
            log.trace("Notifying rejection for request #{}", id);
            return rejector.reject(id, tryable);
        };
        this.valueChecker = (id, triable, value) -> {
            boolean result = valueChecker.test(id, triable, value);
            log.trace("Verification for request #{}: {}", id, result ? "PASSED" : "FAILED");
            return result;
        };
    }

    public Future<?> process(K id, Tryable<P> tryable) {

        Tryable<P> tr1 = tryable.doTry();

        return tr1.canTry() ?
                payloadProcessor.process(tryable.getPayload())
                    .flatMap(v->
                        valueChecker.test(id,tr1,v) ?
                            successProcessor.success(id, tr1.success(), v):
                            failureProcessor.failed(id,tr1.failed())
                    ):rejector.reject(id, tr1.failed());
    }



}
