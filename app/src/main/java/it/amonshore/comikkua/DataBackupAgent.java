package it.amonshore.comikkua;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import org.json.JSONException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.JsonHelper;

/**
 * Created by narsenico on 15/11/16.
 *
 * A0072
 * TODO: backup e restore anche delle preferenze
 * TODO: backup e restore delle immagini
 */

public class DataBackupAgent extends BackupAgent {

    private static final String DATA_BCK_KEY = "data_bck_key";

    @Override
    public void onBackup(ParcelFileDescriptor oldState,
                         BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        Utils.d(this.getClass(), "onBackup start");

        // per prima cosa devo capire se i dati attuali sono diversi da quelli presenti nel backup
        // per farlo devo usare le informazioni salvate in oldState
        // (che non contiene l'ultimo backup, ma solo una rappresentazione dello stesso)
        // se i dati sono cambiati posso inviarli attraveso il BackupManager
        final JsonHelper jsonHelper = new JsonHelper();
        try {
            // estraggo tutti i dati in formato json
            final String json = jsonHelper.comics2string(DataManager.getDataManager().getRawComics(), "", true);
            final byte[] buff = json.getBytes("UTF-8");
            // calcolo il MD5 checksum del json, in modo da confrontarlo con quello salvato in oldState
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buff);
            final String digest = Utils.toHex(md.digest());
            // recupero le informazioni dall'ultimo backup
            final String oldDigest = tryReadOldDigest(oldState);
            Utils.d(this.getClass(), "onBackup compare " + digest + " = " + oldDigest);
            // se è diverso salvo
            if (!digest.equals(oldDigest)) {
                Utils.d(this.getClass(), "onBackup send");
                // invio l'intero json
                data.writeEntityHeader(DATA_BCK_KEY, buff.length);
                data.writeEntityData(buff, buff.length);
            } else {
                Utils.d(this.getClass(), "onBackup nothing to send");
            }
            // in ogni caso salvo il MD5 checksum nel nuovo stato (verrà passato come oldState la prossima volta)
            final DataOutputStream out = new DataOutputStream(new FileOutputStream(newState.getFileDescriptor()));
            out.writeUTF(digest);
            out.close();
        } catch (NoSuchAlgorithmException nsaex) {
            Utils.e(this.getClass(), "onBackup", nsaex);
        } catch (JSONException jsonex) {
            Utils.e(this.getClass(), "onBackup", jsonex);
        }
    }

    @Override
    public void onRestore(BackupDataInput data,
                          int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        Utils.d(this.getClass(), "onRestore start, ver " + appVersionCode);

        // estraggo il json inviato in fase di backup
        // e ripristino i dati (quelli già presenti verranno eliminati)
        while (data.readNextHeader()) {
            final String key = data.getKey();
            final int dataSize = data.getDataSize();
            if (DATA_BCK_KEY.equals(key)) {
                final byte[] buff = new byte[dataSize];
                data.readEntityData(buff, 0, dataSize);
                try {
                    final JsonHelper jsonHelper = new JsonHelper();
                    final String json = new String(buff, 0, buff.length, "UTF-8");
                    Utils.d(this.getClass(), "onRestore restore");
                    DataManager.getDataManager().restoreFromRaw(jsonHelper.string2comics(json));
                    // calcolo il MD5 checksum del json, in modo da salvarlo nello stato
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(buff);
                    final String digest = Utils.toHex(md.digest());
                    // salvo il MD5 checksum nel nuovo stato (verrà passato come oldState al prossimo onBakcup)
                    final FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
                    final DataOutputStream out = new DataOutputStream(outstream);
                    out.writeUTF(digest);
                    out.close();
                } catch (NoSuchAlgorithmException nsaex) {
                    Utils.e(this.getClass(), "onRestore", nsaex);
                } catch (JSONException jsonex) {
                    Utils.e(this.getClass(), "onRestore", jsonex);
                }
            } else {
                data.skipEntityData();
            }
        }
    }

    private String tryReadOldDigest(ParcelFileDescriptor oldState) throws IOException {
        final FileInputStream fisOldState = new FileInputStream(oldState.getFileDescriptor());
        final DataInputStream disOldState = new DataInputStream(fisOldState);
        try {
            return disOldState.readUTF();
        } catch (EOFException eofex) {
            return "";
        } finally {
            disOldState.close();
        }
    }
}
