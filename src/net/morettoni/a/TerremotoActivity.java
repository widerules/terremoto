package net.morettoni.a;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import net.morettoni.a.beans.Terremoto;
import net.morettoni.a.beans.TerremotoItemAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;

public class TerremotoActivity extends Activity {
	private ListView terremotiView;
	private ArrayList<Terremoto> terremotiList;
	private TerremotoItemAdapter terremotiItems;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		terremotiView = (ListView) findViewById(R.id.terremotiList);
		terremotiList = new ArrayList<Terremoto>();

		terremotiItems = new TerremotoItemAdapter(this, R.layout.terremotoitem,
				terremotiList);
		terremotiView.setAdapter(terremotiItems);

		updateEvents();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			updateEvents();
			return true;
		}
		return false;
	}

	private void updateEvents() {
		terremotiList.clear();
		
		String feed = getString(R.string.feed);
		try {
			URL url = new URL(feed);
			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
			    BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
			    String str;
			    String[] event;
			    Terremoto terremoto;
			    boolean firstLine = true;
			    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

			    while ((str = in.readLine()) != null) {
			    	if (firstLine) {
			    		firstLine =false;
			    		continue;
			    	}
			    	
			    	event = str.split(",");
			    	
			    	terremoto = new Terremoto();
			    	terremoto.setLatitudine(Double.parseDouble(event[0]));
			    	terremoto.setLongitudine(Double.parseDouble(event[1]));
			    	terremoto.setProfondita(Double.parseDouble(event[2]));
			    	try {
						terremoto.setData(df.parse(event[3]));
					} catch (ParseException e) {
					}
			    	terremoto.setMagnitude(Double.parseDouble(event[4]));
			    	terremoto.setLuogo(event[5].replaceAll("_", " "));
			    	
			    	terremotiList.add(terremoto);
			    }
			    in.close();
			}
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}

		terremotiItems.notifyDataSetChanged();
	}
}