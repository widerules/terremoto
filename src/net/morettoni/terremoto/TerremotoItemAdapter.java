package net.morettoni.terremoto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.morettoni.terremoto.R;
import net.morettoni.terremoto.beans.Terremoto;
import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TerremotoItemAdapter extends ArrayAdapter<Terremoto> {
    private int resource;
    private Location currentLocation;

    public TerremotoItemAdapter(Context _context, int _resource,
            ArrayList<Terremoto> terremotiList) {
        super(_context, _resource, terremotiList);
        resource = _resource;
    }

    public void setCurrentLocation(Location location) {
        currentLocation = location;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout terremotoView;
        if (convertView == null) {
            terremotoView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                    inflater);
            vi.inflate(resource, terremotoView, true);
        } else {
            terremotoView = (LinearLayout) convertView;
        }

        TextView dateView = (TextView) terremotoView.findViewById(R.id.rowDate);
        TextView luogoView = (TextView) terremotoView
                .findViewById(R.id.rowPlace);
        TextView magView = (TextView) terremotoView.findViewById(R.id.rowMag);
        TextView deepView = (TextView) terremotoView.findViewById(R.id.rowDeep);
        TextView distView = (TextView) terremotoView
                .findViewById(R.id.rowDistance);

        Terremoto terremoto = getItem(position);
        Date dataEvento = terremoto.mData;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");

        dateView.setText(sdf.format(dataEvento));
        luogoView.setText(terremoto.mLuogo);
        magView.setText(String.format("%.1f", terremoto.mMagnitude));
        deepView.setText(String.format("%,.1fkm", terremoto.mProfondita));

        if (currentLocation == null) {
            distView.setText("");
        } else {
            Location event = new Location("dummy");
            event.setLatitude(terremoto.mLatitudine);
            event.setLongitude(terremoto.mLongitudine);

            distView.setText(String.format("dist. %,.0fkm",
                    event.distanceTo(currentLocation) / 1000.0F));
        }

        return terremotoView;
    }
}
