package net.morettoni.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import net.morettoni.a.beans.Terremoto;
import net.morettoni.a.R;

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
    private long oldestEvent = 2592000L;
    private Location currentLocation;

    @Override
    public void onCreate() {
        alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intentToFire = new Intent(TerremotoAlarmReceiver.TERREMOTI_ALARM);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

        terremotoNotification = new Notification(R.drawable.icon,
                "Nuovo terremoto!", System.currentTimeMillis());
    }

    @Override
    public void onDestroy() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        int providersCount = providers.size();

        locationManager.removeUpdates(this);
        for (int i = 0; i < providersCount; i++) {
            locationManager.removeUpdates(this);
        }

        super.onDestroy();
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
                            new InputStreamReader(
                                    httpConnection.getInputStream()));
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

                        try {
                            terremoto = new Terremoto();
                            terremoto.mLatitudine = Double
                                    .parseDouble(event[0]);
                            terremoto.mLongitudine = Double
                                    .parseDouble(event[1]);
                            terremoto.mProfondita = Double
                                    .parseDouble(event[2]);
                            try {
                                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                                terremoto.mData = df.parse(event[3]);
                                df.setTimeZone(TimeZone.getDefault());
                            } catch (ParseException e) {
                            }
                            terremoto.mMagnitude = Double.parseDouble(event[4]);
                            terremoto.mLuogo = event[5].replaceAll("_", " ");
                            terremoto.mId = Long.parseLong(event[6]);

                            if (aggiungi(terremoto))
                                publishProgress(terremoto);
                            else
                                break;
                        } catch (NumberFormatException e) {
                            break;
                        }
                    }
                    in.close();
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }

            removeOldEvents();

            return null;
        }

        @Override
        protected void onProgressUpdate(Terremoto... values) {
            Terremoto terremoto = values[0];
            Date data = terremoto.mData;
            double mag = terremoto.mMagnitude;

            if (data.getTime() > lastNotifiedEventDate && mag >= minMag) {
                float distance = -1.0F;
                if (currentLocation != null && maxDist > 0) {
                    Location event = new Location("dummy");
                    event.setLatitude(terremoto.mLatitudine);
                    event.setLongitude(terremoto.mLongitudine);

                    distance = event.distanceTo(currentLocation) / 1000.0F;

                    if (maxDist > 0 && distance > maxDist) return;
                }
                lastNotifiedEventDate = data.getTime();
                String luogo = terremoto.mLuogo;

                String svcName = Context.NOTIFICATION_SERVICE;
                NotificationManager notificationManager;
                notificationManager = (NotificationManager) getSystemService(svcName);

                Context context = getApplicationContext();
                SimpleDateFormat df = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                String dateText = df.format(data);
                String titleText;
                if (distance < 0)
                    titleText = String.format("%s: %.1f", luogo, mag);
                else
                    titleText = String.format("%s: %.1f (dist. %.0fkm)", luogo,
                            mag, distance);
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
    public void onStart(Intent intent, int startId) {
        startService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return Service.START_NOT_STICKY;
    }

    private void startService() {
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);

        updatePreferences(prefs);

        aggiornaTerremoti();
    }

    private void updatePreferences(SharedPreferences prefs) {
        updateFreq = Integer
                .parseInt(prefs.getString("PREF_UPDATE_FREQ", "30")) * 60L * 1000L;
        oldestEvent = Long.parseLong(prefs.getString("PREF_OLDEST_EVENT",
                "2592000"));
        minMag = Integer.parseInt(prefs.getString("PREF_MIN_MAG", "3"));
        boolean autoUpdate = prefs.getBoolean("PREF_AUTO_UPDATE", true);
        vibrateNotify = prefs.getBoolean("PREF_VIBRATE", true);
        boolean track = prefs.getBoolean("PREF_TRACK_LOCATION", false);
        if (!track)
            maxDist = 0;
        else
            maxDist = Integer.parseInt(prefs.getString("PREF_MAX_DIST", "200"));

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        int providersCount = providers.size();

        locationManager.removeUpdates(this);
        for (int i = 0; i < providersCount; i++) {
            if (maxDist > 0) {
                String providerName = providers.get(i);
                if (locationManager.isProviderEnabled(providerName)) {
                    currentLocation = LocationHelper.getBestLocation(
                            currentLocation,
                            locationManager.getLastKnownLocation(providerName),
                            LOCATION_UPDATE_FREQ);
                    locationManager.requestLocationUpdates(providerName,
                            LOCATION_UPDATE_FREQ, 2500L, this);
                }
            } else {
                locationManager.removeUpdates(this);
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
        removeOldEvents();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void removeOldEvents() {
        if (oldestEvent != 0L) {
            ContentResolver cr = getContentResolver();

            long now = System.currentTimeMillis();
            String w = String.format("%s < %d", TerremotoProvider.KEY_DATA, now
                    - (oldestEvent * 1000L));
            try {
                cr.delete(TerremotoProvider.CONTENT_URI, w, null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private boolean aggiungi(Terremoto terremoto) {
        ContentResolver cr = getContentResolver();

        String w = TerremotoProvider.KEY_ID + " = " + terremoto.mId;
        if (cr.query(TerremotoProvider.CONTENT_URI, null, w, null, null)
                .getCount() <= 0) {
            ContentValues values = new ContentValues();

            values.put(TerremotoProvider.KEY_ID, terremoto.mId);
            values.put(TerremotoProvider.KEY_DATA, terremoto.mData.getTime());
            values.put(TerremotoProvider.KEY_LAT, terremoto.mLatitudine);
            values.put(TerremotoProvider.KEY_LNG, terremoto.mLongitudine);
            values.put(TerremotoProvider.KEY_MAG, terremoto.mMagnitude);
            values.put(TerremotoProvider.KEY_WHERE, terremoto.mLuogo);
            values.put(TerremotoProvider.KEY_DEEP, terremoto.mProfondita);

            cr.insert(TerremotoProvider.CONTENT_URI, values);
            nuovoTerremoto(terremoto);
            return true;
        }

        return false;
    }

    private void nuovoTerremoto(Terremoto terremoto) {
        Intent intent = new Intent(NUOVO_TERREMOTO);
        intent.putExtra(TerremotoProvider.KEY_ID, terremoto.mId);
        intent.putExtra(TerremotoProvider.KEY_DATA, terremoto.mData.getTime());
        intent.putExtra(TerremotoProvider.KEY_LAT, terremoto.mLatitudine);
        intent.putExtra(TerremotoProvider.KEY_LNG, terremoto.mLongitudine);
        intent.putExtra(TerremotoProvider.KEY_MAG, terremoto.mMagnitude);
        intent.putExtra(TerremotoProvider.KEY_WHERE, terremoto.mLuogo);
        intent.putExtra(TerremotoProvider.KEY_DEEP, terremoto.mProfondita);

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
        currentLocation = LocationHelper.getBestLocation(currentLocation,
                location, LOCATION_UPDATE_FREQ);
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
