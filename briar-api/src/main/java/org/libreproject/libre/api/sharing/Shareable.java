package org.libreproject.libre.api.sharing;

import org.libreproject.bramble.api.Nameable;
import org.libreproject.bramble.api.nullsafety.NotNullByDefault;
import org.libreproject.bramble.api.sync.GroupId;

@NotNullByDefault
public interface Shareable extends Nameable {

	GroupId getId();

}
