package it.amonshore.comikkua.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import it.amonshore.comikkua.R;

/**
 * Created by narsenico on 17/05/16.
 *
 * A0067
 */
public class SearchModeDialog {

    public final static int ITEM_AMAZON = 0;
    public final static int ITEM_WEB_SEARCH = 1;

    /**
     * Mostra un dialog per la scelta del metodo di ricerca (Amazon o web)
     *
     * @param context   contesto
     * @param onClickListener   gestione evento click
     */
    public static void showDialog(Context context, DialogInterface.OnClickListener onClickListener) {
        final Item[] items = {
                new Item("Amazon", R.drawable.ic_action_accept),
                new Item("Web search", R.drawable.ic_action_accept)
        };
        final ItemAdapter adapter = new ItemAdapter(context,
                android.R.layout.select_dialog_item,
                android.R.id.text1,
                items);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true)
                .setTitle("Search comics with")
                .setAdapter(adapter, onClickListener)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    private final static class Item {
        public final String Text;
        public final int Icon;
        public Item(String text, int icon) {
            this.Text = text;
            this.Icon = icon;
        }

        @Override
        public String toString() {
            return this.Text;
        }
    }

    private final static class ItemAdapter extends ArrayAdapter<Item> {

        public ItemAdapter(Context context, int resource,
                           int textViewResourceId, Item[] items) {
            super(context, resource, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setCompoundDrawablesWithIntrinsicBounds( getItem(position).Icon, 0, 0, 0 );

//            final int dp5 = (int) (5 * parent.getResources().getDisplayMetrics().density + 0.5f);
//            textView.setCompoundDrawablePadding(dp5);

            return view;
        }
    }
}
