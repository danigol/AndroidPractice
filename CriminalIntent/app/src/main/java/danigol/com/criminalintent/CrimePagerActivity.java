package danigol.com.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import static danigol.com.criminalintent.HelperMethods.hideSoftKeyboard;

/**
 * Created by daniellegolinsky on 4/30/18.
 */

public class CrimePagerActivity extends AppCompatActivity {

    private static final String EXTRA_CRIME_ID = "danigol.com.criminalintent.crime_id";

    private ViewPager mViewPager;
    private List<Crime> mCrimes;

    private TextView mCrimeNumber;
    private Button mFirstButton;
    private Button mLastButton;
    private EditText mGotoValue;
    private Button mGoto;

    public static Intent newIntent(Context packageContext, UUID crimeId) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        UUID crimeId = (UUID) getIntent().getSerializableExtra(EXTRA_CRIME_ID);

        mViewPager = (ViewPager) findViewById(R.id.crime_view_pager);

        mCrimes = CrimeLab.get(this).getCrimes();
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);
                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }
        });

        // Default to the tapped crime
        for (int i = 0; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).getId().equals(crimeId)) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }

        mCrimeNumber = findViewById(R.id.crime_number);
        mCrimeNumber.setText(setCrimeNumberText(mViewPager.getCurrentItem()));

        // Set up first/last/goto options
        mFirstButton = (Button) findViewById(R.id.first_button);
        mLastButton = (Button) findViewById(R.id.last_button);
        mFirstButton.setOnClickListener(v -> mViewPager.setCurrentItem(0));
        mLastButton.setOnClickListener(v -> mViewPager.setCurrentItem(mViewPager.getAdapter().getCount() - 1));

        mGoto = findViewById(R.id.pager_goto_button);
        mGoto.setOnClickListener(v -> gotoPage());

        mGotoValue = findViewById(R.id.pager_goto_value);
        mGotoValue.setText("" + mViewPager.getCurrentItem());
        mGoto.setEnabled(false); // Above, we matched the value. Here we set it to false

        // Listeners
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updateView();
            }
            @Override
            public void onPageSelected(int position) {
                updateView();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                updateView();
            }
        });

        mGotoValue.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) { // I don't like this, but I think it's a bug
                gotoPage();
                return true;
            }
            return false;
        });

        mGotoValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                int gotoValue = -1;
                try {
                    gotoValue = Integer.parseInt(s.toString());
                }
                catch(Exception e) {
                    // Something
                }
                mGoto.setEnabled(shouldEnableGoto(gotoValue));
            }

            @Override
            public void afterTextChanged(final Editable s) {
                String content = s.toString();
                if (content != null && content.endsWith("\n")) {
                    mGotoValue.setText(content.replace("\n", ""));
                    gotoPage();
                }
            }
        });
    }

    private boolean shouldEnableGoto(int gotoValue) {
        return gotoValue >= 0 && gotoValue < mCrimes.size() && mViewPager.getCurrentItem() != gotoValue;
    }

    public void gotoPage() {
        String gotoValueString = mGotoValue.getText().toString();
        int gotoValue = mViewPager.getCurrentItem();
        try {
            gotoValue = Integer.parseInt(gotoValueString);
            mGoto.setEnabled(false);
            hideSoftKeyboard(this);
        }
        catch(Exception e) {
        }
        if (gotoValue != mViewPager.getCurrentItem()) {
            mViewPager.setCurrentItem(gotoValue);
        }
    }

    private void updateView() {
        mFirstButton.setEnabled(mViewPager.getCurrentItem() != 0);
        mLastButton.setEnabled(mViewPager.getCurrentItem() != mViewPager.getAdapter().getCount() - 1);
        mGotoValue.setText("" + mViewPager.getCurrentItem());
        mCrimeNumber.setText(setCrimeNumberText(mViewPager.getCurrentItem()));
        mGoto.setEnabled(false);
    }

    private String setCrimeNumberText(int crimeNumber) {
        return String.format("Crime number: %d", crimeNumber);
    }
}
