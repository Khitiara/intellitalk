package org.npenn.gifted.aacpecsapp;

import android.graphics.Bitmap;

class Word {
    public final String displayText;
    public final String spokenText;
    public final Bitmap image;

    public Word(String displayText, String spokenText, Bitmap image) {
        this.displayText = displayText;
        this.spokenText = spokenText;
        this.image = image;
    }
}
