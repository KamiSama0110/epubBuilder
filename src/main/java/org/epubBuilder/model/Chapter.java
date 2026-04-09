package org.epubBuilder.model;

import java.util.UUID;

public class Chapter {
    private final String id = UUID.randomUUID().toString();
    private String title;
    private String body; // texto plano con marcas [IMAGEN:path]

    public Chapter(String title) {
        this.title = title;
        this.body  = "";
    }

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public void setTitle(String t) { title = t; }
    public String getBody()        { return body; }
    public void setBody(String b)  { body = b; }

    @Override
    public String toString() { return title.isBlank() ? "(sin título)" : title; }
}