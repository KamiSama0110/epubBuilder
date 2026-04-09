package org.epubBuilder.model;

import java.util.UUID;

public class PreliminaryPage {

    public enum Type { IMAGEN, TEXTO, IMAGEN_Y_TEXTO }

    private final String id = UUID.randomUUID().toString();
    private String title;
    private Type   type;
    private String imagePath;
    private String text;

    public PreliminaryPage(String title, Type type) {
        this.title     = title;
        this.type      = type;
        this.imagePath = "";
        this.text      = "";
    }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public void   setTitle(String t)   { title = t; }
    public Type   getType()            { return type; }
    public void   setType(Type t)      { type = t; }
    public String getImagePath()       { return imagePath; }
    public void   setImagePath(String p){ imagePath = p; }
    public String getText()            { return text; }
    public void   setText(String t)    { text = t; }

    @Override
    public String toString() {
        return title.isBlank() ? "(sin título)" : title;
    }
}