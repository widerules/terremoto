package net.morettoni.a;

import java.util.ArrayList;

import net.morettoni.a.beans.Terremoto;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class TerremotoItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<Terremoto> terremoti;

	public TerremotoItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));

		terremoti = new ArrayList<Terremoto>();
		populate();
	}

	public void addOverlay(Terremoto overlay) {
		terremoti.add(overlay);
		setLastFocusedIndex(-1);
		populate();
	}

	public void clear() {
		terremoti.clear();
		setLastFocusedIndex(-1);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		Terremoto terremoto = terremoti.get(i);
		OverlayItem oi = new OverlayItem(terremoto.getGeoPoint(), "Terremoto",
				String.format("%s: %.1f", terremoto.getLuogo(), terremoto
						.getMagnitude()));
		return oi;
	}
	
	@Override
	public int size() {
		return terremoti.size();
	}
}