package org.libreproject.bramble.api.lifecycle.event;

import org.libreproject.bramble.api.event.Event;
import org.libreproject.bramble.api.lifecycle.LifecycleManager.LifecycleState;

/**
 * An event that is broadcast when the app enters a new lifecycle state.
 */
public class LifecycleEvent extends Event {

	private final LifecycleState state;

	public LifecycleEvent(LifecycleState state) {
		this.state = state;
	}

	public LifecycleState getLifecycleState() {
		return state;
	}
}
