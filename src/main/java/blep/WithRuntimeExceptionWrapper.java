package blep;

import io.vavr.CheckedFunction0;

public interface WithRuntimeExceptionWrapper {

    default <T> T toUnchecked(CheckedFunction0<T> checkedFunction0) {
        return checkedFunction0.unchecked().apply();
    }
}
