package blep;

import blep.Tryable.TryOnce;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TryOnceTest {

    @Test
    public void should_try() {
        assertThat(
                TryOnce.build("test").canTry()
        ).isTrue();
    }

    @Test
    public void should_not_retry() {
        assertThat(
                TryOnce.build("test")
                        .failed()
                        .canTry()
        ).isFalse();
    }
}