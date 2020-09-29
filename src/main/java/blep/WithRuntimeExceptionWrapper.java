package blep;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;

public interface WithRuntimeExceptionWrapper {

    default <T> T toUnchecked(CheckedFunction0<T> checkedFunction0) {
        return checkedFunction0.unchecked().apply();
    }

    default void toUnchecked(CheckedRunnable checkedFunction0) {
        try {
            checkedFunction0.run();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
