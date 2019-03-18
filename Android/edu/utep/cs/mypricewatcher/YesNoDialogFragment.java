package edu.utep.cs.mypricewatcher;

import android.app.Dialog;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * A dialog fragment showing an alert dialog with one or two buttons (Yes and No).
 *
 * Usage:
 * <code></code>
 *   YesNoDialogFragment dialog = new YesNoDialogFragment("Welcome ...", () -> doSomething());
 *   dialog.show(getSupportFragmentManager(), "Quit?");
 * </code>
 */
public class YesNoDialogFragment extends DialogFragment {

    /** Save instance state that can be stored in a bundle, e.g., Runnable. */
    public static class DialogViewModel extends ViewModel {
        public String title;
        public String message;
        public Runnable action;
        public Runnable actionOpt;
    }

    private DialogViewModel viewModel;
    private String title;
    private String message;
    private Runnable action;
    private Runnable actionOpt;

    public YesNoDialogFragment() {
        setCancelable(false); // no cancelling by touching outside the dialog.
    }

    public static YesNoDialogFragment instance(String message, Runnable action) {
        return instance(message, action, null);
    }

    public static YesNoDialogFragment instance(String message, Runnable action, Runnable actionOpt) {
        return instance(null, message, action, actionOpt);
    }

    public static YesNoDialogFragment instance(String title, String message, Runnable action) {
        return instance(title, message, action, null);
    }

    public static YesNoDialogFragment instance(String title, String message, Runnable action, Runnable actionOpt) {
        YesNoDialogFragment dialog = new YesNoDialogFragment();
        dialog.title = title;
        dialog.message = message;
        dialog.action = action;
        dialog.actionOpt = actionOpt;
        return dialog;
    }

    /** Called to indicate that the activity's onCreate() has completed. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(DialogViewModel.class);
        if (savedInstanceState != null) {
            title = viewModel.title;
            message = viewModel.message;
            action = viewModel.action;
            actionOpt = viewModel.actionOpt;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> action.run())
                .setNegativeButton(android.R.string.no, (dialog, id) -> {
                    if (actionOpt != null) {
                        actionOpt.run();
                    }
                });
        if (title != null) {
            builder.setTitle(title);
        }
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.title = title;
        viewModel.message = message;
        viewModel.action = action;
        viewModel.actionOpt = actionOpt;
    }
}

