package org.libreproject.libre.android;

public interface DestroyableContext {

	void runOnUiThreadUnlessDestroyed(Runnable runnable);
}
