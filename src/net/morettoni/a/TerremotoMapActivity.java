package net.morettoni.a;

import android.os.Bundle;

import com.google.android.maps.MapActivity;

public class TerremotoMapActivity extends MapActivity {
	@Override
	protected void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	    setContentView(R.layout.terremotomap);
	 }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
