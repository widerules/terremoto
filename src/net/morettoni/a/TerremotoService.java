package net.morettoni.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import net.morettoni.a.beans.Terremoto;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;

public class TerremotoService extends Service {

	public static final String NUOVO_TERREMOTO = "Nuovo_Terremoto";
	public static final String TERREMOTI_TIMER = "TerremotiTimer";
	public static String LISTA_TERREMOTI_AGGIORNATA = "net.morettoni.terremoto.nuovi_terremoti";
	private Timer updateTimer;

	private TerremotoLookupTask lastLookup = null;

	@Override
	public void onCreate() {
		updateTimer = new Timer(TERREMOTI_TIMER);
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
					int count = 0;
					SimpleDateFormat df = new SimpleDateFormat(
							"dd/MM/yyyy hh:mm:ss");

					while ((str = in.readLine()) != null) {
						if (count++ == 0) {
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

						if (count < 5 && aggiungi(terremoto))
							publishProgress(terremoto);
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
			Context context = getApplicationContext();
			String expandedTitle = String.format("%s: %.1f", values[0]
					.getLuogo(), values[0].getMagnitude());
			Toast.makeText(context, expandedTitle, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			sendBroadcast(new Intent(LISTA_TERREMOTI_AGGIORNATA));
			stopSelf();
		}
	}

	private TimerTask doRefresh = new TimerTask() {
		public void run() {
			aggiornaTerremoti();
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int updateFreq = 5;
		boolean autoUpdate = true;

		updateTimer.cancel();
		if (autoUpdate) {
			updateTimer = new Timer(TERREMOTI_TIMER);
			updateTimer.scheduleAtFixedRate(doRefresh, 0,updateFreq*60*1000);
		}

		aggiornaTerremoti();

		return Service.START_NOT_STICKY;
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private boolean aggiungi(Terremoto terremoto) {
		ContentResolver cr = getContentResolver();

		String w = TerremotoProvider.KEY_ID + " = " + terremoto.getId();

		if (cr.query(TerremotoProvider.CONTENT_URI, null, w, null, null).getCount() <= 0) {
			ContentValues values = new ContentValues();

			values.put(TerremotoProvider.KEY_ID, terremoto.getId());
			values.put(TerremotoProvider.KEY_DATA, terremoto.getData().getTime());
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
		intent.putExtra(TerremotoProvider.KEY_DATA, terremoto.getData().getTime());
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
}
