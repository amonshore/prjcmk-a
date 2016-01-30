package it.amonshore.comikkua.reminder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import it.amonshore.comikkua.data.DataManager;

/**
 * Created by narsenico on 26/01/16.
 */
public class ReleaseReminderBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            //A0033 riattivo i reminder
            DataManager.getDataManager()
                    .updateData(DataManager.ACTION_REMINDER_BOOT,
                            DataManager.NO_COMICS, DataManager.NO_RELEASE);
        }
    }

    /**
     *
     * @param context contesto
     * @param enabled true abilita questo boot receiver
     */
    public static void setEnabled(Context context, boolean enabled) {
        ComponentName receiver = new ComponentName(context, ReleaseReminderBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
