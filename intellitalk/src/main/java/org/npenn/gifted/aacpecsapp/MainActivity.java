package org.npenn.gifted.aacpecsapp;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jess.ui.TwoWayAdapterView;

import org.lucasr.twowayview.TwoWayView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    static final String dataTemplateUrl = "https://raw.github.com/robotbrain/intellitalk/master/datatemplate.json";
    TextToSpeech textToSpeech;
    ImageButton playButton;
    QueueAdapter queueAdapter;
    List<Word> queue = Lists.newArrayList();
    ContentLoader contentLoader = new ContentLoader();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        {
            File data = new File(this.getFilesDir(), "data.json");
            if (!data.exists()) {
                //Todo: download base file
                //Todo: host base file (derp)
            } else if (!data.isFile()) {
                Toast.makeText(this, "Error loading content! This means your install may be corrupted!\nDetails: the data file is a directory or system file.", Toast.LENGTH_LONG).show();
                return;
            } else {
                //Todo: check if file is up-to-date
            }
        }

        try {
            contentLoader.load(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading content! This means your install may be corrupted!\nDetails: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        queueAdapter = new QueueAdapter(this);
        ((TwoWayView) findViewById(R.id.wordQueue)).setAdapter(queueAdapter);
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

    public void launchSettings(MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onInit(int i) {
        if (i == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
            playButton.setEnabled(true);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceListener());
            Toast.makeText(this, "Text to speech initialized properly! I can talk!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Test to speech initialization error! Try restarting the app.", Toast.LENGTH_LONG).show();
        }
    }

    public void play(View view) {
        playButton.setEnabled(false);

        //Make text now
        StringBuilder builder = new StringBuilder();
        for (Word w : queue) {
            if (builder.length() != 0 && !Lists.newArrayList(".", ",", "!", "?", ";", ":").contains(w.spokenText)) {
                builder.append(' ');
            }
            builder.append(w.spokenText);
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

    public static class QueueAdapter extends BaseAdapter {
        MainActivity activity;

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
                view = activity.getLayoutInflater().inflate(R.layout.queue_item, null);
            }
            Word w = activity.queue.get(i);
            if (w.image != null) {
                ((ImageView) view.findViewById(R.id.item_image)).setImageBitmap(w.image);
            }
            ((TextView) view.findViewById(R.id.item_text)).setText(w.displayText);
            return view;
        }
    }

    class UtteranceListener extends UtteranceProgressListener {

        @Override
        public void onStart(String s) {

        }

        @Override
        public void onDone(String s) {
            playButton.post(new Runnable() {
                @Override
                public void run() {
                    playButton.setEnabled(true);
                    //queue.clear();
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

    class CommonWordListItemClickListener extends BaseAdapter implements TwoWayAdapterView.OnItemClickListener {


        @Override
        public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public int getCount() {
            return 0;
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
            return null;
        }
    }
}