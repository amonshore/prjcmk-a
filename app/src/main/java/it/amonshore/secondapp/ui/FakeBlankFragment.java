package it.amonshore.secondapp.ui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import it.amonshore.secondapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FakeBlankFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FakeBlankFragment extends Fragment implements OnChangePageListener {

    private static final String ARG_MESSAGE = "message";

    private String mMessage;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param message Fragment message
     * @return A new instance of fragment FakeBlankFragment.
     */
    public static FakeBlankFragment newInstance(String message) {
        FakeBlankFragment fragment = new FakeBlankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    public FakeBlankFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMessage = getArguments().getString(ARG_MESSAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fake_blank, container, false);
        ((TextView)rootView.findViewById(R.id.txt_message)).setText(mMessage);
        return rootView;
    }

    @Override
    public void finishActionMode() {
        //
    }

}
