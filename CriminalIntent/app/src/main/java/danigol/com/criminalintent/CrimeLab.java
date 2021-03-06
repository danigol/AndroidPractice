package danigol.com.criminalintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import danigol.com.criminalintent.database.CrimeBaseHelper;
import danigol.com.criminalintent.database.CrimeCursorWrapper;
import danigol.com.criminalintent.database.CrimeDbSchema.CrimeTable;

/**
 * Created by daniellegolinsky on 2/13/18.
 */
public class CrimeLab {

    private static CrimeLab sCrimeLab;

    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }

        return sCrimeLab;
    }

    private CrimeLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();
    }

    public void addCrime(Crime c) {
        ContentValues values = getContentValues(c);
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);

        mDatabase.update(CrimeTable.NAME, values,
                         CrimeTable.Cols.UUID + " = ?",
                         new String[] { uuidString });
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                CrimeTable.NAME,
                null, // Select all columns from table with null
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new CrimeCursorWrapper(cursor);
    }

    public void deleteCrime(Crime c) {
        mDatabase.delete(CrimeTable.NAME,
                         CrimeTable.Cols.UUID + " = ?",
                         new String[] { c.getId().toString() });

    }

    public List<Crime> getCrimes() {
        List<Crime> crimes = new ArrayList<>();
        CrimeCursorWrapper cursor = queryCrimes(null, null);
        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    crimes.add(cursor.getCrime());
                    cursor.moveToNext();
                }
            }
        }
        finally {
            cursor.close();
        }

        return crimes;
    }

    public Crime getCrime(UUID id) {
        CrimeCursorWrapper cursor = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );
        Crime c = null;
        try {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                c = cursor.getCrime();
            }
        }
        finally {
            cursor.close();
        }
        return c;
    }

    /**
     * Crime DB
     * Returns the content values for a given crime
     * @param crime
     * @return
     */
    private static ContentValues getContentValues(Crime crime) {
        ContentValues values = new ContentValues();

        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());
        values.put(CrimeTable.Cols.SUSPECT_PHONE, crime.getSuspectPhone());

        return values;
    }
}
