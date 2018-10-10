package danigol.com.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static danigol.com.criminalintent.HelperMethods.hideSoftKeyboard;

/**
 * Created by daniellegolinsky on 2/12/18.
 */

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_TIME = "DialogTime";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_TIME = 1;

    private Crime mCrime;
    private Button mDateButton;
    private Button mTimeButton;
    private EditText mTitleField;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO: Something
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: Something
            }
        });

        mTitleField.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) { // I don't like this, but I think it's a bug
                hideSoftKeyboard(getActivity());
                return true;
            }
            return false;
        });

        // Date button should launch dialog fragment with date picker
        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setEnabled(true);
        mDateButton.setOnClickListener(v1 -> {
            FragmentManager manager = getFragmentManager();
            DatePickerFragment dateDialog = DatePickerFragment.newInstance(mCrime.getDate());
            dateDialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
            dateDialog.show(manager, DIALOG_DATE);
        });

        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        updateTime();
        mTimeButton.setEnabled(true);
        mTimeButton.setOnClickListener(v2 -> promptUserForTime());

        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> mCrime.setSolved(isChecked));

        mReportButton = v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(v2 -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
            i = Intent.createChooser(i, getString(R.string.send_report));
            startActivity(i);
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_crime_crime_fragment:
                Crime crime = new Crime();
                CrimeLab.get(getActivity()).addCrime(crime);
                Intent intent = CrimePagerActivity.newIntent(getActivity(), crime.getId());
                startActivity(intent);
                return true;
            case R.id.delete_crime_crime_fragment:
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        String extra = "";
        boolean promptForTime = false;
        switch (requestCode) {
            case REQUEST_DATE:
                extra = DatePickerFragment.EXTRA_DATE;
                promptForTime = true;
                break;
            case REQUEST_TIME:
                extra = TimePickerFragment.EXTRA_TIME;
                break;
        }

        Date date = (Date) data.getSerializableExtra(extra);
        mCrime.setDate(date);

        if (promptForTime) {
            // Prompt user for the time now
            promptUserForTime();
            // Update our date with the time
            mCrime.setDate(date);
        }

        // Update the GUI
        updateDateAndTime();
    }

    private void promptUserForTime() {
        FragmentManager manager = getFragmentManager();
        TimePickerFragment timePicker = TimePickerFragment.newInstance(mCrime.getDate());
        timePicker.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
        timePicker.show(manager, DIALOG_TIME);
    }

    private void updateDateAndTime() {
        updateDate();
        updateTime();
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDateString());
    }

    private void updateTime() {
        Date crimeTime = mCrime.getDate();
        Calendar timeTranslator = Calendar.getInstance();
        timeTranslator.setTime(crimeTime);
        int hour = timeTranslator.get(Calendar.HOUR_OF_DAY);
        int minute = timeTranslator.get(Calendar.MINUTE);
        String timeOfDay = "am";
        if (hour > 12) {
            timeOfDay = "pm";
            hour -= 12;
        }

        String hourString = prettyTime(hour, true);
        String minuteString = prettyTime(minute, false);

        mTimeButton.setText(hourString + ":" + minuteString + " " + timeOfDay);
    }

    private String prettyTime(int time, boolean isHour) {
        if (isHour && time == 0) {
            time = 12;
        }
        return time < 10 ? "0" + time : time + "";
    }

    private String getCrimeReport() {
        String solvedString = null;
        solvedString = mCrime.isSolved() ?
                       getString(R.string.crime_report_solved) :
                       getString(R.string.crime_report_unsolved);

        String dateFormat = "EEE, MMM, dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        suspect = suspect != null ?
                  getString(R.string.crime_report_suspect, suspect) :
                  getString(R.string.crime_report_no_suspect);

        return getString(R.string.crime_report,
                         mCrime.getTitle(), dateString, solvedString, suspect);
    }
}

