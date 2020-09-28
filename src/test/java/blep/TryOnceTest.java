package blep;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TryOnceTest {

    @Test
    public void should_try() {
        assertThat(
                Retryable.init("test", 1).canTry()
        ).isTrue();
    }

    @Test
    public void should_not_retry() {
        assertThat(
                Retryable.init("test", 1)
                        .failed()
                        .canTry()
        ).isFalse();
    }
}