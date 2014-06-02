package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.lucasr.twowayview.TwoWayView;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    private final List<Word> queue = Lists.newArrayList();
    private QueueAdapter queueAdapter;
    private boolean canPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        queueAdapter = new QueueAdapter(this);
        ((TwoWayView) findViewById(R.id.wordQueue)).setAdapter(queueAdapter);
        GridView commonPhraseView = (GridView) findViewById(R.id.commonPhraseView);
        commonPhraseView.setAdapter(new CommonPhraseAdapter(this));
        commonPhraseView.setOnItemClickListener(new CommonWordListItemClickListener());
        IntellitalkState.INSTANCE.textToSpeech.setOnUtteranceProgressListener(new UtteranceListener());
        canPlay = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_play:
                play();
                return true;
            case R.id.action_clear:
                clear();
                return true;
            case R.id.action_refresh:
                reload();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void play() {
        if (!canPlay) {
            Toast.makeText(this, "Text to speech not initialized!", Toast.LENGTH_LONG).show();
        }
        canPlay = false;

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
        IntellitalkState.INSTANCE.textToSpeech.speak(builder.toString(), TextToSpeech.QUEUE_FLUSH, params);
    }

    @Override
    public void onBackPressed() {
        IntellitalkState.INSTANCE.textToSpeech.shutdown();
        finish();
        System.exit(0);
    }

    public void reload() {
        Intent intent = new Intent(this, LoadingActivity.class);
        intent.putExtra(LoadingActivity.IS_RELOAD, true);
        startActivity(intent);
    }

    public void clear() {
        queue.clear();
        queueAdapter.notifyDataSetChanged();
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
                    canPlay = true;
                    queue.clear();
                    MainActivity.this.queueAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onError(String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    canPlay = true;
                    Toast.makeText(MainActivity.this, "Speech failed!", Toast.LENGTH_LONG).show();
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