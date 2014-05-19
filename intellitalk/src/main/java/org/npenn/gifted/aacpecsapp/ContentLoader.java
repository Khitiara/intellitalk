package org.npenn.gifted.aacpecsapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class ContentLoader {
    public static final ContentLoader INSTANCE = new ContentLoader();
    public IntellitalkContent content = new IntellitalkContent();

    private ContentLoader() {

    }

    public void load(Context context) throws IOException {
        content = new IntellitalkContent();
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(new BufferedReader(new InputStreamReader(context.openFileInput("data.json")))).getAsJsonObject();
        JsonArray commonPhrases = json.getAsJsonArray("common_phrases");
        for (JsonElement phrase : commonPhrases) {
            JsonObject phraseObject = phrase.getAsJsonObject();
            String displayText = phraseObject.get("display_text").getAsString();
            String spokenText = phraseObject.get("spoken_text").getAsString();
            Bitmap image = getBitmap(phraseObject);
            Word phraseWord = new Word(displayText, spokenText, image);
            content.commonWords.add(phraseWord);
        }

        parseCategories(json.getAsJsonArray("categories"));
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

    private void parseCategories(JsonArray rootCategories) {
        List<CategoryParseHelper> helpers = Lists.newArrayList();
        helpers.add(new CategoryParseHelper(null, rootCategories));
        while (!helpers.isEmpty()) {
            CategoryParseHelper helper = helpers.remove(0);
            for (JsonElement categoryRaw : helper.content) {
                JsonObject categoryObject = categoryRaw.getAsJsonObject();
                String name = categoryObject.get("name").getAsString();
                Bitmap image = categoryObject.has("image") ? getBitmap(categoryObject) : null;
                JsonArray words = categoryObject.getAsJsonArray("contents");
                List<Word> content = Lists.newArrayList();
                for (JsonElement wordRaw : words) {
                    JsonObject wordObject = wordRaw.getAsJsonObject();
                    String displayText = wordObject.get("display_text").getAsString();
                    String spokenText = wordObject.get("spoken_text").getAsString();
                    Bitmap wordImage = getBitmap(wordObject);
                    Word word = new Word(displayText, spokenText, wordImage);
                    content.add(word);
                }
                WordCategory category = new WordCategory(name, image, content);
                if (helper.root == null) {
                    this.content.categories.add(category);
                } else {
                    helper.root.nestedCategories.add(category);
                }
                if (categoryObject.has("categories")) {
                    helpers.add(new CategoryParseHelper(category, categoryObject.getAsJsonArray("categories")));
                }
            }
        }
    }

    public class IntellitalkContent {
        public final List<Word> commonWords = Lists.newArrayList();
        public final List<WordCategory> categories = Lists.newArrayList();
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
