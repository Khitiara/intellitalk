package org.npenn.gifted.aacpecsapp;

import android.graphics.Bitmap;

import com.google.common.collect.Lists;

import java.util.List;

public class WordCategory {
    public String name;
    public Bitmap image;
    public List<Word> content;
    public List<WordCategory> nestedCategories = Lists.newArrayList();

    public WordCategory(String name, Bitmap image, Iterable<Word> content) {
        this.name = name;
        this.image = image;
        this.content = Lists.newArrayList(content);
    }
}
