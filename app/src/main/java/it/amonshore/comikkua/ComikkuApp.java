package it.amonshore.comikkua;

import android.app.Application;

import it.amonshore.comikkua.data.DataManager;

/**
 * Created by Narsenico on 25/06/2015.
 */
public class ComikkuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(this);
        //inizializzo il data manager e leggo i dati
        //TODO ricavare il nome dell'utente dalle preferenze, se non c'è usare default
        DataManager
                .init(this, "default")
                .readComics()
                .initReminderEngine() //A0033
//                .updateReminder() //A0033
                .removeDirtyImages(false); //A0055
    }
}
