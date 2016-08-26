package org.briarproject.api.data;

import java.util.Map.Entry;

public class BdfEntry implements Entry<String, Object>, Comparable<BdfEntry> {

	private final String key;
	private final Object value;

	public BdfEntry(String key, Object value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object setValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(BdfEntry e) {
		if (e == this) return 0;
		return key.compareTo(e.key);
	}
}
