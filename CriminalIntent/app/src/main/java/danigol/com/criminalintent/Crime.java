package danigol.com.criminalintent;

import android.text.format.DateFormat;

import java.util.Date;
import java.util.UUID;

/**
 * Created by daniellegolinsky on 2/12/18.
 */

public class Crime {

    private UUID mId;
    private String mTitle;
    private Date mDate;
    private boolean mSolved;
    private String mSuspect;
    private String mSuspectPhone;

    public Crime() {
        this(UUID.randomUUID());
    }

    public Crime(UUID id) {
        mId = id;
        mDate = new Date();
    }

    public UUID getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public String getDateString() {
        DateFormat df = new android.text.format.DateFormat();
        return df.format("E, MMM dd, yyyy", this.mDate).toString();
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public String getSuspect() {
        return mSuspect;
    }

    public void setSuspect(final String suspect) {
        mSuspect = suspect;
    }

    public String getSuspectPhone() {
        return mSuspectPhone;
    }
    public void setSuspectPhone(String phone) {
        mSuspectPhone = phone;
    }
}
