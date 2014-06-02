package org.npenn.gifted.aacpecsapp;

import android.speech.tts.TextToSpeech;

public class IntellitalkState {
    public static final IntellitalkState INSTANCE = new IntellitalkState();
    public TextToSpeech textToSpeech;

    private IntellitalkState() {

    }
}
