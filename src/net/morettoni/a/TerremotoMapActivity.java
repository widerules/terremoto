package net.morettoni.a;

import net.morettoni.a.beans.Terremoto;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MapView.LayoutParams;

public class TerremotoMapActivity extends MapActivity implements OnSharedPreferenceChangeListener {
	public static final String CENTER_TERREMOTO = "net.morettoni.terremoto.CENTER_TERREMOTO";
	private Cursor terremotiCursor;
	private TerremotiReceiver terremotiReceiver;
	private CenterTerremotoReceiver centerTerremotiReceiver;
	private MapView mapView;
	private FixMyLocationOverlay myLocationOverlay;
	private TerremotoItemizedOverlay terremotoItemizedOverlay;
	private int minMag = 3;
	private int maxPins = 10;
	private NotificationManager notificationManager;

	public class TerremotiReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			notificationManager.cancel(TerremotoService.NOTIFICATION_ID);
			
			refreshTerremoti();
			MapView mapView = (MapView) findViewById(R.id.terremotiMap);
			mapView.invalidate();
		}
	}

	public class CenterTerremotoReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			Terremoto terremoto = new Terremoto();
			terremoto.setLatitudine(intent.getDoubleExtra(TerremotoProvider.KEY_LAT, 0.0));
			terremoto.setLongitudine(intent.getDoubleExtra(TerremotoProvider.KEY_LNG, 0.0));
			terremoto.setLuogo(intent.getStringExtra(TerremotoProvider.KEY_WHERE));
			terremoto.setMagnitude(intent.getDoubleExtra(TerremotoProvider.KEY_MAG, 0.0));
			terremotoItemizedOverlay.addOverlay(terremoto);

			MapView mapView = (MapView) findViewById(R.id.terremotiMap);
			mapView.getController().animateTo(terremoto.getGeoPoint());
			mapView.getController().setZoom(10);
			mapView.invalidate();
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.terremotomap);

		Uri terremotiURI = TerremotoProvider.CONTENT_URI;
		terremotiCursor = getContentResolver().query(terremotiURI, null, null,
				null, null);

		mapView = (MapView) findViewById(R.id.terremotiMap);

		myLocationOverlay = new FixMyLocationOverlay(this, mapView);
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);
		minMag = Integer.parseInt(prefs.getString("PREF_MIN_MAG", "3"));
		maxPins = Integer.parseInt(prefs.getString("PREF_MAP_PINS", "10"));
		
		Drawable defaultMarker = getResources().getDrawable(R.drawable.map_marker_blue); 
		defaultMarker.setBounds(0, 0, defaultMarker.getIntrinsicWidth(), 
		    defaultMarker.getIntrinsicHeight()); 
		terremotoItemizedOverlay = new TerremotoItemizedOverlay(defaultMarker);
		mapView.getOverlays().add(terremotoItemizedOverlay);
		mapView.getOverlays().add(myLocationOverlay);

		LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.layout_zoom);
		View zoomView = mapView.getZoomControls();
		zoomLayout.addView(zoomView, new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mapView.displayZoomControls(true);
		mapView.setSatellite(true);
		mapView.getController().setZoom(7);
		
	    String svcName = Context.NOTIFICATION_SERVICE;
	    notificationManager = (NotificationManager)getSystemService(svcName);		

		refreshTerremoti();
		mapView.invalidate();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("PREF_MIN_MAG")) {
			int mag = Integer.parseInt(sharedPreferences.getString("PREF_MIN_MAG", "3"));
			if (minMag != mag) {
				minMag = mag;
				refreshTerremoti();
				mapView.invalidate();
			}
		}

		if (key.equals("PREF_MAP_PINS")) {
			int pins = Integer.parseInt(sharedPreferences.getString("PREF_MAP_PINS", "10"));
			if (maxPins != pins) {
				maxPins = pins;
				refreshTerremoti();
				mapView.invalidate();
			}
		}
	}
	
	private void refreshTerremoti() {
		Terremoto terremoto;
		double mag;
		int pins = maxPins;
		
		terremotoItemizedOverlay.clear();
		terremotiCursor.requery();
		if (terremotiCursor.moveToFirst()) {
			do {
				mag = terremotiCursor.getDouble(TerremotoProvider.MAGNITUDE_COLUMN);
				if (mag >= minMag) {
					terremoto = new Terremoto();
					terremoto.setLatitudine(terremotiCursor.getFloat(TerremotoProvider.LATITUDE_COLUMN));
					terremoto.setLongitudine(terremotiCursor.getFloat(TerremotoProvider.LONGITUDE_COLUMN));
					terremoto.setLuogo(terremotiCursor.getString(TerremotoProvider.WHERE_COLUMN));
					terremoto.setMagnitude(mag);
					terremotoItemizedOverlay.addOverlay(terremoto);
					pins--;
				}
			} while (terremotiCursor.moveToNext() && pins > 0);
		}
	}	
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onResume() {
		terremotiCursor.requery();

		notificationManager.cancel(TerremotoService.NOTIFICATION_ID);
		
		IntentFilter filter;
		filter = new IntentFilter(TerremotoService.NUOVO_TERREMOTO);
		terremotiReceiver = new TerremotiReceiver();
		registerReceiver(terremotiReceiver, filter);
		
		filter = new IntentFilter(CENTER_TERREMOTO);
		centerTerremotiReceiver = new CenterTerremotoReceiver();
		registerReceiver(centerTerremotiReceiver, filter);		

		if (myLocationOverlay != null) {
			myLocationOverlay.enableCompass();
			myLocationOverlay.enableMyLocation();
		}

		super.onResume();
	}

	@Override
	public void onPause() {
		terremotiCursor.deactivate();
		if (myLocationOverlay != null) {
			myLocationOverlay.disableCompass();
			myLocationOverlay.disableMyLocation();
		}

		super.onPause();
	}

	@Override
	public void onDestroy() {
		terremotiCursor.close();
		if (myLocationOverlay != null) {
			myLocationOverlay.disableCompass();
			myLocationOverlay.disableMyLocation();
		}
		
		super.onDestroy();
	}
}
