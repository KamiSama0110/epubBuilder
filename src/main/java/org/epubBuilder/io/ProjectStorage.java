package org.epubBuilder.io;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.Chapter;
import org.epubBuilder.model.PreliminaryPage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class ProjectStorage {

    private static final String AUTOSAVE_NAME = ".epubBuilder-autosave.epb";

    private ProjectStorage() {
    }

    public static File autosaveFile() {
        return new File(System.getProperty("user.home"), AUTOSAVE_NAME);
    }

    public static void save(Book book, File file) throws IOException {
        if (file == null) throw new IOException("Archivo invalido");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Snapshot snapshot = Snapshot.fromBook(book);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(snapshot);
        }
    }

    public static void loadInto(Book target, File file) throws IOException {
        if (target == null || file == null || !file.exists()) {
            throw new IOException("No se puede abrir el proyecto");
        }

        Object obj;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            obj = in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Formato de proyecto incompatible", e);
        }

        if (!(obj instanceof Snapshot snapshot)) {
            throw new IOException("Formato de archivo invalido");
        }

        snapshot.applyTo(target);
    }

    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String author;
        private String illustrator;
        private String synopsis;
        private String language;
        private String coverPath;
        private List<ChapterData> chapters;
        private List<PreliminaryData> preliminaries;

        static Snapshot fromBook(Book book) {
            Snapshot snapshot = new Snapshot();
            snapshot.title = safe(book.getTitle());
            snapshot.author = safe(book.getAuthor());
            snapshot.illustrator = safe(book.getIllustrator());
            snapshot.synopsis = safe(book.getSynopsis());
            snapshot.language = safe(book.getLanguage());
            snapshot.coverPath = safe(book.getCoverPath());

            snapshot.chapters = new ArrayList<>();
            for (Chapter chapter : book.getChapters()) {
                snapshot.chapters.add(new ChapterData(safe(chapter.getTitle()), safe(chapter.getBody())));
            }

            snapshot.preliminaries = new ArrayList<>();
            for (PreliminaryPage page : book.getPreliminaryPages()) {
                snapshot.preliminaries.add(new PreliminaryData(
                        safe(page.getTitle()),
                        page.getType().name(),
                        safe(page.getImagePath()),
                        safe(page.getText())
                ));
            }

            return snapshot;
        }

        void applyTo(Book target) {
            target.setTitle(safe(title));
            target.setAuthor(safe(author));
            target.setIllustrator(safe(illustrator));
            target.setSynopsis(safe(synopsis));
            target.setLanguage(safe(language).isBlank() ? "es" : safe(language));
            target.setCoverPath(safe(coverPath));

            target.getChapters().clear();
            if (chapters != null) {
                for (ChapterData chapter : chapters) {
                    Chapter c = new Chapter(safe(chapter.title));
                    c.setBody(safe(chapter.body));
                    target.getChapters().add(c);
                }
            }

            target.getPreliminaryPages().clear();
            if (preliminaries != null) {
                for (PreliminaryData page : preliminaries) {
                    PreliminaryPage.Type type;
                    try {
                        type = PreliminaryPage.Type.valueOf(safe(page.type));
                    } catch (IllegalArgumentException ex) {
                        type = PreliminaryPage.Type.IMAGEN;
                    }
                    PreliminaryPage p = new PreliminaryPage(safe(page.title), type);
                    p.setImagePath(safe(page.imagePath));
                    p.setText(safe(page.text));
                    target.getPreliminaryPages().add(p);
                }
            }
        }
    }

    private static class ChapterData implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String title;
        private final String body;

        private ChapterData(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

    private static class PreliminaryData implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String title;
        private final String type;
        private final String imagePath;
        private final String text;

        private PreliminaryData(String title, String type, String imagePath, String text) {
            this.title = title;
            this.type = type;
            this.imagePath = imagePath;
            this.text = text;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}