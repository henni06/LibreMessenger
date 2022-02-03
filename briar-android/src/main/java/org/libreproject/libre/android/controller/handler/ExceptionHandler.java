package org.libreproject.libre.android.controller.handler;

public interface ExceptionHandler<E extends Exception> {

	void onException(E exception);

}
