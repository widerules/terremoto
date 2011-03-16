package net.morettoni.terremoto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.morettoni.terremoto.R;
import net.morettoni.terremoto.beans.Terremoto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TabHost.TabSpec;

public class TerremotoActivity extends TabActivity implements
        OnSharedPreferenceChangeListener, LocationListener {
    private static final long LOCATION_UPDATE_FREQ = 15L * 60L * 1000L;
    private static final int DETTAGLI_DIALOG = 1;
    private static final int INFO_DIALOG = 2;
    private ListView terremotiView;
    private ArrayList<Terremoto> terremotiList;
    private TerremotoItemAdapter terremotiItems;
    private Terremoto selectedTerremoto;
    private TabHost tabHost;
    private TerremotoReceiver receiver;
    private int minMag = 3;
    private boolean trackLocation = false;
    private NotificationManager notificationManager;
    private Location currentLocation;

    public class TerremotoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            notificationManager.cancel(TerremotoService.NOTIFICATION_ID);
            updateEvents();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources();
        tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("tab_lista")
                .setIndicator("Lista", res.getDrawable(R.drawable.list_tab))
                .setContent(R.id.terremotiList));

        TabSpec tabSpec = tabHost.newTabSpec("tab_mappa");
        Intent i = new Intent().setClass(this, TerremotoMapActivity.class);
        tabSpec.setIndicator("Mappa", res.getDrawable(R.drawable.map_tab))
                .setContent(i);
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(0);

        terremotiView = (ListView) findViewById(R.id.terremotiList);
        terremotiList = new ArrayList<Terremoto>();

        terremotiView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Terremoto terremoto = terremotiList.get(position);
                selectedTerremoto = terremoto;
                showDialog(DETTAGLI_DIALOG);
                return true;
            }
        });

        terremotiView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("rawtypes")
            public void onItemClick(AdapterView _av, View _v, int _index,
                    long arg3) {
                Terremoto terremoto = terremotiList.get(_index);
                showTerremotoInMap(terremoto);
            }
        });

        terremotiItems = new TerremotoItemAdapter(this, R.layout.terremotoitem,
                terremotiList);
        terremotiView.setAdapter(terremotiItems);

        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
        minMag = Integer.parseInt(prefs.getString("PREF_MIN_MAG", "3"));

        String svcName = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager) getSystemService(svcName);

        trackLocation = prefs.getBoolean("PREF_TRACK_LOCATION", false);
        if (trackLocation) enableLocationTrack();
        startService(new Intent(this, TerremotoService.class));
        updateEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter;
        filter = new IntentFilter(TerremotoService.LISTA_TERREMOTI_AGGIORNATA);
        receiver = new TerremotoReceiver();
        registerReceiver(receiver, filter);

        notificationManager.cancel(TerremotoService.NOTIFICATION_ID);

        if (trackLocation) enableLocationTrack();
        updateEvents();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        disableLocationTrack();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        LayoutInflater li;
        View dettagliView;
        AlertDialog.Builder dettagliDialog;

        switch (id) {
        case DETTAGLI_DIALOG:
            li = LayoutInflater.from(this);
            dettagliView = li.inflate(R.layout.dettagli, null);
            dettagliDialog = new AlertDialog.Builder(this);
            dettagliDialog.setTitle("?");
            dettagliDialog.setView(dettagliView);
            return dettagliDialog.create();
        case INFO_DIALOG:
            li = LayoutInflater.from(this);
            dettagliView = li.inflate(R.layout.dettagli, null);
            dettagliDialog = new AlertDialog.Builder(this);
            dettagliDialog.setTitle("Terremoto!");
            dettagliDialog.setView(dettagliView);
            return dettagliDialog.create();
        }
        return null;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        AlertDialog dettagliDialog;
        TextView tv;

        switch (id) {
        case INFO_DIALOG:
            dettagliDialog = (AlertDialog) dialog;
            tv = (TextView) dettagliDialog.findViewById(R.id.dettagliTerremoto);
            tv.setText(getString(R.string.info_text));
            break;
        case DETTAGLI_DIALOG:
            if (selectedTerremoto != null) {
                double lat = selectedTerremoto.mLatitudine;
                double lon = selectedTerremoto.mLongitudine;
                double mag = selectedTerremoto.mMagnitude;
                double deep = selectedTerremoto.mProfondita;
                Date data = selectedTerremoto.mData;
                long evId = selectedTerremoto.mId;
                String luogo = selectedTerremoto.mLuogo;

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                StringBuilder dettagli = new StringBuilder();
                dettagli.append("Data evento: ");
                dettagli.append(sdf.format(data));
                dettagli.append(String.format("\nMagnitudine: %.1f\n", mag));
                dettagli.append(String.format("Profondità: %,.1fkm\n", deep));
                dettagli.append(String.format(
                        "Posizione: %.3f (lat) %.3f (lon)\n", lat, lon));

                if (currentLocation != null) {
                    Location event = new Location(currentLocation);
                    event.setLatitude(lat);
                    event.setLongitude(lon);

                    dettagli.append(String.format("Distanza: %,.0fkm\n",
                            (event.distanceTo(currentLocation) / 1000.0F)));
                }
                dettagli.append("\nhttp://cnt.rm.ingv.it/data_id/");
                dettagli.append(evId);
                dettagli.append("/event.php");

                dettagliDialog = (AlertDialog) dialog;
                dettagliDialog.setTitle(luogo);
                tv = (TextView) dettagliDialog
                        .findViewById(R.id.dettagliTerremoto);
                tv.setText(dettagli.toString());
            }
            break;
        }
    }

    private void showTerremotoInMap(Terremoto terremoto) {
        tabHost.setCurrentTab(1);

        Intent intent = new Intent(TerremotoMapActivity.CENTER_TERREMOTO);
        intent.putExtra(TerremotoProvider.KEY_LAT, terremoto.mLatitudine);
        intent.putExtra(TerremotoProvider.KEY_LNG, terremoto.mLongitudine);
        intent.putExtra(TerremotoProvider.KEY_WHERE, terremoto.mLuogo);
        intent.putExtra(TerremotoProvider.KEY_MAG, terremoto.mMagnitude);
        sendBroadcast(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Context context = getBaseContext();
        switch (item.getItemId()) {
        case R.id.refresh:
            Intent startIntent = new Intent(context, TerremotoService.class);
            context.startService(startIntent);
            return true;
        case R.id.info:
            showDialog(INFO_DIALOG);
            return true;
        case R.id.preference:
            Intent settingsActivity = new Intent(context,
                    TerremotoPreference.class);
            startActivity(settingsActivity);
            return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals("PREF_MIN_MAG")) {
            int newMag = Integer.parseInt(sharedPreferences.getString(
                    "PREF_MIN_MAG", "0"));

            if (newMag != minMag) {
                minMag = newMag;
                updateEvents();
            }
        }

        if (key.equals("PREF_TRACK_LOCATION")) {
            boolean track = sharedPreferences.getBoolean("PREF_TRACK_LOCATION",
                    false);
            if (!track && trackLocation) {
                disableLocationTrack();
                currentLocation = null;
                terremotiItems.setCurrentLocation(currentLocation);
                terremotiItems.notifyDataSetChanged();
            }
            if (track && !trackLocation) {
                enableLocationTrack();
            }

            trackLocation = track;
        }
    }

    private void disableLocationTrack() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        int providersCount = providers.size();

        for (int i = 0; i < providersCount; i++) {
            String providerName = providers.get(i);
            if (locationManager.isProviderEnabled(providerName)) {
                locationManager.removeUpdates(this);
            }
        }
    }

    private void enableLocationTrack() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        long updateMinTime, updateMinDistance;
        int providersCount = providers.size();

        for (int i = 0; i < providersCount; i++) {
            String providerName = providers.get(i);
            if (locationManager.isProviderEnabled(providerName)) {
                onLocationChanged(locationManager
                        .getLastKnownLocation(providerName));
                if (LocationManager.GPS_PROVIDER.equals(providerName)) {
                    updateMinTime = 0L;
                    updateMinDistance = 0L;
                } else {
                    updateMinTime = LOCATION_UPDATE_FREQ;
                    updateMinDistance = 2500L;
                }
                locationManager.requestLocationUpdates(providerName,
                        updateMinTime, updateMinDistance, this);
            }
        }
    }

    private void updateEvents() {
        terremotiList.clear();
        ContentResolver cr = getContentResolver();

        Cursor c = cr.query(TerremotoProvider.CONTENT_URI, null, null, null,
                null);

        if (c.moveToFirst()) {
            do {
                if (c.getDouble(TerremotoProvider.MAGNITUDE_COLUMN) >= minMag) {
                    Terremoto terremoto = new Terremoto();
                    terremoto.mId = c.getLong(TerremotoProvider.ID_COLUMN);
                    terremoto.mData = new Date(
                            c.getLong(TerremotoProvider.DATA_COLUMN));
                    terremoto.mLongitudine = c
                            .getDouble(TerremotoProvider.LONGITUDE_COLUMN);
                    terremoto.mLatitudine = c
                            .getDouble(TerremotoProvider.LATITUDE_COLUMN);
                    terremoto.mMagnitude = c
                            .getDouble(TerremotoProvider.MAGNITUDE_COLUMN);
                    terremoto.mLuogo = c
                            .getString(TerremotoProvider.WHERE_COLUMN);
                    terremoto.mProfondita = c
                            .getDouble(TerremotoProvider.DEEP_COLUMN);

                    terremotiList.add(terremoto);
                }
            } while (c.moveToNext());
        }

        terremotiItems.notifyDataSetChanged();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = LocationHelper.getBestLocation(currentLocation,
                location, LOCATION_UPDATE_FREQ);
        terremotiItems.setCurrentLocation(currentLocation);
        terremotiItems.notifyDataSetChanged();
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