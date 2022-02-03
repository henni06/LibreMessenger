package org.libreproject.libre.android.controller.handler;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.libre.android.DestroyableContext;

import javax.annotation.concurrent.Immutable;

import androidx.annotation.UiThread;

@Immutable
@NotNullByDefault
public abstract class UiResultExceptionHandler<R, E extends Exception>
		extends UiExceptionHandler<E> implements ResultExceptionHandler<R, E> {

	protected UiResultExceptionHandler(DestroyableContext listener) {
		super(listener);
	}

	@Override
	public void onResult(R result) {
		listener.runOnUiThreadUnlessDestroyed(() -> onResultUi(result));
	}

	@UiThread
	public abstract void onResultUi(R result);

}
