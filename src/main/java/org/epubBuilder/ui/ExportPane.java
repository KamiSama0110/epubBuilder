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

    private final Book book;
    private final Stage stage;
    private final Label lTitle = new Label();
    private final Label lAuthor = new Label();
    private final Label lLang = new Label();
    private final Label lChapters = new Label();
    private final Label status = new Label("");

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
        setSpacing(26);
        setPadding(new Insets(40));
        getStyleClass().add("pane-bg");

        Label title = new Label("Exportar EPUB");
        title.getStyleClass().add("heading-label");

        Label subtitle = new Label("Revisa el resumen y genera tu archivo.");
        subtitle.getStyleClass().add("title-label");

        VBox header = new VBox(4, title, subtitle);
        header.setAlignment(Pos.CENTER);

        VBox card = new VBox(0);
        card.setMaxWidth(520);
        card.getStyleClass().add("export-card");

        card.getChildren().addAll(
            summaryRow("Titulo", lTitle),
                divider(),
            summaryRow("Autor", lAuthor),
                divider(),
            summaryRow("Idioma", lLang),
                divider(),
            summaryRow("Capitulos", lChapters)
        );

        Button btnExport = new Button("Exportar EPUB");
        btnExport.getStyleClass().add("primary-button");
        btnExport.setPrefWidth(230);
        btnExport.setOnAction(e -> doExport());

        status.setWrapText(true);
        status.setMaxWidth(520);
        status.getStyleClass().add("status-muted");

        getChildren().addAll(header, card, btnExport, status);
        refresh();
    }

    private HBox summaryRow(String key, Label valueLabel) {
        Label keyLbl = new Label(key);
        keyLbl.setMinWidth(118);
        keyLbl.getStyleClass().add("summary-key");

        valueLabel.getStyleClass().add("summary-value");

        HBox row = new HBox(16, keyLbl, valueLabel);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Separator divider() {
        return new Separator();
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
        String safeName = book.getTitle().replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (safeName.isBlank()) safeName = "libro";
        fc.setInitialFileName(
            safeName + ".epub"
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
        status.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 700;");
    }

    private String orBlank(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}