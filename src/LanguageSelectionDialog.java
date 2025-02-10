package com.alsolutions.mapia;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.alsolutions.mapia.model.LanguageList;

public class LanguageSelectionDialog  extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] languages = LanguageList.getHumanReadable();
        builder.setTitle(R.string.lanaguage_selection)
                .setItems(languages, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((MapsMainActivity) getActivity())
                                .language_selected(which);
                    }
                });
        return builder.create();
    }
}