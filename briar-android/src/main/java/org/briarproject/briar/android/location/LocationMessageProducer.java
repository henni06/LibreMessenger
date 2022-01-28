package org.briarproject.briar.android.location;

import android.util.Log;

import org.json.JSONObject;

public class LocationMessageProducer {

	public enum Actions{ADD,SET,DELETE};

	public static String buildLocationMessage(double lng,double lat,double bearing,int subType){
		return "{\"type\":\"location\",\"lng\":" +
				lng +
				",\"lat\":" +
				lat + ",\"bearing\":"+
				bearing+",\"subType\":"+subType+"}";
	}


	public static String buildMarkerMessage(LocationInfo locationInfo,Actions action){
		switch(action){
			case ADD:
				return buildAddMarkerMessage(locationInfo.id, locationInfo.lng,
						locationInfo.lat, locationInfo.type,locationInfo.message,locationInfo.size);

			case SET:
				return buildSetMarkerMessage(locationInfo.id, locationInfo.lng,
						locationInfo.lat,locationInfo.message,locationInfo.size,locationInfo.type);
			case DELETE:
				return buildDeleteMarkerMessage(locationInfo.id);
		}
		return null;
	}

	public static String buildAddMarkerMessage(String id,double lng,double lat,LocationInfo.LocationInfoType subType,String message,int size) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "marker");
			obj.put("subType",subType.ordinal());
			obj.put("action",Actions.ADD.ordinal());
			obj.put("id",id);
			obj.put("lng",lng);
			obj.put("lat",lat);
			obj.put("size",size);
			obj.put("message",message);

		}
		catch(Exception e){
			Log.d(LocationMessageProducer.class.getName(),e.getMessage());
		}
		return obj.toString();

	}

	public static String buildSetMarkerMessage(String id,double lng,double lat,String message,int size,
			LocationInfo.LocationInfoType subType) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "marker");
			obj.put("action",Actions.SET.ordinal());
			obj.put("id",id);
			obj.put("lng",lng);
			obj.put("lat",lat);
			obj.put("size",size);
			obj.put("message",message);
			obj.put("subType",subType.ordinal());
		}
		catch(Exception e){
			Log.d(LocationMessageProducer.class.getName(),e.getMessage());
		}
		return obj.toString();

	}

	public static String buildDeleteMarkerMessage(String id) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", "marker");
			obj.put("action",Actions.DELETE.ordinal());
			obj.put("id",id);
		}
		catch(Exception e){
			Log.d(LocationMessageProducer.class.getName(),e.getMessage());
		}
		return obj.toString();

	}
}
