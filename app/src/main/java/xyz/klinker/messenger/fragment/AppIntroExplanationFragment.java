package xyz.klinker.messenger.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import xyz.klinker.messenger.R;

public class AppIntroExplanationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_intro_explanation, container, false);

        TextView description = (TextView) root.findViewById(R.id.description);
        description.setText(getString(R.string.message_anywhere_trial_description));

        return root;
    }
}
