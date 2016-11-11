package org.briarproject.android.controller.handler;

import android.support.annotation.UiThread;

import org.briarproject.android.DestroyableContext;
import org.briarproject.api.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class UiResultExceptionHandler<R, E extends Exception>
		extends UiExceptionHandler<E> implements ResultExceptionHandler<R, E> {

	protected UiResultExceptionHandler(DestroyableContext listener) {
		super(listener);
	}

	@Override
	public void onResult(final R result) {
		listener.runOnUiThreadUnlessDestroyed(new Runnable() {
			@Override
			public void run() {
				onResultUi(result);
			}
		});
	}

	@UiThread
	public abstract void onResultUi(R result);

}
