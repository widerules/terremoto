package net.morettoni.a;

import java.util.ArrayList;
import java.util.Date;

import net.morettoni.a.beans.Terremoto;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TerremotoActivity extends TabActivity {
	private ListView terremotiView;
	private ArrayList<Terremoto> terremotiList;
	private TerremotoItemAdapter terremotiItems;
	private TabHost tabHost;
	private TerremotoReceiver receiver;
    
    public class TerremotoReceiver extends BroadcastReceiver {
      @Override
      public void onReceive(Context context, Intent intent) {
    	  updateEvents();
      }
   	}

    @Override 
    public void onResume() {
      IntentFilter filter;
      filter = new IntentFilter(TerremotoService.LISTA_TERREMOTI_AGGIORNATA);
      receiver = new TerremotoReceiver();
      registerReceiver(receiver, filter);

      updateEvents();
      super.onResume();
    }

    @Override
    public void onPause() {
      unregisterReceiver(receiver);
      super.onPause();
    }	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		tabHost = getTabHost();

		tabHost.addTab(tabHost.newTabSpec("tab_lista").setIndicator(
				"Lista",
				getResources().getDrawable(
						android.R.drawable.ic_menu_sort_by_size)).setContent(
				R.id.terremotiList));

		TabSpec tabSpec = tabHost.newTabSpec("tab_mappa");
		Intent i = new Intent().setClass(this, TerremotoMapActivity.class);
		tabSpec.setIndicator("Mappa",
				getResources().getDrawable(android.R.drawable.ic_menu_mapmode))
				.setContent(i);
		tabHost.addTab(tabSpec);
		tabHost.setCurrentTab(0);

		terremotiView = (ListView) findViewById(R.id.terremotiList);
		terremotiList = new ArrayList<Terremoto>();

		terremotiItems = new TerremotoItemAdapter(this, R.layout.terremotoitem,
				terremotiList);
		terremotiView.setAdapter(terremotiItems);

		updateEvents();
		startService(new Intent(this, TerremotoService.class));		
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
		ContentResolver cr = getContentResolver();

	    Cursor c = cr.query(TerremotoProvider.CONTENT_URI, null, null, null, null);
			 
	      if (c.moveToFirst()) {
	        do {
	        	Terremoto terremoto = new Terremoto();
	        	terremoto.setId(c.getLong(TerremotoProvider.ID_COLUMN));
	        	terremoto.setData(new Date(c.getLong(TerremotoProvider.DATA_COLUMN)));
	        	terremoto.setLongitudine(c.getDouble(TerremotoProvider.LONGITUDE_COLUMN));
	        	terremoto.setLatitudine(c.getDouble(TerremotoProvider.LATITUDE_COLUMN));
	        	terremoto.setMagnitude(c.getDouble(TerremotoProvider.MAGNITUDE_COLUMN));
	        	terremoto.setLuogo(c.getString(TerremotoProvider.WHERE_COLUMN));
	        	terremoto.setProfondita(c.getDouble(TerremotoProvider.DEEP_COLUMN));
	        	
	        	terremotiList.add(terremoto);
	        } while(c.moveToNext());
	      }

		terremotiItems.notifyDataSetChanged();
	}
}