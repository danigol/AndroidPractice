package danigol.com.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
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
import android.widget.TextView;

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
    private static final int REQUEST_CONTACT = 2;
    private static final int CONTACTS_PERMISSION = 3;

    private boolean canAccessContacts = false;

    private Crime mCrime;
    private Button mDateButton;
    private Button mTimeButton;
    private EditText mTitleField;
    private CheckBox mSolvedCheckBox;
    private Button mSuspectButton;
    private Button mSuspectPhoneButton;
    private Button mReportButton;
    private TextView mContactsTip;

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

        // Crime title field.
        setupTitleField(v);

        // Date button should launch dialog fragment with date picker
        setupDateButton(v);

        // Set up the time button
        setupTimeButton(v);

        // Set up the solved check box button
        setupSolvedCheckbox(v);

        // Set Suspect (Contact picker) button
        setupSuspectButton(v);

        // Call Suspect button
        setupCallSuspectButton(v);

        // Add a contacts tip for permissions
        setupContactPermissionText(v);

        // Set up the report button
        setupReportCrimeButton(v);

        // Request contacts on first open
        requestPermissions(new String[]{ Manifest.permission.READ_CONTACTS }, CONTACTS_PERMISSION);

        // Disable contacts related buttons if user has not chosen them.
        updateContactsButtons();

        return v;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case CONTACTS_PERMISSION:
                canAccessContacts = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                updateContactsButtons();
                break;
            default:
                break;
        }
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
            case REQUEST_CONTACT:
                if (data != null) {
                    Uri contactUri = data.getData();

                    String[] queryFields = new String[] {
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts._ID
                    };

                    Cursor c = getActivity().getContentResolver()
                                            .query(contactUri, queryFields, null, null, null);

                    try {
                        if (c.getCount() == 0) {
                            return;
                        }

                        c.moveToFirst();
                        String suspect = c.getString(0);
                        mCrime.setSuspect(suspect);

                        // Try to grab the phone number
                        String contactID = c.getString(1);
                        Cursor phoneCursor = getActivity().getContentResolver()
                                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                       null,
                                       ContactsContract.CommonDataKinds.Phone.CONTACT_ID +
                                               " = " + contactID,
                                       null, null);

                        if (phoneCursor.getCount() > 0) {
                            phoneCursor.moveToFirst();
                            String number = phoneCursor.getString(
                                        phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    .replace(" ", "")
                                    .replace("(", "")
                                    .replace(")", "")
                                    .replace("-", "");
                            mCrime.setSuspectPhone(number);
                        }
                        else {
                            mCrime.setSuspectPhone(null);
                        }
                    }
                    finally {
                        c.close();
                    }
                }
                break;
            default:
                break;
        }

        if (requestCode == REQUEST_DATE || requestCode == REQUEST_TIME) {
            Date date = (Date) data.getSerializableExtra(extra);
            mCrime.setDate(date);

            if (promptForTime) {
                // Prompt user for the time now
                promptUserForTime();
                // Update our date with the time
                mCrime.setDate(date);
            }
        }

        // Update the GUI
        updateGUI();
    }

    /**
     * Ask the user to input the time of the crime
     */
    private void promptUserForTime() {
        FragmentManager manager = getFragmentManager();
        TimePickerFragment timePicker = TimePickerFragment.newInstance(mCrime.getDate());
        timePicker.setTargetFragment(CrimeFragment.this, REQUEST_TIME);
        timePicker.show(manager, DIALOG_TIME);
    }

    /**
     * Calls both updateDate() and updateTime() to update the GUI
     */
    private void updateDateAndTime() {
        updateDate();
        updateTime();
    }

    /**
     * Sets the date on the button
     */
    private void updateDate() {
        mDateButton.setText(mCrime.getDateString());
    }

    /**
     * Updates the text of the time
     */
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

    /**
     * Format the time
     * @param time
     * @param isHour
     * @return
     */
    private String prettyTime(int time, boolean isHour) {
        if (isHour && time == 0) {
            time = 12;
        }
        return time < 10 ? "0" + time : time + "";
    }

    /**
     * Generate the string for the crime report that we send via email
     * @return
     */
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

    /**
     * Update the contact selection and calling buttons for suspects
     * Will add text or enable/disable
     */
    private void updateContactsButtons() {
        mSuspectButton.setEnabled(canAccessContacts);
        mSuspectPhoneButton.setEnabled(canAccessContacts);

        if (!canAccessContacts) {
            mContactsTip.setVisibility(View.VISIBLE);
        }
        else {
            mContactsTip.setVisibility(View.INVISIBLE);
        }

        if (mCrime != null && mCrime.getSuspect() != null) {
            mSuspectButton.setText("Suspect: " + mCrime.getSuspect());
        }

        if (mCrime != null && mCrime.getSuspectPhone() != null) {
            mSuspectPhoneButton.setText("Call Suspect: " + mCrime.getSuspectPhone());
        }
    }

    /**
     * Calls all GUI updates
     */
    private void updateGUI() {
        updateDateAndTime();
        updateContactsButtons();
        // Update the DB
        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    ///////////////////////
    /** SET UP THE VIEW **/
    ///////////////////////

    /**
     * Initialize the title field
     * @param v
     */
    private void setupTitleField(View v) {
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
    }

    /**
     * Initialize the Date button
     * @param v
     */
    private void setupDateButton(View v) {
        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setEnabled(true);
        mDateButton.setOnClickListener(dateButtonView -> {
            FragmentManager manager = getFragmentManager();
            DatePickerFragment dateDialog = DatePickerFragment.newInstance(mCrime.getDate());
            dateDialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
            dateDialog.show(manager, DIALOG_DATE);
        });
    }

    /**
     * Initialize the time button
     * @param v
     */
    private void setupTimeButton(View v) {
        mTimeButton = (Button) v.findViewById(R.id.crime_time);
        updateTime();
        mTimeButton.setEnabled(true);
        mTimeButton.setOnClickListener(timeButtonView -> promptUserForTime());
    }

    /**
     * Initialize the solved checkbox
     * @param v
     */
    private void setupSolvedCheckbox(View v) {
        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> mCrime.setSolved(isChecked));
    }

    /**
     * Initialize the suspect button
     * This chooses a contact to label as a suspect
     * @param v
     */
    private void setupSuspectButton(View v) {
        mSuspectButton = v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(suspectButtonView -> {
            if (canAccessContacts) {
                Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });
    }

    /**
     * Initialize the call suspect button
     * @param v
     */
    private void setupCallSuspectButton(View v) {
        mSuspectPhoneButton = v.findViewById(R.id.crime_call);
        mSuspectPhoneButton.setOnClickListener(callView -> {
            if (canAccessContacts) {
                final Intent callContact = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mCrime.getSuspectPhone()));
                startActivity(callContact);
            }
        });
    }

    /**
     * Setup the contacts permission text
     * This will default to invisible unless the user
     *      does not give us contacts access.
     * @param v
     */
    private void setupContactPermissionText(View v) {
        mContactsTip = v.findViewById(R.id.contacts_access_tip);
        mContactsTip.setVisibility(View.INVISIBLE);
    }

    /**
     * Initialize the report crime button
     * This will send an email to report the crime
     * @param v
     */
    private void setupReportCrimeButton(View v) {
        mReportButton = v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(reportButtonView -> {
            Intent i = ShareCompat.IntentBuilder.from(this.getActivity())
                                                .setType("text/plain")
                                                .setChooserTitle(getString(R.string.crime_report_subject))
                                                .setText(getCrimeReport())
                                                .setSubject(getString(R.string.crime_report_subject))
                                                .getIntent();
            startActivity(i);
        });
    }
}

