package edu.utep.cs.mypricewatcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pricewatcher.model.Item;
import pricewatcher.model.ItemManager;

import static android.webkit.URLUtil.isValidUrl;

public class AddDialogFragment extends AbstractDialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int[] buttonColors = {
                R.color.colorAddButton,
                android.R.color.holo_green_light, // enabled
                R.color.colorDisabled}; // disabled
        View rootView = onCreateView(inflater, container, savedInstanceState,
                R.layout.fragment_add_dialog, R.id.addButton, buttonColors);
        return rootView;
    }

    /** Called when the add button is clicked. */
    protected void doneClicked(View view) {
        String name = nameEdit.getText().toString();
        if (!checkEmptyValue(name, "Enter a name!")) {
            return;
        }
        String url = urlEdit.getText().toString();
        if (!checkEmptyValue(url, "Enter a URL!")) {
            return;
        }
        if (!isValidUrl(url)) {
            OkDialogFragment dialog = OkDialogFragment.instance("Enter a valid URL!");
            dialog.show(getActivity().getSupportFragmentManager(), "Add");
            return;
        }

        ItemManager itemManager = viewModel.itemManager();
        if (itemManager.contains(url)) {
            toast("URL already exists!");
        } else {
            float price = priceEdit.getTag() == null ? 0f : (float) priceEdit.getTag();
            Item item = itemManager.add(name, groupEdit.getText().toString(), url, price);
            if (item == null) {
                toast("Failed to add!");
            } else {
                toast("Added.");
                dismiss();
                if (listener != null) {
                    listener.onCompletion(item);
                }
            }
        }
    }

}
