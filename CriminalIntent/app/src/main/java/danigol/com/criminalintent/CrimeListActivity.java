package danigol.com.criminalintent;

import android.support.v4.app.Fragment;

/**
 * Created by daniellegolinsky on 2/14/18.
 */

public class CrimeListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }
}
