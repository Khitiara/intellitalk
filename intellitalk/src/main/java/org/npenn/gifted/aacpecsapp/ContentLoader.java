package org.npenn.gifted.aacpecsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class ContentLoader extends AsyncTaskLoader<ContentLoader.IntellitalkContent> implements TextToSpeech.OnInitListener {

    private static final String dataTemplateUrl = "https://raw.github.com/intellitalkdev/intellitalk/develop/datatemplate.json";
    CountDownLatch ttsDone = new CountDownLatch(1);

    public ContentLoader(Context context) {
        super(context);
    }

    @Override
    public void cancelLoadInBackground() {
        ttsDone.countDown();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            IntellitalkState.INSTANCE.textToSpeech.setLanguage(Locale.US);
            IntellitalkState.INSTANCE.textToSpeech.setSpeechRate(.75f);
            Log.i("intellitalk.loading", "Done TTS.");
            ttsDone.countDown();
        } else {
            Log.e("intellitalk.loading", "Error with Text to Speech init!");
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Error!").setMessage("Error loading tts!").setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((Activity) getContext()).finish();
                    System.exit(0);
                }
            }).show();
        }
    }

    private Bitmap getBitmap(JsonObject jsonObject) {
        if (!jsonObject.has("image")) {
            return null;
        }
        String imageBase64 = jsonObject.get("image").getAsString();
        if (imageBase64.isEmpty()) {
            return null;
        }
        byte[] imageRaw = Base64.decode(imageBase64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageRaw, 0, imageRaw.length);
    }

    private void parseCategories(IntellitalkContent intellitalkContent, JsonArray rootCategories) {
        List<CategoryParseHelper> helpers = Lists.newArrayList();
        helpers.add(new CategoryParseHelper(null, rootCategories));
        while (!helpers.isEmpty()) {
            CategoryParseHelper helper = helpers.remove(0);
            for (JsonElement categoryRaw : helper.content) {
                JsonObject categoryObject = categoryRaw.getAsJsonObject();
                String name = categoryObject.get("name").getAsString();
                Bitmap image = getBitmap(categoryObject);
                JsonArray words = categoryObject.getAsJsonArray("contents");
                List<Word> content = Lists.newArrayList();
                for (JsonElement wordRaw : words) {
                    if (this.isLoadInBackgroundCanceled()) {
                        return;
                    }
                    JsonObject wordObject = wordRaw.getAsJsonObject();
                    String displayText = wordObject.get("display_text").getAsString();
                    String spokenText = wordObject.get("spoken_text").getAsString();
                    Bitmap wordImage = getBitmap(wordObject);
                    Word word = new Word(displayText, spokenText, wordImage);
                    content.add(word);
                    intellitalkContent.commonWords.add(word);  //Until categories are added.
                }
                WordCategory category = new WordCategory(name, image, content);
                /*if (helper.root == null) {
                    intellitalkContent.categories.add(category);
                } else {
                    helper.root.nestedCategories.add(category);
                }*/
                if (categoryObject.has("categories")) {
                    helpers.add(new CategoryParseHelper(category, categoryObject.getAsJsonArray("categories")));
                }
            }
        }
    }

    @Override
    public IntellitalkContent loadInBackground() {

        File dataFile = new File(this.getContext().getFilesDir(), "data.json");
        try {
            Files.copy(dataFile, new File(this.getContext().getFilesDir(), "data.json.backup"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //noinspection ResultOfMethodCallIgnored
        dataFile.delete();

        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        wakeLock.acquire();
        Log.i("intellitalk.download", "Starting download.");
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(dataTemplateUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw Throwables.propagate(new IOException("Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage()));
            }

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(dataFile);

            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                if (this.isLoadInBackgroundCanceled()) {
                    input.close();
                    return null;
                }
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
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
        if (this.isLoadInBackgroundCanceled()) {
            return null;
        }
        IntellitalkContent content = new IntellitalkContent();
        JsonParser parser = new JsonParser();
        JsonObject json;
        try {
            json = parser.parse(new BufferedReader(new InputStreamReader(getContext().openFileInput("data.json")))).getAsJsonObject();
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        }
        JsonArray commonPhrases = json.getAsJsonArray("common_phrases");
        for (JsonElement phrase : commonPhrases) {
            JsonObject phraseObject = phrase.getAsJsonObject();
            String displayText = phraseObject.get("display_text").getAsString();
            String spokenText = phraseObject.get("spoken_text").getAsString();
            Bitmap image = getBitmap(phraseObject);
            Word phraseWord = new Word(displayText, spokenText, image);
            content.commonWords.add(phraseWord);
            if (this.isLoadInBackgroundCanceled()) {
                return null;
            }
        }

        parseCategories(content, json.getAsJsonArray("categories"));

        Log.i("intellitalk.loading", "Done file.");

        IntellitalkState.INSTANCE.textToSpeech = new TextToSpeech(getContext().getApplicationContext(), this);
        try {
            ttsDone.await();
        } catch (InterruptedException e) {
            //Ignore
        }

        wakeLock.release();
        if (this.isLoadInBackgroundCanceled()) {
            return null;
        }
        return content;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    public class IntellitalkContent {
        public final List<Word> commonWords = Lists.newArrayList();
    }

    private class CategoryParseHelper {
        final WordCategory root;
        final JsonArray content;

        private CategoryParseHelper(WordCategory root, JsonArray content) {
            this.root = root;
            this.content = content;
        }
    }
}
