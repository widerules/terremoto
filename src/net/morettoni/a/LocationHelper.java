package net.morettoni.a;

import java.util.Date;

import android.location.Location;

public class LocationHelper {
	private static long LOCATION_UPDATE_FREQ = 15L * 60L * 1000L;
	
	public static Location getBestLocation(Location oldLocation, Location newLocation) {
		return getBestLocation(oldLocation, newLocation, LOCATION_UPDATE_FREQ);
	}
	public static Location getBestLocation(Location oldLocation, Location newLocation, long freq) {
		if (newLocation != null && oldLocation == null) {
			return newLocation;
		} else if (newLocation == null) {
			return null;
		}

		long now = new Date().getTime();
		long locationUpdateDelta = now - newLocation.getTime();
		long lastLocationUpdateDelta = now - oldLocation.getTime();
		boolean locationIsInTimeThreshold = locationUpdateDelta <= freq;
		boolean lastLocationIsInTimeThreshold = lastLocationUpdateDelta <= freq;
//		boolean locationIsMostRecent = locationUpdateDelta <= lastLocationUpdateDelta;

		boolean accuracyComparable = newLocation.hasAccuracy()
				|| oldLocation.hasAccuracy();
		boolean locationIsMostAccurate = false;
		if (accuracyComparable) {
			if (newLocation.hasAccuracy() && !oldLocation.hasAccuracy()) {
				locationIsMostAccurate = true;
			} else if (!newLocation.hasAccuracy() && oldLocation.hasAccuracy()) {
				locationIsMostAccurate = false;
			} else {
				locationIsMostAccurate = newLocation.getAccuracy() <= oldLocation
						.getAccuracy();
			}
		}

		if (accuracyComparable && locationIsMostAccurate
				&& locationIsInTimeThreshold) {
			return newLocation;
		} else if (locationIsInTimeThreshold && !lastLocationIsInTimeThreshold) {
			return newLocation;
		}
		
		return oldLocation;
	}

}
