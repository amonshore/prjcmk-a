package it.amonshore.comikkua.ui.remote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utils;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.JsonHelper;
import it.amonshore.comikkua.data.RemoteRequestManager;

public class RemoteComicsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_comics);
        //
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list_remote_comics);
        final RemoteComicsAdapter adapter = new RemoteComicsAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnItemClickListener(new RemoteComicsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position) {
                Toast.makeText(view.getContext(), "Item @ position " + position, Toast.LENGTH_SHORT).show();
                adapter.toggleSelection(position);
            }
        });
        // recupero l'elenco dei fumetti remoti
        final Response.Listener<JSONArray> respListener = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Utils.d("A0061", response.toString());
                try {
                    final JsonHelper helper = new JsonHelper();
                    final ArrayList<Comics> remoteComics = new ArrayList<>();
                    for (int ii = 0; ii < response.length(); ii++) {
                        // TODO: implemenare helper.json2remoteComics(...) che tenga conto dei campi cid, categories[], etc
                        remoteComics.add(helper.json2comics(response.getJSONObject(ii)));
                    }
                    adapter.setComics(remoteComics);
                } catch (JSONException ex) {
                    Utils.e("A0061", "onResponse", ex);
                }
            }
        };
        final Response.ErrorListener errListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.e("A0061", "onErrorResponse", error);
                // TODO: fake da togliere
                final Comics comics = new Comics(-999);
                comics.setName("fake");
                comics.setPublisher("publisher");
                final ArrayList<Comics> remoteComics = new ArrayList<>();
                remoteComics.add(comics);
                adapter.setComics(remoteComics);
            }
        };
        // TODO: leggere parametro query di ricerca e usarla nella query
        // estendo la rihiesta per aggiungere l'header
        final JsonArrayRequest reqComics = new JsonArrayRequest
                (Request.Method.GET, "http://192.168.0.3:3000/v1/comics", (String) null, respListener, errListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("X-prjcmk-country", Locale.getDefault().getISO3Country());
                return params;
            }
        };
        RemoteRequestManager.getInstance(this).addToRequestQueue(reqComics);
    }

}
