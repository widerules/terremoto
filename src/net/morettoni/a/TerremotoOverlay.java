package net.morettoni.a;

import java.util.ArrayList;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class TerremotoOverlay extends Overlay {
	private Cursor terremotiCursor;
	private ArrayList<GeoPoint> terremotiLocations;
	private int rad = 5;
	private int minMag = 3;
	
	public void setMinMag(int newMag) {
		if (newMag != minMag) {
			minMag = newMag;
			refreshTerremoti();
		}
	}

	public TerremotoOverlay(Cursor cursor) {
		super();
		terremotiCursor = cursor;

		terremotiLocations = new ArrayList<GeoPoint>();
		refreshTerremoti();
		terremotiCursor.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				refreshTerremoti();
			}
		});
	}

	private void refreshTerremoti() {
		terremotiLocations.clear();
		if (terremotiCursor.moveToFirst()) {
			do {
				if (terremotiCursor.getDouble(TerremotoProvider.MAGNITUDE_COLUMN) >= minMag) {
					Double lat = terremotiCursor.getFloat(TerremotoProvider.LATITUDE_COLUMN) * 1E6;
					Double lng = terremotiCursor.getFloat(TerremotoProvider.LONGITUDE_COLUMN) * 1E6;
	
					GeoPoint geoPoint = new GeoPoint(lng.intValue(), lat.intValue());
					terremotiLocations.add(geoPoint);
				}
			} while (terremotiCursor.moveToNext());
		}
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		Projection projection = mapView.getProjection();

		// Create and setup your paint brush
		Paint paint = new Paint();
		paint.setARGB(250, 255, 0, 0);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);

		if (shadow == false) {
			for (GeoPoint point : terremotiLocations) {

				Point myPoint = new Point();
				projection.toPixels(point, myPoint);

				RectF oval = new RectF(myPoint.x - rad, myPoint.y - rad,
						myPoint.x + rad, myPoint.y + rad);

				canvas.drawOval(oval, paint);
			}
		}
	}
}