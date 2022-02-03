package org.briarproject.briar.android.threaded;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.data.BdfList;
import org.briarproject.bramble.api.data.BdfReader;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.location.event.MarkerAddedEvent;
import org.briarproject.bramble.api.location.event.MarkerRemovedEvent;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.bramble.api.sync.event.LocationMessageEvent;
import org.briarproject.bramble.data.BdfReaderFactoryImpl;
import org.briarproject.briar.R;
import org.briarproject.briar.android.location.LocationInfo;
import org.briarproject.briar.android.location.LocationMessageProducer;
import org.briarproject.briar.android.privategroup.conversation.GroupMessageItem;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import it.beppi.tristatetogglebutton_library.TriStateToggleButton;

public class ThreadMap extends Fragment {

	public interface UserInteractionListener{
		public void onMarkerAdded();
	}

	private Random rndID=new Random();

	public static final String LOCATION_IDENTIFIER = "{\"type\":\"location\"";
	public static final String MARKER_IDENTIFIER = "{\"type\":\"marker\"";

	private static final int WT_RED = 60 * 1000 * 10; //Older than 10 minutes
	private static final int WT_YELLOW = 60 * 1000; //Older than 1 minutes
	public static final int AM_DELETE=-1;
	public static final int AM_SELECT=0;
	public static final int AM_ADDINFORMATION=1;
	public static final int AM_ADDWARNING=2;
	public static final int AM_ADDALERT=3;
	public static final int AM_ADDMEETING=4;
	private LocationInfo selectedLocationInfo=null;
	private TableLayout loEditMarker;
	private int actionMode=AM_SELECT;
	private UserInteractionListener userInteractionListener;
	private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
	private MapView map = null;
	private ArrayList<LocationInfo> locations = new ArrayList<LocationInfo>();
	private ThreadListViewModel viewModel;
	private boolean admin=false;

	public void setUserInteractionListener(
			UserInteractionListener userInteractionListener) {
		this.userInteractionListener = userInteractionListener;
	}

	public MapView getMap(){
		return map;
	}

	public void setAdmin(boolean admin){
		this.admin=admin;
	}

	public ThreadMap(ThreadListViewModel viewModel){
		super();
		this.viewModel=viewModel;

	}

	public void setActionMode(int mode){
		actionMode=mode;
	}

	private String getMessageText(BdfList body) throws FormatException {
		// Message type (0), member (1), parent ID (2), previous message ID (3),
		// text (4), signature (5)
		return body.getString(4);
	}

	public BdfList toList(byte[] b, int off, int len) throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		BdfReaderFactoryImpl bdfReaderFactory=  new BdfReaderFactoryImpl();
		BdfReader reader = bdfReaderFactory.createReader(in);
		try {
			BdfList list = reader.readList();
			if (!reader.eof()) throw new FormatException();
			return list;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BdfList toList(byte[] b) throws FormatException {
		return toList(b, 0, b.length);
	}

	public BdfList toList(Message m) throws FormatException {
		return toList(m.getBody());
	}

	public void handleMarkerMessage(GroupMessageItem message){

		if (message.getText().startsWith(MARKER_IDENTIFIER)) {
			LocationInfo locationInfo=LocationInfo.parseLocationInfo(message.getText());
			locationInfo.admin=true;
			boolean found=false;
			for (int i = 0; i < locations.size(); i++) {

				if (locations.get(i).admin &&
						locations.get(i).id!=null && locations.get(i).id.equals(locationInfo.id)) {
					locations.set(i, locationInfo);
					found = true;
				}
			}
			if (!found) {
				locations.add(locationInfo);
			}
		}
		if(getContext()!=null){
			refreshMap();
		}
	}


	public void handleLocationMessage(LocationMessageEvent event,Author author,String alias)
			throws Exception {
		if (getContext() == null) return;
		String text=getMessageText(toList(event.getMessage()));
		if (text.startsWith(LOCATION_IDENTIFIER)) {
			LocationInfo locationInfo=LocationInfo.parseLocationInfo(text);
			locationInfo.author = author;
			locationInfo.alias = alias;
			locationInfo.timestamp = event.getMessage().getTimestamp();
			boolean found = false;

			for (int i = 0; i < locations.size(); i++) {

				if (!locations.get(i).admin &&
						locations.get(i).author!=null && locations.get(i).author.equals(author) &&
						locations.get(i).type.equals(
								LocationInfo.LocationInfoType.USERPOSITION)) {
					locations.set(i, locationInfo);
					found = true;
				}
			}
			if (!found) {
				locations.add(locationInfo);
			}
			refreshMap();

		}
	}

	public void handleLocationMessage(GroupMessageItem message)
			throws Exception {
		if (getContext() == null) return;
		if (message.getText().startsWith(LOCATION_IDENTIFIER)) {

			LocationInfo locationInfo=LocationInfo.parseLocationInfo(message.getText());
			locationInfo.timestamp=message.getTimestamp();
			boolean found = false;
			for (int i = 0; i < locations.size(); i++) {

				if (locations.get(i).author.equals(message.getAuthor()) &&
						locations.get(i).type.equals(
								LocationInfo.LocationInfoType.USERPOSITION)) {
					locations.set(i, locationInfo);
					found = true;
				}
			}
			if (!found) {
				locations.add(locationInfo);
			}
			refreshMap();

		}
	}


	private LocationListener locationListener;

	private void handleLocation() {
		LocationManager locationManager =
				(LocationManager) this.getActivity()
						.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		String provider = locationManager.getBestProvider(criteria, true);
		boolean hasLocation=false;
		if (provider != null) {
			if (ActivityCompat.checkSelfPermission(this.getActivity(),
					Manifest.permission.ACCESS_FINE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED && ActivityCompat
					.checkSelfPermission(this.getActivity(),
							Manifest.permission.ACCESS_COARSE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED) {
				return;
			}
			Location location = locationManager.getLastKnownLocation(provider);
			if(location!=null){
				GeoPoint geoPoint=new GeoPoint(location.getLatitude(),location.getLongitude());
				map.getController().animateTo(geoPoint);

				hasLocation=true;
			}
		}
		if(!hasLocation) {
			locationListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					try {
						GeoPoint geoPoint = new GeoPoint(location.getLatitude(),
								location.getLongitude());
						map.getController().animateTo(geoPoint);
						locationManager.removeUpdates(locationListener);
					} catch (Exception e) {
					}
				}

				public void onStatusChanged(String provider, int status,
						Bundle extras) {
				}

				public void onProviderEnabled(String provider) {
				}

				public void onProviderDisabled(String provider) {
				}
			};
			ThreadMap.requestPermissionsIfNecessary(this.getActivity(),
					new String[] {
							Manifest.permission.ACCESS_FINE_LOCATION,
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.ACCESS_COARSE_LOCATION});
			if (ActivityCompat.checkSelfPermission(this.getActivity(),
					Manifest.permission.ACCESS_FINE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED && ActivityCompat
					.checkSelfPermission(this.getActivity(),
							Manifest.permission.ACCESS_COARSE_LOCATION) !=
					PackageManager.PERMISSION_GRANTED) {

				return;
			}
			locationManager.requestLocationUpdates(provider, 10000, 10,
					locationListener);
		}

	}

	protected void onMarkerAdded(MarkerAddedEvent mae){
			LocationInfo locationInfo=LocationInfo.parseLocationInfo(mae.getText());
			locationInfo.admin=true;
			boolean found=false;
			for (int i = 0; i < locations.size(); i++) {

				if (locations.get(i).admin &&
						locations.get(i).id!=null && locations.get(i).id.equals(locationInfo.id)) {
					locations.set(i, locationInfo);
					found = true;
				}
			}
			if (!found) {
				locations.add(locationInfo);
			}

		if(getContext()!=null){
			refreshMap();
		}
		refreshMap();
	}


	protected void onMarkerRemoved(MarkerRemovedEvent mre){
		for(LocationInfo locationInfo:locations){
			if(locationInfo.id!=null && locationInfo.id.equals(mre.getMarkerID())){
				locations.remove(locationInfo);
			}
		}
		refreshMap();
	}

	protected void refreshMap(){
		try {
			map.getOverlays().clear();
			map.getOverlays().add(new MapEventsOverlay(mReceive));
			for (int i = 0; i < locations.size(); i++) {

				LocationInfo iLocationInfo = locations.get(i);
				if (iLocationInfo.timestamp > 0 &&
						System.currentTimeMillis() - iLocationInfo.timestamp >
								(2 * WT_RED)) {
					if (locations.get(i).marker != null &&
							locations.get(i).marker.getInfoWindow() != null) {
						locations.get(i).marker.getInfoWindow().close();
					}
					locations.remove(i);
				} else {
					Marker locationMarker = new Marker(map);
					String titleMessage = "";

					//locationMarker.setDefaultIcon();
					if (iLocationInfo.message != null) {
						titleMessage = iLocationInfo.message;
					} else {
						Date messageDate = new Date(iLocationInfo.timestamp);
						if (iLocationInfo.author != null) {
							titleMessage =
									titleMessage +
											iLocationInfo.author.getName();
						}
						if (iLocationInfo.alias != null) {
							titleMessage =
									titleMessage + "(" + iLocationInfo.alias +
											")";
						}
						titleMessage = titleMessage + "\r\n" +
								DateFormat.getTimeInstance()
										.format(messageDate);
					}
					locationMarker.setTitle(titleMessage);


					if (iLocationInfo.type
							.equals(LocationInfo.LocationInfoType.USERPOSITION)) {
						int size = 64;


						Drawable pin = resize(getResources()
								.getDrawable(R.drawable.pinlocation), size);
						locationMarker
								.setRotation((float) iLocationInfo.bearing);
						if (System.currentTimeMillis() -
								iLocationInfo.timestamp > WT_RED) {
							pin.setAlpha(64);


						} else if (System.currentTimeMillis() -
								iLocationInfo.timestamp >
								WT_YELLOW) {
							pin.setAlpha(128);

						}


						locationMarker.setIcon(pin);
					} else {
						int size = 32;
						if (iLocationInfo.size > 0 && iLocationInfo.size <= 5) {
							size = 32 + (iLocationInfo.size - 2) * 5;
						}
						if (iLocationInfo.type.equals(
								LocationInfo.LocationInfoType.USERALERT)) {
							Drawable pin;
							if(admin && iLocationInfo.admin) {
								pin = resize(getResources().getDrawable(
										R.drawable.alert_icon_admin), size);
							}else{
								pin = resize(getResources().getDrawable(
										R.drawable.alert_icon), size);
							}


							locationMarker.setIcon(pin);
						} else if (iLocationInfo.type.equals(
								LocationInfo.LocationInfoType.USERWARNING)) {
							Drawable pin;
							if(admin && iLocationInfo.admin) {
								pin = resize(getResources().getDrawable(
										R.drawable.warning_icon_admin), size);
							}else{
								pin = resize(getResources().getDrawable(
										R.drawable.warning_icon), size);
							}
							locationMarker.setIcon(pin);

						} else if (iLocationInfo.type.equals(
								LocationInfo.LocationInfoType.USERINFO)) {
							Drawable pin;
							if(admin && iLocationInfo.admin) {
								pin = resize(getResources().getDrawable(
										R.drawable.information_icon_admin), size);
							}else{
								pin = resize(getResources().getDrawable(
										R.drawable.information_icon), size);
							}
							locationMarker.setIcon(pin);
						}else if (iLocationInfo.type.equals(
								LocationInfo.LocationInfoType.USERMEETING)) {
							Drawable pin =
									resize(getResources().getDrawable(
											R.drawable.meeting_point), size);

							locationMarker.setIcon(pin);
						}
						if (iLocationInfo.size == 0) {
							iLocationInfo.size = 2;
						}
						if (iLocationInfo.admin) {
							if(admin) {
								locationMarker.setDraggable(true);
								locationMarker.setOnMarkerClickListener(

										new Marker.OnMarkerClickListener() {

											@Override
											public boolean onMarkerClick(
													Marker marker,
													MapView mapView) {

												handleMarkerClick(marker);
												return false;
											}
										});
								locationMarker.setOnMarkerDragListener(
										new Marker.OnMarkerDragListener() {
											@Override
											public void onMarkerDrag(
													Marker marker) {

											}

											@Override
											public void onMarkerDragEnd(
													Marker marker) {
												iLocationInfo.lat =
														marker.getPosition()
																.getLatitude();
												iLocationInfo.lng =
														marker.getPosition()
																.getLongitude();

												viewModel.createAndStoreMessage(LocationMessageProducer.buildMarkerMessage(iLocationInfo,LocationMessageProducer.Actions.SET),null);

											}

											@Override
											public void onMarkerDragStart(
													Marker marker) {

											}
										});
							}
						} else {
							if(admin) {
								locationMarker.setOnMarkerClickListener(
										new Marker.OnMarkerClickListener() {
											@Override
											public boolean onMarkerClick(
													Marker marker,
													MapView mapView) {
												handleClientMarkerClick(marker);

												return false;
											}
										});

							}
						}
					}
					//locationMarker.setDefaultIcon();
					locationMarker.setAnchor(Marker.ANCHOR_CENTER,
							Marker.ANCHOR_CENTER);
					locationMarker.setPosition(
							new GeoPoint(iLocationInfo.lat, iLocationInfo.lng));
					//locationMarker.setTextLabelFontSize(15);
					iLocationInfo.marker = locationMarker;
					map.getOverlays().add(locationMarker);
				}

			}
			map.invalidate();
		}
		catch (Exception e){
			Log.d(ThreadMap.class.getName(),e.getMessage());
		}
	}




	@SuppressLint("MissingPermission")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater
				.inflate(R.layout.fragment_map, container, false);
		ApplicationInfo applicationInfo = this.getContext().getApplicationInfo();
		Configuration.getInstance().setUserAgentValue(getString(applicationInfo.labelRes));
		map = (MapView) view.findViewById(R.id.map);

		map.setTileSource(TileSourceFactory.WIKIMEDIA);
		//map.setTileSource(TileSourceFactory.HIKEBIKEMAP);
		map.setMultiTouchControls(true);

		loEditMarker=view.findViewById(R.id.loEditMarker);
		Button bDelete=view.findViewById(R.id.btnDelete);
		bDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loEditMarker.setVisibility(View.GONE);
				deleteAdminMarker();
			}
		});
		TriStateToggleButton btnMarkerAction=view.findViewById(R.id.markerAction);
		btnMarkerAction.setOnToggleChanged(
				new TriStateToggleButton.OnToggleChanged() {
					@Override
					public void onToggle(
							TriStateToggleButton.ToggleStatus toggleStatus,
							boolean booleanToggleStatus, int toggleIntValue) {
						switch(toggleStatus){
							case off:
								if(selectedLocationInfo!=null){
									LinearLayout loMarkerActions=view.findViewById(R.id.loMarkerAction);
									loMarkerActions.animate()
											.alpha(0f)
											.setDuration(500)
											.setListener(new AnimatorListenerAdapter() {
												@Override
												public void onAnimationEnd(
														Animator animation) {
													locations.remove(selectedLocationInfo);
													loMarkerActions.setAlpha(1.0f);
													loMarkerActions.setVisibility(View.GONE);
													refreshMap();
												}
											});



								}
								break;
							case on:
								shareMarker(selectedLocationInfo);
								break;
						}
					}
				});

		Button bOk=view.findViewById(R.id.btnOK);
		bOk.setOnClickListener(new View.OnClickListener() {
			                       @Override
			                       public void onClick(View v) {
				                       loEditMarker.setVisibility(View.GONE);
				                       if(selectedLocationInfo!=null){
				                       	SeekBar sbSize=getView().findViewById(R.id.sbSize);
				                       	selectedLocationInfo.size=sbSize.getProgress();
				                       	EditText edtMessage=getView().findViewById(R.id.edtMessage);
				                       	selectedLocationInfo.message=edtMessage.getText().toString();
				                       	viewModel.createAndStoreMessage(LocationMessageProducer.buildMarkerMessage(selectedLocationInfo,LocationMessageProducer.Actions.SET),null);

					                       refreshMap();


				                       }
			                       }
		                       }
		);
		IMapController mapController = map.getController();

		mapController.setZoom(9.5);
		/*map.getZoomController().setOnZoomListener(
				new CustomZoomButtonsController.OnZoomListener() {
					@Override
					public void onVisibilityChanged(boolean b) {

					}

					@Override
					public void onZoom(boolean b) {
						try {
							refreshMap();
						}
						catch(Exception e){}
					}
				});*/
		requestPermissionsIfNecessary(this.getActivity(),new String[] {
				Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_COARSE_LOCATION
		});
		handleLocation();

		Runnable runnable= () -> {
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








	private Drawable getRotateDrawable(final Drawable d, final float angle) {
		final Drawable[] arD = { d };
		return new LayerDrawable(arD) {
			@Override
			public void draw(final Canvas canvas) {
				canvas.save();
				canvas.rotate(angle, d.getBounds().width() / 2, d.getBounds().height() / 2);
				super.draw(canvas);
				canvas.restore();
			}
		};
	}

	public static Bitmap RotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	private Drawable resize(Drawable image,int size) {
		Bitmap b = ((BitmapDrawable)image).getBitmap();
		double density=getResources().getDisplayMetrics().density;
		Bitmap bitmapResized = Bitmap.createScaledBitmap(b, (int)(size*density), (int)(size*density), false);
		bitmapResized.setDensity(Bitmap.DENSITY_NONE);
		BitmapDrawable drawable=new BitmapDrawable(getResources(), bitmapResized);

		drawable.setBounds(0,0,(int)(size*density), (int)(size*density));
		return drawable;
	}

	final MapEventsReceiver mReceive = new MapEventsReceiver(){
		@Override
		public boolean singleTapConfirmedHelper(GeoPoint p) {
			if(actionMode<=0){
				return false;
			}
			LocationInfo locationInfo = new LocationInfo();
			locationInfo.lng = p.getLongitude();
			locationInfo.lat = p.getLatitude();
			locationInfo.timestamp = 0;
			locationInfo.admin =true;
			locationInfo.id="#"+rndID.nextInt()+
					locationInfo.lng+
					locationInfo.lat;
			switch(actionMode){
				case AM_ADDALERT:
					locationInfo.type = LocationInfo.LocationInfoType.USERALERT;
					break;
				case AM_ADDWARNING:
					locationInfo.type = LocationInfo.LocationInfoType.USERWARNING;
					break;
				case AM_ADDINFORMATION:
					locationInfo.type = LocationInfo.LocationInfoType.USERINFO;
					break;
				case AM_ADDMEETING:
					locationInfo.type = LocationInfo.LocationInfoType.USERMEETING;
					break;
			}
			locations.add(locationInfo);
			viewModel.createAndStoreMessage(LocationMessageProducer.buildMarkerMessage(locationInfo,LocationMessageProducer.Actions.ADD),null);
			refreshMap();
			if(userInteractionListener!=null){
				userInteractionListener.onMarkerAdded();
			}
			actionMode=AM_SELECT;
			return false;
		}
		@Override
		public boolean longPressHelper(GeoPoint p) {
			return false;
		}
	};


	private void handleMarkerClick(Marker marker){
		LocationInfo locationInfo=findLocationInfo(marker);
		if(locationInfo!=null){
			selectedLocationInfo=locationInfo;

			getView().findViewById(R.id.loMarkerAction).setVisibility(View.GONE);

			loEditMarker.setVisibility(View.VISIBLE);
			EditText edtMessage=getView().findViewById(R.id.edtMessage);
			if(locationInfo.message!=null) {
				edtMessage.setText(locationInfo.message);
			}else{
				edtMessage.setText("");
			}
			SeekBar sbSize=getView().findViewById((R.id.sbSize));
			sbSize.setProgress(locationInfo.size);
		}
	}

	private LocationInfo findLocationInfo(Marker marker){
		for(LocationInfo iLocationInfo:locations){
			if(iLocationInfo.marker!=null && iLocationInfo.marker.equals(marker)){
				return iLocationInfo;
			}
		}
		return null;
	}

	private void handleClientMarkerClick(Marker marker){
		LocationInfo locationInfo=findLocationInfo(marker);
		if(locationInfo!=null) {
			selectedLocationInfo=locationInfo;
			TriStateToggleButton button=(TriStateToggleButton) getView().findViewById(R.id.markerAction);
			button.setToggleStatus(TriStateToggleButton.ToggleStatus.mid);
			getView().findViewById(
					R.id.loEditMarker)
					.setVisibility(View.GONE);
			getView().findViewById(
					R.id.loMarkerAction)
					.setVisibility(
							View.VISIBLE);

		}
	}

	private void shareMarker(LocationInfo locationInfo){
		if(locationInfo!=null){
			LocationInfo mLocationInfo = new LocationInfo();
			mLocationInfo.lng = locationInfo.lng;
			mLocationInfo.lat = locationInfo.lat;
			mLocationInfo.timestamp = 0;
			mLocationInfo.admin =true;
			mLocationInfo.id="#"+rndID.nextInt()+
					locationInfo.lng+
					locationInfo.lat;
			mLocationInfo.type = locationInfo.type;
			locations.add(mLocationInfo);
			viewModel.createAndStoreMessage(LocationMessageProducer.buildMarkerMessage(mLocationInfo,LocationMessageProducer.Actions.ADD),null);
			locations.remove(locationInfo);
			LinearLayout loMarkerActions=getView().findViewById(R.id.loMarkerAction);
			loMarkerActions.animate()
					.alpha(0f)
					.setDuration(500)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(
								Animator animation) {
							loMarkerActions.setAlpha(1.0f);
							loMarkerActions.setVisibility(View.GONE);
							refreshMap();
						}
					});



		}
	}
	public void deleteAdminMarker(){
		if(selectedLocationInfo!=null){
			viewModel.createAndStoreMessage(LocationMessageProducer.buildMarkerMessage(selectedLocationInfo,LocationMessageProducer.Actions.DELETE),null);
			locations.remove(selectedLocationInfo);
			refreshMap();
		}
	}
}
