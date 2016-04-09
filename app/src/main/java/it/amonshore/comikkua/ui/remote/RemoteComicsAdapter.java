package it.amonshore.comikkua.ui.remote;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public RemoteComicsAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mComics = new ArrayList<>();
    }

    public void setComics(ArrayList<Comics> comics) {
        mComics = comics;
        notifyItemRangeChanged(0, mComics.size());
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolderRemoteComic onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = mLayoutInflater.inflate(R.layout.list_remote_comics_item, parent, false);
        final ViewHolderRemoteComic viewHolderRemoteComic = new ViewHolderRemoteComic(view, mOnItemClickListener);
        return viewHolderRemoteComic;
    }

    @Override
    public void onBindViewHolder(ViewHolderRemoteComic holder, int position) {
        final Comics currentComics = mComics.get(position);
        holder.mTextView.setText(currentComics.getName());
    }

    @Override
    public int getItemCount() {
        return mComics.size();
    }

    static class ViewHolderRemoteComic extends RecyclerView.ViewHolder {
        private TextView mTextView;

        public ViewHolderRemoteComic(View itemView, final OnItemClickListener onItemClickListener) {
            super(itemView);
            itemView.setClickable(true);
            if (onItemClickListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemClickListener.onItemClick(ViewHolderRemoteComic.this, v, getAdapterPosition());
                    }
                });
            }
            mTextView = (TextView) itemView.findViewById(R.id.txt_list_comics_name);
        }
    }

    /**
     *
     */
    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position);
    }

}
