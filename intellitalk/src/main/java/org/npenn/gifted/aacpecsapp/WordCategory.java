package org.npenn.gifted.aacpecsapp;

import android.graphics.Bitmap;

import com.google.common.collect.Lists;

import java.util.List;

class WordCategory {
    public final List<WordCategory> nestedCategories = Lists.newArrayList();

    public WordCategory(String name, Bitmap image, Iterable<Word> content) {
        String name1 = name;
        Bitmap image1 = image;
        List<Word> content1 = Lists.newArrayList(content);
    }
}
