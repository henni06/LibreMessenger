package org.libreproject.libre.privategroup.invitation;

import org.libreproject.bramble.api.sync.Group.Visibility;

interface State {

	int getValue();

	Visibility getVisibility();

	boolean isAwaitingResponse();
}
