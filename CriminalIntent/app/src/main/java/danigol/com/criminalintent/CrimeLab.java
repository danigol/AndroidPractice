package danigol.com.criminalintent;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by daniellegolinsky on 2/13/18.
 */
public class CrimeLab {

    private static CrimeLab sCrimeLab;

    private List<Crime> mCrimes;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }

        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mCrimes = new ArrayList<>();
    }

    public List<Crime> getCrimes() {
        return mCrimes;
    }

    public Crime getCrime(UUID id) {
        Crime crime = null;
        // Delicious Java 8 way that stupid backwards-ass Google didn't support until NOUGAT
        // SERIOUSLY, NOUGAT. What the actual fuck is wrong with Google? God, Android sucks.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            crime = mCrimes.stream()
                    .parallel()
                    .filter(c -> c.getId() == id)
                    .findFirst()
                    .orElse(null);
        }
        else{
            // Do it the nasty old java 7 way
            for (Crime c : mCrimes) {
                if (c.getId() == id) {
                    crime = c;
                }
            }
        }
        return crime;
    }
}