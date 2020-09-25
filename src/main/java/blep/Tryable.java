package blep;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.ToString;

import static blep.Tryable.Status.FAILED;
import static blep.Tryable.Status.SUCCEEDED;
import static blep.Tryable.Status.TO_TRY;

@JsonSubTypes(
        {
                @Type(Tryable.TryOnce.class),
                @Type(Tryable.Retryable.class)
        }
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
public interface Tryable<V> {


    enum Status{
        TO_TRY, SUCCEEDED, FAILED
    }

    V getPayload();

    boolean canTry();

    Status getStatus();

    Tryable<V> doTry();

    Tryable<V> success();

    Tryable<V> failed();

    @Getter
    @ToString
    class TryOnce<V> implements Tryable<V> {
        private final V payload;
        private final boolean canTry;
        @Getter
        private final Status status;

        @JsonCreator
        public TryOnce(
                @JsonProperty("payload") V payload,
                @JsonProperty("canTry") boolean canTry,
                @JsonProperty("status") Status status) {
            this.payload = payload;
            this.canTry = canTry;
            this.status = status;
        }

        public static <V> TryOnce<V> build(V payload) {
            return new TryOnce<>(payload, true, TO_TRY);
        }

        @Override
        public V getPayload() {
            return payload;
        }

        @Override
        public boolean canTry() {
            return canTry;
        }

        @Override
        public Tryable<V> doTry() {
            return this;
        }

        @Override
        public Tryable<V> success() {
            return new TryOnce<>(payload, false, SUCCEEDED);
        }

        @Override
        public Tryable<V> failed() {
            return canTry ? new TryOnce<>(
                    payload,
                    false,
                    TO_TRY
            ): new TryOnce<>(
                    payload,
                    false,
                    FAILED
            );
        }
    }

    @Getter
    @ToString
    class Retryable<V> implements Tryable<V>{

        private final V payload;

        private final Integer max;

        private final Integer tries;

        private final Status status;

        public Retryable(
                @JsonProperty("payload") V payload,
                @JsonProperty("max") Integer max,
                @JsonProperty("tries") Integer tries,
                @JsonProperty("status") Status status) {
            this.payload = payload;
            this.max = max;
            this.tries = tries;
            this.status = status;
        }

        @JsonCreator
        public Retryable<V> failed() {
            return new Retryable<>(
                    payload,
                    max,
                    tries,
                    FAILED
            );
        }

        public Retryable<V> doTry(){
            return new Retryable<>(
                    payload,
                    max,
                    tries +1,
                    max > tries ? TO_TRY : FAILED
            );
        }

        public Retryable<V> success(){
            return new Retryable<>(
                    payload,
                    max,
                    tries,
                    SUCCEEDED
            );
        }

        public boolean canTry() {
            return status == TO_TRY ;
        }

        public static <V> Retryable<V> init(V payload, Integer max) {
            return new Retryable<>(payload, max, 0, TO_TRY);
        }

    }

}
