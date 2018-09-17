package danigol.com.criminalintent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;

/**
 * Created by daniellegolinsky on 9/17/18.
 */

public class TimePickerFragment extends DialogFragment {

    public static final String EXTRA_TIME = "danigol.com.criminalintent.time";
    public static final String ARG_TIME = "time";

    private TimePicker mTimePicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        View v = LayoutInflater.from(getActivity())
                               .inflate(R.layout.dialog_time, null);

        mTimePicker = v.findViewById(R.id.time_picker_fragment);
        mTimePicker.setIs24HourView(false);

        // TODO Init

        // TODO Actually display something
        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.date_picker_title)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
