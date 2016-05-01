package it.amonshore.comikkua;

/**
 * Created by Narsenico on 14/10/2015.
 */
public abstract class RequestCodes {

    public final static int EDIT_COMICS_REQUEST = 1001;
    public final static int EDIT_RELEASE_REQUEST = 2001;
    public final static int LOAD_IMAGE = 3001;
    public final static int CROP_IMAGE = 3010;
    //i codici di richiesta per i permessi possono essere solo da 0 a 255
    public final static int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 101;

    //TODO completare con gli altri codici
}
