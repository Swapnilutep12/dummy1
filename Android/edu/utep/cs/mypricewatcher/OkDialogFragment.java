package edu.utep.cs.mypricewatcher;

import android.app.Dialog;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * A dialog fragment showing an alert dialog that has a single OK button.
 *
 * Usage:
 * <code></code>
 *   OkDialogFragment dialog = new OkDialogFragment("Welcome ...", () -> doSomething());
 *   dialog.show(getSupportFragmentManager(), "Welcome");
 * </code>
 */
public class OkDialogFragment extends DialogFragment {

    /** Save instance state that can be stored in a bundle, e.g., Runnable. */
    public static class DialogViewModel extends ViewModel {
        public String message;
        public Runnable action;
    }

    private DialogViewModel viewModel;
    private String message;
    private Runnable action;

    public OkDialogFragment() {
        setCancelable(false); // no cancelling by touching outside the dialog.
    }

    public static OkDialogFragment instance(String message) {
        return OkDialogFragment.instance(message, null);
    }

    public static OkDialogFragment instance(String message, Runnable action) {
        OkDialogFragment dialog = new OkDialogFragment();
        dialog.message = message;
        dialog.action = action;
        return dialog;
    }

    /** Called to indicate that the activity's onCreate() has completed. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(DialogViewModel.class);
        if (savedInstanceState != null) {
            message = viewModel.message;
            action = viewModel.action;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton("OK",(dialog, id) -> {
                        if (action != null) {
                            action.run();
                        }
                    });
            return builder.create();
        }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.message = message;
        viewModel.action = action;
    }
}

