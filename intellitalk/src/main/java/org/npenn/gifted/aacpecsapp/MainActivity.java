package org.npenn.gifted.aacpecsapp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.lucasr.twowayview.TwoWayView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    private final List<Word> queue = Lists.newArrayList();
    private TextToSpeech textToSpeech;
    private ImageButton playButton;
    private QueueAdapter queueAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        manager.setMode(AudioManager.MODE_NORMAL);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        queueAdapter = new QueueAdapter(this);
        ((TwoWayView) findViewById(R.id.wordQueue)).setAdapter(queueAdapter);
        GridView commonPhraseView = (GridView) findViewById(R.id.commonPhraseView);
        commonPhraseView.setAdapter(new CommonPhraseAdapter(this));
        commonPhraseView.setOnItemClickListener(new CommonWordListItemClickListener());
        playButton = (ImageButton) findViewById(R.id.playButton);
        playButton.setEnabled(false);
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("UnusedParameters")
    public void launchSettings(MenuItem unused) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onInit(int i) {
        if (i == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
            playButton.setEnabled(true);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceListener());
            textToSpeech.setSpeechRate(.75f);
            Toast.makeText(this, "Text to speech initialized properly! I can talk!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Test to speech initialization error! Try restarting the app.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void play(View unused) {
        playButton.setEnabled(false);

        //Make text now
        StringBuilder builder = new StringBuilder();
        for (Word w : queue) {
            builder.append(w.spokenText);
            if (builder.length() != 0 && !Lists.newArrayList(".", ",", "!", "?", ";", ":").contains(w.spokenText)) {
                builder.append(' ');
            }
        }
        HashMap<String, String> params = Maps.newHashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Intellitalk speech");
        textToSpeech.speak(builder.toString(), TextToSpeech.QUEUE_FLUSH, params);
    }

    @Override
    protected void onDestroy() {
        textToSpeech.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

    }

    public void reload(MenuItem item) {
        Intent intent = new Intent(this, LoadingActivity.class);
        intent.putExtra(LoadingActivity.IS_RELOAD, true);
        startActivity(intent);
    }

    public void clear(View view) {
        queue.clear();
    }

    public static class QueueAdapter extends BaseAdapter {
        final MainActivity activity;

        public QueueAdapter(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            return activity.queue.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = activity.getLayoutInflater().inflate(R.layout.word_layout, null);
            }
            Word w = activity.queue.get(i);
            if (w.image != null) {
                ((ImageView) view.findViewById(R.id.item_image)).setImageBitmap(w.image);
            } else {
                ((TextView) view.findViewById(R.id.item_text)).setText(w.displayText);
            }
            return view;
        }
    }

    private class UtteranceListener extends UtteranceProgressListener {

        @Override
        public void onStart(String s) {

        }

        @Override
        public void onDone(String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playButton.setEnabled(true);
                    queue.clear();
                    MainActivity.this.queueAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onError(String s) {
            playButton.post(new Runnable() {
                @Override
                public void run() {
                    playButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Text to speech error! Please try again.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class CommonWordListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final Word w = (Word) adapterView.getItemAtPosition(i);
            MainActivity.this.queue.add(w);
            MainActivity.this.queueAdapter.notifyDataSetChanged();
        }
    }
}