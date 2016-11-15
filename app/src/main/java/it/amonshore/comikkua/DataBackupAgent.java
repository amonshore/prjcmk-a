package it.amonshore.comikkua;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import it.amonshore.comikkua.data.DataManager;
import it.amonshore.comikkua.data.FileHelper;

/**
 * Created by narsenico on 15/11/16.
 *
 * A0072
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

        // visto che andrò a salvare il contenuto del database sottoforma di file json, lo creo e calcolo l'hash
        // poi lo confronto con quello salvato nell'oldState
        final File bckFile = File.createTempFile("comikku_bck_", ".json");
        try {
            // salvo tutti i dati in un file
            if (DataManager.getDataManager().backupToFile(bckFile)) {
                // leggo tutti i byte del file
                final byte[] bs = FileHelper.readAllBytes(bckFile);
                // calcolo il MD5 checksum del file, in modo da confrontarlo con quello salvato in oldState
                final MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(bs);
                final String digest = Utils.toHex(md.digest());
                // recupero le informazioni dall'ultimo backup
                final String oldDigest = tryReadOldDigest(oldState);
                Utils.d(this.getClass(), "onBackup compare " + digest + " = " + oldDigest);
                // se è diverso salvo
                if (!digest.equals(oldDigest)) {
                    Utils.d(this.getClass(), "onBackup send");
                    // invio l'intero file
                    data.writeEntityHeader(DATA_BCK_KEY, bs.length);
                    data.writeEntityData(bs, bs.length);
                } else {
                    Utils.d(this.getClass(), "onBackup nothing to send");
                }
                // in ogni caso salvo il MD5 checksum nel nuovo stato (verrà passato come oldState la prossima volta)
                final FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
                final DataOutputStream out = new DataOutputStream(outstream);
                out.writeUTF(digest);
                out.close();
            }
        } catch (NoSuchAlgorithmException nsaex) {
            Utils.e(this.getClass(), "onBackup", nsaex);
        } finally {
            if (bckFile.exists()) {
                bckFile.delete();
            }
        }
    }

    @Override
    public void onRestore(BackupDataInput data,
                          int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        // TODO: A0072 restore backup
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
