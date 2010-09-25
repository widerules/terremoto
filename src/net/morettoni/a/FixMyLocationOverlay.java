package net.morettoni.a;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import android.content.Context;
import android.graphics.Canvas;

/*
 * Fix for:
 * http://groups.google.com/group/android-developers/browse_thread/thread/43615742f462bbf1/8918ddfc92808c42?
 */
public class FixMyLocationOverlay extends MyLocationOverlay {
	public FixMyLocationOverlay(Context context, MapView mapView) {
        super(context, mapView);
    }

	@Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
        try {
            return super.draw(canvas, mapView, shadow, when);
        } catch (ClassCastException e) {
            return false;
        }
    }
}
