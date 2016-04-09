package it.amonshore.comikkua.data;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by narsenico on 04/03/16.
 */
public class RemoteRequestManager {

    private static RemoteRequestManager mRemoteRequestManager;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    private RemoteRequestManager(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized RemoteRequestManager getInstance(Context context) {
        if (mRemoteRequestManager == null) {
            mRemoteRequestManager = new RemoteRequestManager(context);
        }
        return mRemoteRequestManager;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
