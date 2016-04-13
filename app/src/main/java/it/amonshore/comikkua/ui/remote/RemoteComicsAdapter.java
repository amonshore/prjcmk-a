package it.amonshore.comikkua.ui.remote;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;

/**
 * Created by narsenico on 04/03/16.
 */
public class RemoteComicsAdapter extends RecyclerView.Adapter<RemoteComicsAdapter.ViewHolderRemoteComic> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<Comics> mComics;
    private OnItemClickListener mOnItemClickListener;
    private SparseArray<Boolean> mSelectedItems;

    public RemoteComicsAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mComics = new ArrayList<>();
    }

    public void setComics(ArrayList<Comics> comics) {
        mComics = comics;
        mSelectedItems = new SparseArray<>();
        notifyItemRangeChanged(0, mComics.size());
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void toggleSelection(int position) {
        if (mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        } else {
            mSelectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    @Override
    public ViewHolderRemoteComic onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = mLayoutInflater.inflate(R.layout.list_remote_comics_item, parent, false);
        return new ViewHolderRemoteComic(view, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolderRemoteComic holder, int position) {
        final Comics currentComics = mComics.get(position);
        holder.mTxtName.setText(currentComics.getName());
        holder.mTxtPublisher.setText(currentComics.getPublisher());
        holder.mChkToimport.setChecked(mSelectedItems.get(position, false));
    }

    @Override
    public int getItemCount() {
        return mComics.size();
    }

    /**
     *
     */
    static class ViewHolderRemoteComic extends RecyclerView.ViewHolder {
        private TextView mTxtName;
        private TextView mTxtPublisher;
        private CheckBox mChkToimport;

        public ViewHolderRemoteComic(final View itemView, final OnItemClickListener onItemClickListener) {
            super(itemView);
            mTxtName = (TextView) itemView.findViewById(R.id.txt_list_comics_name);
            mTxtPublisher = (TextView) itemView.findViewById(R.id.txt_list_comics_publisher);
            mChkToimport = (CheckBox) itemView.findViewById(R.id.chk_list_comics_toimport);

            if (onItemClickListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClickListener.onItemClick(ViewHolderRemoteComic.this, v, getAdapterPosition());
                    }
                });
                mChkToimport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClickListener.onItemClick(ViewHolderRemoteComic.this, v, getAdapterPosition());
                    }
                });
            }
        }
    }

    /**
     *
     */
    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position);
    }

}
