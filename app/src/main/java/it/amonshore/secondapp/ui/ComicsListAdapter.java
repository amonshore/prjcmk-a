package it.amonshore.secondapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import it.amonshore.secondapp.R;
import it.amonshore.secondapp.data.Comics;

/**
 * Created by Calgia on 07/05/2015.
 */
public class ComicsListAdapter extends BaseAdapter {

    private List<Comics> items;
    private Context context;

    public ComicsListAdapter(Context context, List<Comics> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //convertView = LayoutInflater.from(context).inflate(R.layout.list_comics_item, null);
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_2, null);
        }

        Comics comics = (Comics)getItem(position);
        //TextView txtComicsName = (TextView)convertView.findViewById(R.id.txt_comics_name);
        //txtComicsName.setText(comics.getName());

        ((TextView)convertView.findViewById(android.R.id.text1)).setText(comics.getName());
        ((TextView)convertView.findViewById(android.R.id.text2)).setText("notes");

        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }
}
