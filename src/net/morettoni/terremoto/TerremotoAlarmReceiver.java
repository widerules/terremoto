package net.morettoni.terremoto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TerremotoAlarmReceiver extends BroadcastReceiver {
    public static final String TERREMOTI_ALARM = "net.morettono.terremoti.TERREMOTI_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, TerremotoService.class);
        context.startService(startIntent);
    }
}
