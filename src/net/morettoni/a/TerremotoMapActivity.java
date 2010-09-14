package net.morettoni.a;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class TerremotoMapActivity extends MapActivity {
	private Cursor terremotiCursor;
	private TerremotiReceiver receiver;
	private MapView mapView;
    private MapController mc;
    private MyLocationOverlay myLocationOverlay;

	public class TerremotiReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			terremotiCursor.requery();
			MapView mapView = (MapView) findViewById(R.id.terremotiMap);
			mapView.invalidate();
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.terremotomap);

		Uri terremotiURI = TerremotoProvider.CONTENT_URI;
		terremotiCursor = getContentResolver().query(terremotiURI, null,
				null, null, null);

		mapView = (MapView) findViewById(R.id.terremotiMap);
		mc = mapView.getController();
		
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();

		TerremotoOverlay eo = new TerremotoOverlay(terremotiCursor);
		mapView.getOverlays().add(eo);
		mapView.getOverlays().add(myLocationOverlay);
		
		mapView.displayZoomControls(true);
		mapView.setSatellite(true);
		
		myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
            	mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            	mapView.getController().setZoom(16);
            }
        });		
		
        mapView.invalidate();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_3:
            mc.zoomIn();
            break;
        case KeyEvent.KEYCODE_1:
            mc.zoomOut();
            break;
        }
        return super.onKeyDown(keyCode, event);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onResume() {
		terremotiCursor.requery();

		IntentFilter filter;
		filter = new IntentFilter(TerremotoService.NUOVO_TERREMOTO);
		receiver = new TerremotiReceiver();
		registerReceiver(receiver, filter);

		super.onResume();
	}

	@Override
	public void onPause() {
		terremotiCursor.deactivate();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		terremotiCursor.close();
		super.onDestroy();
	}
}
