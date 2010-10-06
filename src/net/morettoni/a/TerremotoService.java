package net.morettoni.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import net.morettoni.a.beans.Terremoto;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class TerremotoService extends Service implements
		OnSharedPreferenceChangeListener, LocationListener {

	public static final String NUOVO_TERREMOTO = "Nuovo_Terremoto";
	public static final String TERREMOTI_TIMER = "TerremotiTimer";
	public static String LISTA_TERREMOTI_AGGIORNATA = "net.morettoni.terremoto.nuovi_terremoti";
	public static final int NOTIFICATION_ID = 1;
	private static final long LOCATION_UPDATE_FREQ = 15L * 60L * 1000L;
	private TerremotoLookupTask lastLookup = null;
	private AlarmManager alarms;
	private PendingIntent alarmIntent;
	private Notification terremotoNotification;
	private long lastNotifiedEventDate = 0L;
	private int minMag = 3;
	private int maxDist = 200;
	private boolean vibrateNotify = true;
	private long updateFreq = 30;
	private Location currentLocation;

	@Override
	public void onCreate() {
		alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		Intent intentToFire = new Intent(TerremotoAlarmReceiver.TERREMOTI_ALARM);
		alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

		terremotoNotification = new Notification(R.drawable.icon,
				"Nuovo terremoto!", System.currentTimeMillis());
	}

	private class TerremotoLookupTask extends AsyncTask<Void, Terremoto, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			String feed = getString(R.string.feed);
			try {
				URL url = new URL(feed);
				HttpURLConnection httpConnection = (HttpURLConnection) url
						.openConnection();
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(httpConnection
									.getInputStream()));
					String str;
					String[] event;
					Terremoto terremoto;
					boolean firstLine = true;
					SimpleDateFormat df = new SimpleDateFormat(
							"dd/MM/yyyy HH:mm:ss");

					while ((str = in.readLine()) != null) {
						if (firstLine) {
							firstLine = false;
							continue;
						}

						event = str.split(",");

						terremoto = new Terremoto();
						terremoto.setLatitudine(Double.parseDouble(event[0]));
						terremoto.setLongitudine(Double.parseDouble(event[1]));
						terremoto.setProfondita(Double.parseDouble(event[2]));
						try {
							df.setTimeZone(TimeZone.getTimeZone("UTC"));
							terremoto.setData(df.parse(event[3]));
							df.setTimeZone(TimeZone.getDefault());
						} catch (ParseException e) {
						}
						terremoto.setMagnitude(Double.parseDouble(event[4]));
						terremoto.setLuogo(event[5].replaceAll("_", " "));
						terremoto.setId(Long.parseLong(event[6]));

						if (aggiungi(terremoto))
							publishProgress(terremoto);
						else
							break;
					}
					in.close();
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Terremoto... values) {
			Terremoto terremoto = values[0];

			if (terremoto.getData().getTime() > lastNotifiedEventDate
					&& terremoto.getMagnitude() >= minMag) {
				float distance = -1.0F;
				if (currentLocation != null && maxDist > 0) {
					Location event = new Location("dummy");
					event.setLatitude(terremoto.getLatitudine());
					event.setLongitude(terremoto.getLongitudine());

					distance = event.distanceTo(currentLocation) / 1000.0F;

					if (maxDist > 0 && distance > maxDist)
						return;
				}
				lastNotifiedEventDate = terremoto.getData().getTime();

				String svcName = Context.NOTIFICATION_SERVICE;
				NotificationManager notificationManager;
				notificationManager = (NotificationManager) getSystemService(svcName);

				Context context = getApplicationContext();
				SimpleDateFormat df = new SimpleDateFormat("HH:mm dd/MM/yyyy");
				String dateText = df.format(terremoto.getData());
				String titleText;
				if (distance < 0)
					titleText = String.format("%s: %.1f", terremoto.getLuogo(),
							terremoto.getMagnitude());
				else
					titleText = String.format("%s: %.1f (dist. %.0fkm)",
							terremoto.getLuogo(), terremoto.getMagnitude(),
							distance);
				Intent startActivityIntent = new Intent(TerremotoService.this,
						TerremotoActivity.class);
				PendingIntent launchIntent = PendingIntent.getActivity(context,
						0, startActivityIntent, 0);

				terremotoNotification.tickerText = titleText;
				terremotoNotification.setLatestEventInfo(context, titleText,
						dateText, launchIntent);
				terremotoNotification.when = java.lang.System
						.currentTimeMillis();
				terremotoNotification.defaults = Notification.DEFAULT_SOUND
						| Notification.DEFAULT_LIGHTS;

				if (vibrateNotify) {
					long[] vibrate = new long[] { 1000, 500, 1000 };
					terremotoNotification.vibrate = vibrate;
				} else {
					terremotoNotification.vibrate = null;
				}

				notificationManager.notify(NOTIFICATION_ID,
						terremotoNotification);
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			sendBroadcast(new Intent(LISTA_TERREMOTI_AGGIORNATA));
			stopSelf();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);

		updatePreferences(prefs);

		aggiornaTerremoti();

		return Service.START_NOT_STICKY;
	}

	private void updatePreferences(SharedPreferences prefs) {
		updateFreq = Integer
				.parseInt(prefs.getString("PREF_UPDATE_FREQ", "30")) * 60L * 1000L;
		minMag = Integer.parseInt(prefs.getString("PREF_MIN_MAG", "3"));
		boolean autoUpdate = prefs.getBoolean("PREF_AUTO_UPDATE", true);
		vibrateNotify = prefs.getBoolean("PREF_VIBRATE", true);
		boolean track = prefs.getBoolean("PREF_TRACK_LOCATION", false);
		if (!track)
			maxDist = 0;
		else
			maxDist = Integer.parseInt(prefs.getString("PREF_MAX_DIST", "200"));
		
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(this);
		if (maxDist > 0) {
			List<String> providers = locationManager.getProviders(true);
			int providersCount = providers.size();
			for (int i = 0; i < providersCount; i++) {
				String providerName = providers.get(i);
				if (locationManager.isProviderEnabled(providerName)) {
					currentLocation = LocationHelper.getBestLocation(
							currentLocation, 
							locationManager.getLastKnownLocation(providerName),
							LOCATION_UPDATE_FREQ);
				}
				locationManager.requestLocationUpdates(providerName,
						LOCATION_UPDATE_FREQ, 2500L, this);
			}
		}

		if (autoUpdate) {
			int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq;
			alarms.setRepeating(alarmType, timeToRefresh, updateFreq,
					alarmIntent);
		} else {
			alarms.cancel(alarmIntent);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private boolean aggiungi(Terremoto terremoto) {
		ContentResolver cr = getContentResolver();

		String w = TerremotoProvider.KEY_ID + " = " + terremoto.getId();
		if (cr.query(TerremotoProvider.CONTENT_URI, null, w, null, null)
				.getCount() <= 0) {
			ContentValues values = new ContentValues();

			values.put(TerremotoProvider.KEY_ID, terremoto.getId());
			values.put(TerremotoProvider.KEY_DATA, terremoto.getData()
					.getTime());
			values.put(TerremotoProvider.KEY_LAT, terremoto.getLatitudine());
			values.put(TerremotoProvider.KEY_LNG, terremoto.getLongitudine());
			values.put(TerremotoProvider.KEY_MAG, terremoto.getMagnitude());
			values.put(TerremotoProvider.KEY_WHERE, terremoto.getLuogo());
			values.put(TerremotoProvider.KEY_DEEP, terremoto.getProfondita());

			cr.insert(TerremotoProvider.CONTENT_URI, values);
			nuovoTerremoto(terremoto);
			return true;
		}

		return false;
	}

	private void nuovoTerremoto(Terremoto terremoto) {
		Intent intent = new Intent(NUOVO_TERREMOTO);
		intent.putExtra(TerremotoProvider.KEY_ID, terremoto.getId());
		intent.putExtra(TerremotoProvider.KEY_DATA, terremoto.getData()
				.getTime());
		intent.putExtra(TerremotoProvider.KEY_LAT, terremoto.getLatitudine());
		intent.putExtra(TerremotoProvider.KEY_LNG, terremoto.getLongitudine());
		intent.putExtra(TerremotoProvider.KEY_MAG, terremoto.getMagnitude());
		intent.putExtra(TerremotoProvider.KEY_WHERE, terremoto.getLuogo());
		intent.putExtra(TerremotoProvider.KEY_DEEP, terremoto.getProfondita());

		sendBroadcast(intent);
	}

	private void aggiornaTerremoti() {
		if (lastLookup == null
				|| lastLookup.getStatus().equals(AsyncTask.Status.FINISHED)) {
			lastLookup = new TerremotoLookupTask();
			lastLookup.execute((Void[]) null);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updatePreferences(sharedPreferences);
	}

	@Override
	public void onLocationChanged(Location location) {
		currentLocation = LocationHelper.getBestLocation(currentLocation, location, LOCATION_UPDATE_FREQ);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
