package net.morettoni.terremoto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TerremotoAlarmReceiver extends BroadcastReceiver {
    public static final String TERREMOTI_ALARM = "net.morettoni.terremoti.TERREMOTI_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Terremoto", "TerremotoAlarmReceiver.onReceive");
        Intent startIntent = new Intent(context, TerremotoService.class);
        context.startService(startIntent);
    }
}
