package org.epubBuilder;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.Chapter;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.epubBuilder.model.PreliminaryPage;

public class EpubExporter {

    private static final Pattern GLOSS_PATTERN = Pattern.compile("\\[\\[GLOSS:([^|\\]]+)\\|([^|\\]]*)\\|([^\\]]+)]]");

    private static class GlossaryRef {
        private final String chapterFile;
        private final String anchorId;

        GlossaryRef(String chapterFile, String anchorId) {
            this.chapterFile = chapterFile;
            this.anchorId = anchorId;
        }
    }

    public void export(Book book, File outputFile) throws Exception {
        nl.siegmann.epublib.domain.Book epub = new nl.siegmann.epublib.domain.Book();

        // ── Metadatos ──────────────────────────────────────────────
        Metadata meta = epub.getMetadata();
        meta.addTitle(book.getTitle().isBlank() ? "Sin título" : book.getTitle());
        meta.addAuthor(new Author(
                book.getAuthor().isBlank() ? "Desconocido" : book.getAuthor()
        ));
        meta.addDescription(book.getSynopsis());
        meta.setLanguage(book.getLanguage().isBlank() ? "es" : book.getLanguage());
        if (!book.getIllustrator().isBlank()) {
            meta.addContributor(new Author(book.getIllustrator()));
        }

        // ── CSS base ───────────────────────────────────────────────
        String css = """
                html, body {
                    margin: 0;
                    padding: 0;
                }
                body {
                    font-family: Georgia, 'Times New Roman', serif;
                    font-size: 1em;
                    line-height: 1.7;
                    margin: 1.5em 2em;
                    color: #1a1a1a;
                }
                h1 {
                    font-size: 1.6em;
                    margin-bottom: 0.8em;
                    border-bottom: 1px solid #ccc;
                    padding-bottom: 0.3em;
                }
                p  { margin: 0.6em 0; text-indent: 1.2em; }
                img { max-width: 100%; display: block; margin: 1.5em auto; }
                body.cover-page {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    line-height: 0;
                    background: #000;
                }
                .cover-page img {
                    display: block;
                    width: 100%;
                    height: 100%;
                    max-width: none;
                    max-height: none;
                    margin: 0;
                    object-fit: cover;
                }
                a  { color: #4a4ae8; text-decoration: none; }
                """;
        epub.addResource(new Resource(css.getBytes(StandardCharsets.UTF_8), "styles/main.css"));

        // ── Portada ────────────────────────────────────────────────
        if (!book.getCoverPath().isBlank()) {
            File coverFile = new File(book.getCoverPath());
            if (coverFile.exists()) {
                byte[] coverBytes = Files.readAllBytes(coverFile.toPath());
                String ext        = coverFile.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";
                String coverHref  = "images/cover." + ext;

                epub.setCoverImage(new Resource(coverBytes, coverHref));

                String coverHtml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
                        "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                        <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>Portada</title>
                            <link rel="stylesheet" type="text/css" href="styles/main.css"/>
                        </head>
                        <body class="cover-page">
                            <img src="%s" alt="Portada"/>
                        </body>
                        </html>
                        """.formatted(coverHref);

                epub.addSection("Portada",
                        new Resource(coverHtml.getBytes(StandardCharsets.UTF_8), "cover.html")
                );
            }
        }

        // ── Páginas preliminares ───────────────────────────────────
        for (int i = 0; i < book.getPreliminaryPages().size(); i++) {
            PreliminaryPage page  = book.getPreliminaryPages().get(i);
            String          pageId = "prelim_" + (i + 1);

            StringBuilder html = new StringBuilder();
            html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" ");
            html.append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
            html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
            html.append("<title>").append(escapeHtml(page.getTitle())).append("</title>\n");
            html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/main.css\"/>\n");
            html.append("</head>\n<body>\n");

            // Imagen
            if (page.getType() != PreliminaryPage.Type.TEXTO && !page.getImagePath().isBlank()) {
                File imgFile = new File(page.getImagePath());
                if (imgFile.exists()) {
                    byte[] imgBytes = Files.readAllBytes(imgFile.toPath());
                    String imgHref  = "images/" + pageId + "_" + imgFile.getName();
                    epub.addResource(new Resource(imgBytes, imgHref));
                    html.append("<img src=\"").append(imgHref)
                            .append("\" alt=\"").append(escapeHtml(imgFile.getName()))
                            .append("\"/>\n");
                }
            }

            // Texto
            if (page.getType() != PreliminaryPage.Type.IMAGEN && !page.getText().isBlank()) {
                for (String line : page.getText().split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty())
                        html.append("<p>").append(escapeHtml(trimmed)).append("</p>\n");
                }
            }

            html.append("</body>\n</html>");

            epub.addSection(
                    page.getTitle().isBlank() ? "Página " + (i + 1) : page.getTitle(),
                    new Resource(html.toString().getBytes(StandardCharsets.UTF_8), pageId + ".html")
            );
        }

        // ── Capítulos ──────────────────────────────────────────────
        Map<String, String> glossaryDefinitions = new LinkedHashMap<>();
        Map<String, List<GlossaryRef>> glossaryReferences = new LinkedHashMap<>();

        for (int i = 0; i < book.getChapters().size(); i++) {
            Chapter chapter   = book.getChapters().get(i);
            String  chapterId = "chapter_" + (i + 1);
            String  chapterFile = chapterId + ".html";

            StringBuilder html = new StringBuilder();
            html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" ");
            html.append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
            html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
            html.append("<title>").append(escapeHtml(chapter.getTitle())).append("</title>\n");
            html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/main.css\"/>\n");
            html.append("</head>\n<body>\n");
            html.append("<h1>").append(escapeHtml(chapter.getTitle())).append("</h1>\n");

            // Párrafos e imágenes inline entrelazados
            String body = chapter.getBody();
            if (body != null && !body.isBlank()) {
                int imgCounter = 0;
                for (String line : body.split("\n")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    if (trimmed.startsWith("[IMAGEN:") && trimmed.endsWith("]")) {
                        // Extraer path de la marca
                        String imgPath = trimmed.substring(8, trimmed.length() - 1);
                        File   imgFile = new File(imgPath);
                        if (!imgFile.exists()) continue;

                        byte[] imgBytes = Files.readAllBytes(imgFile.toPath());
                        String imgHref  = "images/" + chapterId + "_" + imgCounter + "_"
                                + imgFile.getName();
                        imgCounter++;
                        epub.addResource(new Resource(imgBytes, imgHref));
                        html.append("<img src=\"").append(imgHref)
                                .append("\" alt=\"").append(escapeHtml(imgFile.getName()))
                                .append("\"/>\n");
                    } else {
                        html.append("<p>")
                                .append(processGlossaryMarkup(trimmed, chapterFile, glossaryDefinitions, glossaryReferences))
                                .append("</p>\n");
                    }
                }
            }

            html.append("</body>\n</html>");

            epub.addSection(
                    chapter.getTitle().isBlank() ? "Capítulo " + (i + 1) : chapter.getTitle(),
                    new Resource(html.toString().getBytes(StandardCharsets.UTF_8), chapterId + ".html")
            );
        }

        if (!glossaryDefinitions.isEmpty()) {
            String glossaryHtml = buildGlossaryHtml(glossaryDefinitions, glossaryReferences);
            epub.addSection("Glosario",
                    new Resource(glossaryHtml.getBytes(StandardCharsets.UTF_8), "glossary.html"));
        }

        // ── Escribir archivo ───────────────────────────────────────
        try (OutputStream out = new FileOutputStream(outputFile)) {
            new EpubWriter().write(epub, out);
        }
    }

    private String processGlossaryMarkup(
            String text,
            String chapterFile,
            Map<String, String> glossaryDefinitions,
            Map<String, List<GlossaryRef>> glossaryReferences
    ) {
        Matcher matcher = GLOSS_PATTERN.matcher(text);
        StringBuilder out = new StringBuilder();
        int cursor = 0;

        while (matcher.find()) {
            out.append(escapeHtml(text.substring(cursor, matcher.start())));

            String term = matcher.group(1).trim();
            String definition = matcher.group(2).trim();
            String label = matcher.group(3).trim();

            if (term.isBlank()) {
                out.append(escapeHtml(label));
                cursor = matcher.end();
                continue;
            }

            if (label.isBlank()) label = term;
            if (definition.isBlank()) definition = "Sin definicion cargada.";

            glossaryDefinitions.putIfAbsent(term, definition);
            if ("Sin definicion cargada.".equals(glossaryDefinitions.get(term)) && !definition.isBlank()) {
                glossaryDefinitions.put(term, definition);
            }

            List<GlossaryRef> refs = glossaryReferences.computeIfAbsent(term, k -> new ArrayList<>());
            String slug = slugify(term);
            String anchorId = "ref-" + slug + "-" + (refs.size() + 1);
            refs.add(new GlossaryRef(chapterFile, anchorId));

            out.append("<a id=\"")
                    .append(anchorId)
                    .append("\" href=\"glossary.html#term-")
                    .append(slug)
                    .append("\">")
                    .append(escapeHtml(label))
                    .append("</a>");

            cursor = matcher.end();
        }

        out.append(escapeHtml(text.substring(cursor)));
        return out.toString();
    }

    private String buildGlossaryHtml(Map<String, String> definitions, Map<String, List<GlossaryRef>> references) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" ");
        html.append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
        html.append("<title>Glosario</title>\n");
        html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"styles/main.css\"/>\n");
        html.append("</head>\n<body>\n");
        html.append("<h1>Glosario</h1>\n");

        for (Map.Entry<String, String> entry : definitions.entrySet()) {
            String term = entry.getKey();
            String slug = slugify(term);

            html.append("<h2 id=\"term-").append(slug).append("\">")
                    .append(escapeHtml(term))
                    .append("</h2>\n");
            html.append("<p>").append(escapeHtml(entry.getValue())).append("</p>\n");

            List<GlossaryRef> refs = references.get(term);
            if (refs != null && !refs.isEmpty()) {
                html.append("<p>");
                for (int i = 0; i < refs.size(); i++) {
                    GlossaryRef ref = refs.get(i);
                    if (i > 0) html.append(" | ");
                    html.append("<a href=\"")
                            .append(ref.chapterFile)
                            .append("#")
                            .append(ref.anchorId)
                            .append("\">Volver al contexto")
                            .append("</a>");
                }
                html.append("</p>\n");
            }
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    private String slugify(String text) {
        String slug = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.isBlank() ? "termino" : slug;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}