package org.epubBuilder.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class RichTextEditor extends VBox {

    private final WebView webView = new WebView();
    private final WebEngine engine;
    private final HBox toolbar = new HBox(4);

    private final Stage stage;
    private final Runnable onImageInsert;
    private final Runnable onGlossaryInsert;

    public RichTextEditor(Stage stage, Runnable onImageInsert, Runnable onGlossaryInsert) {
        this.stage = stage;
        this.onImageInsert = onImageInsert;
        this.onGlossaryInsert = onGlossaryInsert;
        this.engine = webView.getEngine();

        buildUI();
        initEditor();
    }

    private void buildUI() {
        getStyleClass().add("rich-editor-container");

        buildToolbar();

        webView.setPrefHeight(400);
        webView.getStyleClass().add("custom-html-editor");
        VBox.setVgrow(webView, Priority.ALWAYS);

        getChildren().addAll(toolbar, webView);
    }

    private void initEditor() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                body {
                    font-family: Georgia, 'Times New Roman', serif;
                    font-size: 14px;
                    line-height: 1.7;
                    margin: 1em;
                    color: #1a1a1a;
                    outline: none;
                    min-height: 200px;
                }
                p { margin: 0.5em 0; }
                h1 { font-size: 1.8em; margin: 0.6em 0; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }
                h2 { font-size: 1.5em; margin: 0.5em 0; }
                h3 { font-size: 1.3em; margin: 0.4em 0; }
                h4 { font-size: 1.1em; margin: 0.3em 0; }
                ul, ol { margin: 0.5em 0; padding-left: 2em; }
                li { margin: 0.2em 0; }
                img { max-width: 100%%; display: block; margin: 1em auto; }
                blockquote { border-left: 3px solid #ccc; margin: 0.5em 0; padding-left: 1em; color: #666; }
                div.epub-image-marker {
                    background: #f0f4f8;
                    border: 2px dashed #8ab4f8;
                    padding: 8px 12px;
                    margin: 8px 0;
                    border-radius: 6px;
                    color: #4a6fa5;
                    font-size: 13px;
                }
                </style>
                </head>
                <body contenteditable="true">
                <p><br></p>
                </body>
                </html>
                """;
        engine.loadContent(html, "text/html");
    }

    private void buildToolbar() {
        toolbar.getStyleClass().add("editor-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnBold = createToolbarButton("B", "Negrita (Ctrl+B)", () -> execCmd("bold"));
        btnBold.getStyleClass().addAll("toolbar-format-button", "bold-button");

        Button btnItalic = createToolbarButton("I", "Cursiva (Ctrl+I)", () -> execCmd("italic"));
        btnItalic.getStyleClass().addAll("toolbar-format-button", "italic-button");

        Button btnUnderline = createToolbarButton("U", "Subrayado (Ctrl+U)", () -> execCmd("underline"));
        btnUnderline.getStyleClass().addAll("toolbar-format-button", "underline-button");

        Button btnStrike = createToolbarButton("S", "Tachado", () -> execCmd("strikethrough"));
        btnStrike.getStyleClass().addAll("toolbar-format-button", "strike-button");

        Separator sep1 = new Separator();
        sep1.setPrefHeight(24);

        Button btnAlignLeft = createToolbarButton("\u21E4", "Alinear izquierda", () -> execCmd("justifyLeft"));
        Button btnAlignCenter = createToolbarButton("\u21E5", "Centrar", () -> execCmd("justifyCenter"));
        Button btnAlignRight = createToolbarButton("\u21E6", "Alinear derecha", () -> execCmd("justifyRight"));
        Button btnAlignJustify = createToolbarButton("\u21F2", "Justificar", () -> execCmd("justifyFull"));

        Separator sep2 = new Separator();
        sep2.setPrefHeight(24);

        ComboBox<String> headingBox = new ComboBox<>();
        headingBox.getItems().addAll("Normal", "T\u00EDtulo", "Encabezado 1", "Encabezado 2", "Encabezado 3");
        headingBox.setValue("Normal");
        headingBox.getStyleClass().add("toolbar-combo-box");
        headingBox.setPrefWidth(130);
        headingBox.setOnAction(e -> applyHeading(headingBox.getValue()));

        Separator sep3 = new Separator();
        sep3.setPrefHeight(24);

        Button btnBulletList = createToolbarButton("\u2022 Lista", "Lista con vi\u00F1etas", () -> execCmd("insertUnorderedList"));
        Button btnNumberedList = createToolbarButton("1. Lista", "Lista numerada", () -> execCmd("insertOrderedList"));

        Separator sep4 = new Separator();
        sep4.setPrefHeight(24);

        Button btnInsertImg = createToolbarButton("\uD83D\uDDBC Imagen", "Insertar imagen", () -> {
            if (onImageInsert != null) onImageInsert.run();
        });
        btnInsertImg.getStyleClass().add("toolbar-action-button");

        Button btnInsertGlossary = createToolbarButton("\uD83D\uDCD6 Glosario", "Insertar referencia de glosario", () -> {
            if (onGlossaryInsert != null) onGlossaryInsert.run();
        });
        btnInsertGlossary.getStyleClass().add("toolbar-action-button");

        Separator sep5 = new Separator();
        sep5.setPrefHeight(24);

        Button btnClearFormat = createToolbarButton("\u232B Limpiar", "Limpiar formato", () -> execCmd("removeFormat"));
        btnClearFormat.getStyleClass().add("toolbar-danger-button");

        toolbar.getChildren().addAll(
                btnBold, btnItalic, btnUnderline, btnStrike,
                sep1,
                btnAlignLeft, btnAlignCenter, btnAlignRight, btnAlignJustify,
                sep2,
                headingBox,
                sep3,
                btnBulletList, btnNumberedList,
                sep4,
                btnInsertImg, btnInsertGlossary,
                sep5,
                btnClearFormat
        );
    }

    private Button createToolbarButton(String text, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void execCmd(String command) {
        engine.executeScript("document.execCommand('" + command + "', false, null);");
    }

    private void applyHeading(String type) {
        String tag;
        switch (type) {
            case "Título": tag = "h1"; break;
            case "Encabezado 1": tag = "h2"; break;
            case "Encabezado 2": tag = "h3"; break;
            case "Encabezado 3": tag = "h4"; break;
            default: tag = "p"; break;
        }
        engine.executeScript("document.execCommand('formatBlock', false, '<" + tag + ">');");
    }

    public void setHtmlContent(String html) {
        if (html == null || html.isBlank()) {
            html = "<p><br></p>";
        }
        String safeHtml = html.replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
        engine.executeScript("document.body.innerHTML = '" + safeHtml + "';");
    }

    public String getHtmlContent() {
        Object result = engine.executeScript("document.body.innerHTML");
        if (result == null) return "";
        String content = result.toString();
        if (content == null || content.isBlank() || content.equals("<br>")) return "";
        return content;
    }

    public void requestEditorFocus() {
        webView.requestFocus();
    }

    public WebEngine getWebViewEngine() {
        return engine;
    }

    public void setEditorEnabled(boolean enabled) {
        webView.setDisable(!enabled);
        toolbar.setDisable(!enabled);
    }

    public void insertImageMarker(String imagePath) {
        String safePath = escapeHtmlAttr(imagePath);
        String safeLabel = escapeHtml(imagePath);
        String marker = "<div class='epub-image-marker' contenteditable='false' data-path='" + safePath + "'>📷 [IMAGEN:" + safeLabel + "]</div><p><br></p>";
        String safeMarker = marker.replace("'", "\\'");
        engine.executeScript("document.execCommand('insertHTML', false, '" + safeMarker + "');");
    }

    public void insertGlossaryMarker(String term, String definition, String label) {
        String marker = "[[GLOSS:" + sanitizeGlossaryPart(term) + "|" + sanitizeGlossaryPart(definition) + "|" + sanitizeGlossaryPart(label) + "]]";
        String safeMarker = marker.replace("'", "\\'");
        engine.executeScript("document.execCommand('insertHTML', false, '" + safeMarker + "');");
    }

    public String getPlainText() {
        Object result = engine.executeScript("document.body.innerText;");
        if (result == null) return "";
        return result.toString().trim();
    }

    public java.util.List<GlossaryTokenInfo> findGlossaryTokens() {
        java.util.List<GlossaryTokenInfo> tokens = new java.util.ArrayList<>();
        String html = getHtmlContent();
        if (html.isBlank()) return tokens;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[\\[GLOSS:([^|\\]]+)\\|([^|\\]]*)\\|([^\\]]+)]]");
        java.util.regex.Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            tokens.add(new GlossaryTokenInfo(
                    matcher.start(),
                    matcher.end(),
                    matcher.group(1).trim(),
                    matcher.group(2).trim(),
                    matcher.group(3).trim()
            ));
        }
        return tokens;
    }

    public static class GlossaryTokenInfo {
        public final int start;
        public final int end;
        public final String term;
        public final String definition;
        public final String label;

        public GlossaryTokenInfo(int start, int end, String term, String definition, String label) {
            this.start = start;
            this.end = end;
            this.term = term;
            this.definition = definition;
            this.label = label;
        }
    }

    public java.util.List<String> findImagePaths() {
        java.util.List<String> paths = new java.util.ArrayList<>();
        String html = getHtmlContent();
        if (html.isBlank()) return paths;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("data-path=['\"]([^'\"]+)['\"]");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }

        java.util.regex.Pattern oldPattern = java.util.regex.Pattern.compile("\\[IMAGEN:([^\\]]+)]");
        java.util.regex.Matcher oldMatcher = oldPattern.matcher(html);
        while (oldMatcher.find()) {
            paths.add(oldMatcher.group(1));
        }

        return paths;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeHtmlAttr(String text) {
        return escapeHtml(text).replace("'", "&#39;");
    }

    private String sanitizeGlossaryPart(String input) {
        if (input == null) return "";
        return input.replace("|", "/").replace("]]", "] ]").trim();
    }
}
