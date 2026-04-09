package org.epubBuilder.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.ArrayList;
import java.util.List;

public class Book {

    private final StringProperty title       = new SimpleStringProperty("");
    private final StringProperty author      = new SimpleStringProperty("");
    private final StringProperty illustrator = new SimpleStringProperty("");
    private final StringProperty synopsis    = new SimpleStringProperty("");
    private final StringProperty language    = new SimpleStringProperty("es");
    private final StringProperty coverPath   = new SimpleStringProperty("");
    private final List<PreliminaryPage> preliminaryPages = new ArrayList<>();

    private final List<Chapter> chapters = new ArrayList<>();

    // --- Getters de propiedades (para binding JavaFX) ---
    public StringProperty titleProperty()       { return title; }
    public StringProperty authorProperty()      { return author; }
    public StringProperty illustratorProperty() { return illustrator; }
    public StringProperty synopsisProperty()    { return synopsis; }
    public StringProperty languageProperty()    { return language; }
    public StringProperty coverPathProperty()   { return coverPath; }

    // --- Getters/Setters normales ---
    public String getTitle()       { return title.get(); }
    public void setTitle(String v) { title.set(v); }

    public String getAuthor()       { return author.get(); }
    public void setAuthor(String v) { author.set(v); }

    public String getIllustrator()       { return illustrator.get(); }
    public void setIllustrator(String v) { illustrator.set(v); }

    public String getSynopsis()       { return synopsis.get(); }
    public void setSynopsis(String v) { synopsis.set(v); }

    public String getLanguage()       { return language.get(); }
    public void setLanguage(String v) { language.set(v); }

    public String getCoverPath()       { return coverPath.get(); }
    public void setCoverPath(String v) { coverPath.set(v); }

    public List<PreliminaryPage> getPreliminaryPages() { return preliminaryPages; }

    public List<Chapter> getChapters() { return chapters; }
}
