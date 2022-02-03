package org.libreproject.bramble.api;

import org.libreproject.bramble.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface Consumer<T> {

	void accept(T t);
}
