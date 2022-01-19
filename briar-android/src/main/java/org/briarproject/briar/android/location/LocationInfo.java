package org.briarproject.briar.android.location;

import android.util.JsonReader;

import org.briarproject.bramble.api.identity.Author;
import org.osmdroid.views.overlay.Marker;

import java.io.StringReader;

public class LocationInfo{
	public enum LocationInfoType{USERPOSITION,USERALERT,USERWARNING,USERINFO,USERMEETING};

	public String id;
	public double lng;
	public double lat;
	public double bearing;
	public long timestamp;
	public double speed;
	public LocationInfoType type;
	public String alias;
	public Author author;
	public Marker marker;
	public boolean admin;
	public String message;
	public int size;

	public static LocationInfo parseLocationInfo(String text){
		JsonReader reader =
				new JsonReader(new StringReader(text));
		double lng = 0;
		double lat = 0;
		double bearing = 0;
		double speed = 0;
		int size=2;
		int subType = 0;

		LocationInfo locationInfo = new LocationInfo();
		try{
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("id")) {
					locationInfo.id = reader.nextString();
				} else
				if (name.equals("lng")) {
					lng = reader.nextDouble();
				} else if (name.equals("lat")) {
					lat = reader.nextDouble();
				} else if (name.equals("bearing")) {
					bearing = reader.nextDouble();
				} else if (name.equals("speed")) {
					speed = reader.nextDouble();
				} else if (name.equals("subType")) {
					subType = reader.nextInt();
				} else if (name.equals("size")) {
					size = reader.nextInt();
				}else if (name.equals("message")) {
					locationInfo.message = reader.nextString();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		} catch (Exception e) {
			e.printStackTrace();
		}


		locationInfo.lng = lng;
		locationInfo.lat = lat;
		locationInfo.size=size;
		locationInfo.bearing = bearing;
		locationInfo.speed = speed;

		locationInfo.type = LocationInfo.LocationInfoType.values()[subType];
		return locationInfo;
	}

}

