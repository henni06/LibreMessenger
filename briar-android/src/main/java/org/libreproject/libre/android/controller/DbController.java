package org.libreproject.libre.android.controller;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@Deprecated
@NotNullByDefault
public interface DbController {

	void runOnDbThread(Runnable task);
}
