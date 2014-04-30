package org.npenn.gifted.aacpecsapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.lucasr.twowayview.TwoWayView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    static final String dataTemplateUrl = "https://raw.github.com/robotbrain/intellitalk/master/datatemplate.json";
    private final Object okLock = new Object();
    TextToSpeech textToSpeech;
    ImageButton playButton;
    QueueAdapter queueAdapter;
    List<Word> queue = Lists.newArrayList();
    ProgressDialog mProgressDialog;
    private boolean ttsOk = false;
    private boolean dlOk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File dataFile = new File(this.getFilesDir(), "data.json");
        dataFile.delete();
        if (!dataFile.exists()) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Downloading initial data......");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

            new DataTemplateDownloadTask(this).execute(new Download(dataTemplateUrl, dataFile));
            mProgressDialog.show();

        } else {
            try {
                ContentLoader.INSTANCE.load(MainActivity.this);

                TwoWayView commonPhraseView = (TwoWayView) findViewById(R.id.commonPhraseView);
                commonPhraseView.setOnItemClickListener(new CommonWordListItemClickListener());
                commonPhraseView.setAdapter(new CommonPhraseAdapter(this));
            } catch (IOException e) {
                Log.e("loading", "Error loading data", e);
                Toast.makeText(MainActivity.this, "Error loading data!", Toast.LENGTH_LONG).show();
            }
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
            synchronized (okLock) {
                if (dlOk) {
                    playButton.setEnabled(true);
                } else {
                    ttsOk = true;
                }
            }
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
                view = activity.getLayoutInflater().inflate(R.layout.word_layout, null);
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
                    queue.clear();
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

    class CommonWordListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final Word w = (Word) adapterView.getItemAtPosition(i);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.queue.add(w);
                }
            });
        }
    }

    public class DataTemplateDownloadTask extends AsyncTask<Download, Long, String> {
        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private long max;

        public DataTemplateDownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Download... downloads) {
            Log.w("download", "its running");
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(downloads[0].url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                max = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(downloads[0].outputFile);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (max > 0) // only if total length is known
                        publishProgress(total);
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Long... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax((int) max);
            mProgressDialog.setProgress(progress[0].intValue());
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.e("downloading", result);
            } else {
                Toast.makeText(context, "Done downloading", Toast.LENGTH_SHORT).show();
            }
            try {
                ContentLoader.INSTANCE.load(MainActivity.this);


                TwoWayView commonPhraseView = (TwoWayView) findViewById(R.id.commonPhraseView);
                commonPhraseView.setOnItemClickListener(new CommonWordListItemClickListener());
                commonPhraseView.setAdapter(new CommonPhraseAdapter(MainActivity.this));
            } catch (IOException e) {
                Log.e("loading", "Error loading data", e);
                Toast.makeText(MainActivity.this, "Error loading data!", Toast.LENGTH_LONG).show();
            }
        }

    }

    public class Download {
        public String url;
        public File outputFile;

        public Download(String url, File outputFile) {
            this.url = url;
            this.outputFile = outputFile;
        }
    }
}