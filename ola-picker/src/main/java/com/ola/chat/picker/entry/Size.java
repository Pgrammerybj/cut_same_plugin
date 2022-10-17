package com.ola.chat.picker.entry;

import android.os.Parcel;
import android.os.Parcelable;

public class Size implements Parcelable {
    public final int width;
    public final int height;

    public Size(Parcel var1) {
        this.width = var1.readInt();
        this.height = var1.readInt();
    }

    public static final Creator<Size> CREATOR = new Creator<Size>() {
        @Override
        public Size createFromParcel(Parcel in) {
            return new Size(in);
        }

        @Override
        public Size[] newArray(int size) {
            return new Size[size];
        }
    };

    public void writeToParcel(Parcel var1, int var2) {
        var1.writeInt(this.width);
        var1.writeInt(this.height);
    }

    public int describeContents() {
        return 0;
    }
}
