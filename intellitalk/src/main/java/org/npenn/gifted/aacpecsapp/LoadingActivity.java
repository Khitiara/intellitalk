package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;


public class LoadingActivity extends Activity implements TextToSpeech.OnInitListener {
    public static final String IS_RELOAD = "isreload@intellitalk";
    private static final String dataTemplateUrl = "https://raw.github.com/intellitalkdev/intellitalk/develop/datatemplate.json";
    boolean isReload = false;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(IS_RELOAD)) {
            isReload = savedInstanceState.getBoolean(IS_RELOAD);
        }

        setContentView(R.layout.activity_loading);

        textView = (TextView) findViewById(R.id.loadingBox);

        File dataFile = new File(this.getFilesDir(), "data.json");
        try {
            Files.copy(dataFile, new File(this.getFilesDir(), "data.json.backup"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataFile.delete();
        new DataTemplateDownloadTask(this).execute(new Download(dataFile));
    }

    @Override
    public void onBackPressed() {
        if (!isReload) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.loading, menu);
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

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            IntellitalkState.INSTANCE.textToSpeech.setLanguage(Locale.US);
            IntellitalkState.INSTANCE.textToSpeech.setSpeechRate(.75f);
            Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Log.e("loading", "Error with Text to Speech init!");
            AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
            builder.setTitle("Error!").setMessage("Error loading tts!").setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    System.exit(0);
                }
            }).show();

        }
    }

    public class DataTemplateDownloadTask extends AsyncTask<Download, Long, String> {
        private final Context context;
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
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                try {
                    Files.copy(new File(getFilesDir(), "data.json.backup"), new File(getFilesDir(), "data.json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.e("downloading", result);
            } else {
                textView.setText("Loading file...");
            }
            try {
                ContentLoader.INSTANCE.load(LoadingActivity.this);
                textView.setText("Loading text-to-speech...");
                IntellitalkState.INSTANCE.textToSpeech = new TextToSpeech(getApplicationContext(), LoadingActivity.this);
            } catch (IOException e) {
                Log.e("loading", "Error loading data", e);
                AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                builder.setTitle("Error!").setMessage("Error loading data!\n\n" + e.getMessage()).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                }).show();
            }
        }

    }

    public class Download {
        public final String url;
        public final File outputFile;

        public Download(File outputFile) {
            this.url = LoadingActivity.dataTemplateUrl;
            this.outputFile = outputFile;
        }
    }
}
