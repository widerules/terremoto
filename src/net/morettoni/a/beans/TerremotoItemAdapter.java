package net.morettoni.a.beans;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.morettoni.a.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TerremotoItemAdapter extends ArrayAdapter<Terremoto> {
	int resource;

	public TerremotoItemAdapter(Context _context, int _resource, ArrayList<Terremoto> terremotiList) {
		super(_context, _resource, terremotiList);
		resource = _resource;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout terremotoView;
		if (convertView == null) {
			terremotoView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
			vi.inflate(resource, terremotoView, true);
		} else {
			terremotoView = (LinearLayout) convertView;
		}
		
		TextView dateView = (TextView) terremotoView.findViewById(R.id.rowDate);
		TextView luogoView = (TextView) terremotoView.findViewById(R.id.rowPlace);
		TextView magView = (TextView) terremotoView.findViewById(R.id.rowMag);
		TextView deepView = (TextView) terremotoView.findViewById(R.id.rowDeep);
		
		Terremoto terremoto = getItem(position);
		Date dataEvento = terremoto.getData();
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm dd/MM/yyyy");
		
		dateView.setText(sdf.format(dataEvento));
		luogoView.setText(terremoto.getLuogo());
		magView.setText(String.format("%.1f", terremoto.getMagnitude()));
		deepView.setText(String.format("%.1fkm", terremoto.getProfondita()));
		return terremotoView;
	}
}
