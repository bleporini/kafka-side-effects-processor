package blep;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import static blep.Retryable.Status.*;

@Getter
public class Retryable<V> {


    enum Status{
        TO_TRY, SUCCEEDED, FAILED
    }

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
        return new Retryable<V>(
                payload,
                max,
                tries,
                FAILED
        );
    }

    public Retryable<V> doTry(){
        return new Retryable<V>(
                payload,
                max,
                tries +1,
                status==FAILED ?
                        FAILED :
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
