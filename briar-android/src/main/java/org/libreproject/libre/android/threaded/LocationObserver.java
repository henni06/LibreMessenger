package org.libreproject.libre.android.threaded;

import android.location.LocationListener;

import org.libreproject.bramble.api.sync.GroupId;

import java.util.HashMap;

public class LocationObserver {
	private HashMap<GroupId, LocationListener> listeners=new HashMap<>();

	public void add(GroupId groupId,LocationListener locationListener){
		if(!listeners.containsKey(groupId)){
			listeners.put(groupId,locationListener);
		}
	}

	public boolean isLocationActivated(GroupId groupId){
		return listeners.containsKey(groupId) && listeners.get(groupId)!=null;
	}

	public LocationListener get(GroupId groupId){
		return listeners.get(groupId);
	}

	public void remove(GroupId groupId){
		listeners.remove(groupId);
	}
}
