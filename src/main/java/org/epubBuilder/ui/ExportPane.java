package org.epubBuilder.ui;

import org.epubBuilder.EpubExporter;
import org.epubBuilder.model.Book;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ExportPane extends VBox {

    private final Book  book;
    private final Stage stage;
    private final Label lTitle    = new Label();
    private final Label lAuthor   = new Label();
    private final Label lLang     = new Label();
    private final Label lChapters = new Label();
    private final Label status    = new Label("");

    public ExportPane(Book book, Stage stage) {
        this.book  = book;
        this.stage = stage;
        buildUI();
    }

    public void refresh() {
        lTitle.setText(orBlank(book.getTitle()));
        lAuthor.setText(orBlank(book.getAuthor()));
        lLang.setText(orBlank(book.getLanguage()));
        lChapters.setText(book.getChapters().size() + " capítulos");
    }

    private void buildUI() {
        setAlignment(Pos.CENTER);
        setSpacing(32);
        setPadding(new Insets(64));
        setStyle("-fx-background-color: #0f0f13;");

        // ── Encabezado ─────────────────────────────────────────────
        Label eyebrow = new Label("LISTO PARA PUBLICAR");
        eyebrow.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #5e5ce6;
                -fx-letter-spacing: 2px;
                """);

        Label title = new Label("Exportar EPUB");
        title.setStyle("""
                -fx-font-size: 30px;
                -fx-font-weight: bold;
                -fx-text-fill: #f0f0f5;
                """);

        Label subtitle = new Label("Revisa el resumen y genera tu archivo.");
        subtitle.setStyle("-fx-text-fill: #555566; -fx-font-size: 14px;");

        VBox header = new VBox(6, eyebrow, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // ── Tarjeta resumen ────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(400);
        card.setStyle("""
                -fx-background-color: #18181f;
                -fx-border-color: #2a2a38;
                -fx-border-width: 1;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);

        card.getChildren().addAll(
                summaryRow("Título",     lTitle),
                divider(),
                summaryRow("Autor",      lAuthor),
                divider(),
                summaryRow("Idioma",     lLang),
                divider(),
                summaryRow("Capítulos",  lChapters)
        );

        // ── Botón exportar ─────────────────────────────────────────
        Button btnExport = new Button("Exportar EPUB");
        btnExport.setPrefWidth(220);
        btnExport.setStyle("""
                -fx-background-color: #5e5ce6;
                -fx-text-fill: #ffffff;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-background-radius: 6;
                -fx-padding: 14 0 14 0;
                -fx-cursor: hand;
                """);
        btnExport.setOnAction(e -> doExport());

        status.setWrapText(true);
        status.setMaxWidth(400);
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #555566;");

        getChildren().addAll(header, card, btnExport, status);
        refresh();
    }

    private HBox summaryRow(String key, Label valueLabel) {
        Label keyLbl = new Label(key);
        keyLbl.setMinWidth(100);
        keyLbl.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #555566;
                """);

        valueLabel.setStyle("""
                -fx-font-size: 13px;
                -fx-text-fill: #c0c0d0;
                """);

        HBox row = new HBox(16, keyLbl, valueLabel);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Separator divider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a2a38;");
        return sep;
    }

    private void doExport() {
        if (book.getTitle().isBlank()) {
            setStatus("El libro necesita un título.", "#e05c5c"); return;
        }
        if (book.getChapters().isEmpty()) {
            setStatus("Agrega al menos un capítulo.", "#e05c5c"); return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar EPUB");
        fc.setInitialFileName(
                book.getTitle().replaceAll("[^a-zA-Z0-9 ]", "").trim() + ".epub"
        );
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo EPUB", "*.epub")
        );

        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try {
            new EpubExporter().export(book, file);
            setStatus("✓ Exportado en " + file.getAbsolutePath(), "#5e5ce6");
        } catch (Exception ex) {
            setStatus("Error: " + ex.getMessage(), "#e05c5c");
            ex.printStackTrace();
        }
    }

    private void setStatus(String msg, String color) {
        status.setText(msg);
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    private String orBlank(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}