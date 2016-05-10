package it.amonshore.comikkua;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import it.amonshore.comikkua.data.DataManager;

/**
 * Created by narsenico on 09/05/16.
 *
 * A0066
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Utils.d(this.getClass(), "BootReceiver " + intent.getAction());

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Utils.d(this.getClass(), "Boot completed: reloading reminders");
            DataManager
                    .getDataManager()
                    // devo per forza richiamare startWriteHandler altrimenti updateData non va a buon fine
                    .startWriteHandler()
                    .updateData(DataManager.ACTION_REMINDER_UPDATE,
                            DataManager.NO_COMICS, DataManager.NO_RELEASE)
                    // e quindi lo chiudo altrimenti rimarrà sempre aperto
                    .stopWriteHandler();
        }
    }
}
