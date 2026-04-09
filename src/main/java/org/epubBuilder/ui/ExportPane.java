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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportPane extends VBox {

    private static final Pattern IMAGE_MARKER = Pattern.compile("\\[IMAGEN:([^\\]]+)]");
    private static final Pattern GLOSS_MARKER = Pattern.compile("\\[\\[GLOSS:([^|\\]]+)\\|([^|\\]]*)\\|([^\\]]+)]]");

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
        ValidationResult validation = validateBeforeExport();
        if (!validation.errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se puede exportar");
            alert.setHeaderText("Hay problemas que debes corregir");
            alert.setContentText(String.join("\n", validation.errors));
            alert.showAndWait();
            setStatus("Corrige los errores antes de exportar.", "#e05c5c");
            return;
        }

        if (!validation.warnings.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    String.join("\n", validation.warnings),
                    ButtonType.YES,
                    ButtonType.NO);
            alert.setTitle("Advertencias de exportacion");
            alert.setHeaderText("Puedes continuar, pero revisa estos puntos");
            ButtonType decision = alert.showAndWait().orElse(ButtonType.NO);
            if (decision != ButtonType.YES) {
                setStatus("Exportacion cancelada.", "#d08a2e");
                return;
            }
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

    private ValidationResult validateBeforeExport() {
        ValidationResult result = new ValidationResult();

        if (book.getTitle().isBlank()) {
            result.errors.add("- Falta el titulo del libro.");
        }
        if (book.getChapters().isEmpty()) {
            result.errors.add("- Debes agregar al menos un capitulo.");
        }

        if (book.getAuthor().isBlank()) {
            result.warnings.add("- El autor esta vacio.");
        }
        if (!book.getCoverPath().isBlank()) {
            File coverFile = new File(book.getCoverPath());
            if (!coverFile.exists()) {
                result.warnings.add("- La portada configurada no existe en disco.");
            }
        }

        Map<String, String> glossaryDefinitions = new HashMap<>();

        for (int i = 0; i < book.getChapters().size(); i++) {
            int chapterNumber = i + 1;
            String chapterLabel = "Capitulo " + chapterNumber;
            String chapterTitle = book.getChapters().get(i).getTitle();
            String body = book.getChapters().get(i).getBody();

            if (chapterTitle == null || chapterTitle.isBlank()) {
                result.warnings.add("- " + chapterLabel + " no tiene titulo.");
            }
            if (body == null || body.isBlank()) {
                result.warnings.add("- " + chapterLabel + " no tiene contenido.");
                continue;
            }

            Matcher imageMatcher = IMAGE_MARKER.matcher(body);
            while (imageMatcher.find()) {
                String imagePath = imageMatcher.group(1).trim();
                if (imagePath.isBlank()) continue;
                if (!new File(imagePath).exists()) {
                    result.warnings.add("- Imagen faltante en " + chapterLabel + ": " + imagePath);
                }
            }

            Matcher glossMatcher = GLOSS_MARKER.matcher(body);
            while (glossMatcher.find()) {
                String term = glossMatcher.group(1).trim();
                String definition = glossMatcher.group(2).trim();

                if (term.isBlank()) {
                    result.warnings.add("- Referencia de glosario sin termino en " + chapterLabel + ".");
                    continue;
                }
                if (definition.isBlank()) {
                    result.warnings.add("- Termino sin definicion (" + term + ") en " + chapterLabel + ".");
                }

                String normalized = term.toLowerCase();
                String knownDefinition = glossaryDefinitions.get(normalized);
                if (knownDefinition == null || knownDefinition.isBlank()) {
                    glossaryDefinitions.put(normalized, definition);
                } else if (!definition.isBlank() && !knownDefinition.equals(definition)) {
                    result.warnings.add("- Definiciones distintas para el termino \"" + term + "\".");
                }
            }
        }

        return result;
    }

    private String orBlank(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
    }
}