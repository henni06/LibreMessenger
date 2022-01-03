package org.briarproject.briar.android.threaded;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.timepicker.TimeFormat;

import org.briarproject.bramble.api.identity.Author;
import org.briarproject.briar.R;
import org.briarproject.briar.android.privategroup.conversation.GroupMessageItem;
import org.briarproject.briar.api.identity.AuthorInfo;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.io.StringReader;
import java.text.DateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import im.delight.android.identicons.IdenticonDrawable;

public class ThreadMap extends Fragment{
	public static final String LOCATION_IDENTIFIER = "{\"type\":\"location\"";
	private static final int WT_RED=60*1000*10; //Older than 10 minutes
	private static final int WT_YELLOW=60*1000; //Older than 1 minutes

	private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
	private MapView map = null;

	private ArrayList<LocationInfo> locations=new ArrayList< LocationInfo>();
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public void handleLocationMessage(GroupMessageItem message) throws Exception{
		if(getContext()==null) return;
		if(message.getText().startsWith(LOCATION_IDENTIFIER)){


			JsonReader reader = new JsonReader(new StringReader(message.getText()));
			double lng=0;
			double lat=0;
			double heading=0;
			double speed=0;
			int subType=0;
			try {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("lng")) {
						lng = reader.nextDouble();
					} else if (name.equals("lat")) {
						lat = reader.nextDouble();
					}else if (name.equals("heading")) {
						heading = reader.nextDouble();
					}else if (name.equals("speed")) {
						speed = reader.nextDouble();
					}else if (name.equals("subType")) {
						subType = reader.nextInt();
					}
					else{
						reader.skipValue();
					}
				}
				reader.endObject();
			}
			catch(Exception e){
				e.printStackTrace();
			}

			LocationInfo locationInfo=new LocationInfo();
			locationInfo.lng=lng;
			locationInfo.lat=lat;
			locationInfo.timestamp=message.getTimestamp();
			locationInfo.heading=heading;
			locationInfo.speed=speed;
			locationInfo.author=message.getAuthor();
			locationInfo.alias=message.getAuthorInfo().getAlias();
			locationInfo.type=subType;
			boolean found=false;
			for(int i=0;i<locations.size();i++){

				if(locations.get(i).author.equals(message.getAuthor()) &&
						locations.get(i).type==ThreadListActivity.TP_USERPOSITION){
					locations.set(i,locationInfo);
					found=true;
				}
			}
			if(!found){
				locations.add(locationInfo);
			}
			//locations.put(message.getAuthorInfo(),locationInfo);
			refreshMap();

		}
	}


	private void refreshMap() throws Exception{

		map.getOverlays().clear();
		for(int i=0;i<locations.size();i++) {

			LocationInfo iLocationInfo = locations.get(i);
			if(iLocationInfo.timestamp<System.currentTimeMillis()-(2*WT_RED)) {
				locations.remove(i);
			}else {
				Marker locationMarker = new Marker(map);
				//locationMarker.setDefaultIcon();
				Date messageDate = new Date(iLocationInfo.timestamp);
				String titleMessage = iLocationInfo.author.getName();
				if (iLocationInfo.alias != null) {
					titleMessage =
							titleMessage + "(" + iLocationInfo.alias + ")";
				}
				titleMessage = titleMessage + "\r\n" +
						DateFormat.getTimeInstance().format(messageDate);
				locationMarker.setTitle(titleMessage);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					if (iLocationInfo.type ==
							ThreadListActivity.TP_USERPOSITION) {
						Drawable pin =
								getResources()
										.getDrawable(R.drawable.pinlocation);


						if (iLocationInfo.timestamp <
								System.currentTimeMillis() - WT_RED) {
							pin.setAlpha(64);
							pin.setTint(Color.GREEN);

						} else if (iLocationInfo.timestamp <
								System.currentTimeMillis() - WT_YELLOW) {
							pin.setAlpha(128);
							pin.setTint(Color.GREEN);
						} else
							pin.setTint(Color.GREEN);

						locationMarker.setIcon(pin);
					} else if (iLocationInfo.type ==
							ThreadListActivity.TP_USERALERT) {
						Drawable pin =
								getResources().getDrawable(
										R.drawable.warning);
						pin.setTint(Color.RED);

						locationMarker.setIcon(pin);
					} else if (iLocationInfo.type ==
							ThreadListActivity.TP_USERWARNING) {
						Drawable pin =
								getResources().getDrawable(
										R.drawable.warning);
						pin.setTint(Color.YELLOW);
						locationMarker.setIcon(pin);
					} else if (iLocationInfo.type ==
							ThreadListActivity.TP_USERINFO) {
						Drawable pin =
								getResources().getDrawable(
										android.R.drawable.ic_dialog_info);
						pin.setTint(Color.BLUE);
						locationMarker.setIcon(pin);
					}
				}

				locationMarker.setPosition(
						new GeoPoint(iLocationInfo.lat, iLocationInfo.lng));
				locationMarker.setTextLabelFontSize(15);
				locationMarker
						.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

				map.getOverlays().add(locationMarker);
			}

		}
		map.invalidate();
	}

	private Drawable resize(Drawable image) {
		Bitmap b = ((BitmapDrawable)image).getBitmap();
		Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 80, 80, false);
		return new BitmapDrawable(getResources(), bitmapResized);
	}

	@SuppressLint("MissingPermission")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater
				.inflate(R.layout.fragment_map, container, false);
		map = (MapView) view.findViewById(R.id.map);
		map.setTileSource(TileSourceFactory.HIKEBIKEMAP);

		IMapController mapController = map.getController();

		mapController.setZoom(9.5);

		requestPermissionsIfNecessary(this.getActivity(),new String[] {
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_COARSE_LOCATION
		});

		SingleShotLocationProvider.requestSingleUpdate(this.getActivity(),
				new SingleShotLocationProvider.LocationCallback() {
					@Override public void onNewLocationAvailable(GeoPoint location) {
						MapController mMapController = (MapController) map.getController();
						mMapController.setZoom(13);
						mMapController.setCenter(location);
					}
				});
		Runnable runnable=new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						//Mimimum change is yellow wait time
						Thread.sleep(WT_YELLOW);

					if(ThreadMap.this.getView()!=null) {
						ThreadMap.this.getActivity()
								.runOnUiThread(new Runnable() {

									public void run() {
										try {
											refreshMap();
										}
										catch (Exception e){
											
										}
									}
								});
					}
					} catch (Exception e) {

					}
				}
			}

		};


		FloatingActionButton fabCenter=view.findViewById(R.id.fabCenter);
		fabCenter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				centerMap();
			}
		});

		Thread thread=new Thread(runnable);
		thread.start();
		return view;
	}


	private void centerMap(){

		if(locations.size()==0){
			return;
		}
		MapController mMapController = (MapController) map.getController();


			double avgLng=0;
			double avgLat=0;
			for(LocationInfo iLocationInfo:locations){
				avgLng=avgLng+iLocationInfo.lng;
				avgLat=avgLat+iLocationInfo.lat;
			}
			avgLat=avgLat/locations.size();
			avgLng=avgLng/locations.size();
			mMapController.setCenter(new GeoPoint(avgLat,avgLng));

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		ArrayList<String> permissionsToRequest = new ArrayList<>();
		for (int i = 0; i < grantResults.length; i++) {
			permissionsToRequest.add(permissions[i]);
		}
		if (permissionsToRequest.size() > 0) {
			ActivityCompat.requestPermissions(
					this.getActivity(),
					permissionsToRequest.toArray(new String[0]),
					REQUEST_PERMISSIONS_REQUEST_CODE);
		}
	}

	public static void requestPermissionsIfNecessary(Activity activity,String[] permissions) {
		ArrayList<String> permissionsToRequest = new ArrayList<>();
		for (String permission : permissions) {
			if (ContextCompat.checkSelfPermission(activity, permission)
					!= PackageManager.PERMISSION_GRANTED) {
				// Permission is not granted
				permissionsToRequest.add(permission);
			}
		}
		if (permissionsToRequest.size() > 0) {
			ActivityCompat.requestPermissions(
					activity,
					permissionsToRequest.toArray(new String[0]),
					REQUEST_PERMISSIONS_REQUEST_CODE);
		}
	}


	private class LocationInfo{
		double lng;
		double lat;
		double heading;
		long timestamp;
		double speed;
		int type;
		String alias;
		Author author;
	}

}
