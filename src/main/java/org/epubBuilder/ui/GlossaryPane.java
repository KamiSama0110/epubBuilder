package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.Chapter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossaryPane extends BorderPane {

    private static final Pattern GLOSS_PATTERN = Pattern.compile("\\[\\[GLOSS:([^|\\]]+)\\|([^|\\]]*)\\|([^\\]]+)]]");

    private final Book book;
    private final ObservableList<GlossaryEntry> allEntries = FXCollections.observableArrayList();
    private final ObservableList<GlossaryEntry> visibleEntries = FXCollections.observableArrayList();

    private final TextField searchField = new TextField();
    private final ListView<GlossaryEntry> termsList = new ListView<>(visibleEntries);
    private final TextField termField = new TextField();
    private final TextArea definitionArea = new TextArea();
    private final Label usageLabel = new Label("Usos: 0");
    private final Label statusLabel = new Label("Selecciona un termino para editar.");

    public GlossaryPane(Book book) {
        this.book = book;
        buildUI();
        refresh();
    }

    private void buildUI() {
        getStyleClass().add("pane-bg");
        setPadding(new Insets(24));

        VBox left = new VBox(10);
        left.setPrefWidth(320);
        left.setMinWidth(260);
        left.getStyleClass().add("card");

        Label leftTitle = new Label("Terminos del glosario");
        leftTitle.getStyleClass().add("section-title");

        searchField.setPromptText("Buscar termino...");
        searchField.getStyleClass().add("custom-text-field");
        searchField.textProperty().addListener((o, old, val) -> applyFilter());

        termsList.getStyleClass().addAll("list-view", "entity-list");
        VBox.setVgrow(termsList, Priority.ALWAYS);
        termsList.setPlaceholder(new Label("No hay terminos todavia"));
        termsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GlossaryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.term + " (" + item.usages + ")");
            }
        });

        termsList.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> loadEntry(val));

        left.getChildren().addAll(leftTitle, searchField, termsList);

        VBox right = new VBox(10);
        right.getStyleClass().add("card");
        HBox.setHgrow(right, Priority.ALWAYS);

        Label rightTitle = new Label("Editor global");
        rightTitle.getStyleClass().add("section-title");

        Label termLabel = new Label("TERMINO");
        termLabel.getStyleClass().add("field-label");
        termField.getStyleClass().add("custom-text-field");

        Label defLabel = new Label("DEFINICION");
        defLabel.getStyleClass().add("field-label");
        definitionArea.getStyleClass().add("custom-text-area");
        definitionArea.setWrapText(true);
        definitionArea.setPrefRowCount(8);

        usageLabel.getStyleClass().add("hint-label");
        statusLabel.getStyleClass().add("hint-label");

        Button btnSave = new Button("Guardar cambios");
        btnSave.getStyleClass().add("primary-button");
        btnSave.setOnAction(e -> saveEntryChanges());

        Button btnUnlink = new Button("Desvincular termino");
        btnUnlink.getStyleClass().add("danger-button");
        btnUnlink.setOnAction(e -> unlinkSelectedTerm());

        Button btnRefresh = new Button("Actualizar");
        btnRefresh.getStyleClass().add("action-button");
        btnRefresh.setOnAction(e -> refresh());

        HBox actions = new HBox(10, btnSave, btnUnlink, btnRefresh);
        actions.setAlignment(Pos.CENTER_LEFT);

        right.getChildren().addAll(
                rightTitle,
                termLabel,
                termField,
                defLabel,
                definitionArea,
                usageLabel,
                actions,
                statusLabel
        );

        HBox content = new HBox(18, left, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        setCenter(content);
    }

    public void refresh() {
        GlossaryEntry selected = termsList.getSelectionModel().getSelectedItem();
        String selectedNorm = selected == null ? "" : selected.normalized;

        Map<String, GlossaryEntry> map = new LinkedHashMap<>();
        for (Chapter chapter : book.getChapters()) {
            String body = chapter.getBody();
            if (body == null || body.isBlank()) continue;

            Matcher matcher = GLOSS_PATTERN.matcher(body);
            while (matcher.find()) {
                String term = matcher.group(1).trim();
                String definition = matcher.group(2).trim();
                if (term.isBlank()) continue;

                String normalized = normalize(term);
                GlossaryEntry entry = map.get(normalized);
                if (entry == null) {
                    entry = new GlossaryEntry(term, definition, normalized);
                    map.put(normalized, entry);
                }
                if (entry.definition.isBlank() && !definition.isBlank()) {
                    entry.definition = definition;
                }
                entry.usages++;
            }
        }

        allEntries.setAll(map.values());
        applyFilter();

        if (selectedNorm.isBlank()) {
            if (!visibleEntries.isEmpty()) {
                termsList.getSelectionModel().select(0);
            } else {
                loadEntry(null);
            }
            return;
        }

        GlossaryEntry restored = null;
        for (GlossaryEntry entry : visibleEntries) {
            if (entry.normalized.equals(selectedNorm)) {
                restored = entry;
                break;
            }
        }

        if (restored != null) {
            termsList.getSelectionModel().select(restored);
        } else if (!visibleEntries.isEmpty()) {
            termsList.getSelectionModel().select(0);
        } else {
            loadEntry(null);
        }
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        visibleEntries.clear();

        for (GlossaryEntry entry : allEntries) {
            if (query.isBlank() || entry.term.toLowerCase().contains(query)) {
                visibleEntries.add(entry);
            }
        }
    }

    private void loadEntry(GlossaryEntry entry) {
        if (entry == null) {
            termField.clear();
            definitionArea.clear();
            usageLabel.setText("Usos: 0");
            statusLabel.setText("Selecciona un termino para editar.");
            return;
        }

        termField.setText(entry.term);
        definitionArea.setText(entry.definition);
        usageLabel.setText("Usos: " + entry.usages);
        statusLabel.setText("Edita termino o definicion y guarda.");
    }

    private void saveEntryChanges() {
        GlossaryEntry selected = termsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Primero selecciona un termino.");
            return;
        }

        String newTerm = termField.getText() == null ? "" : termField.getText().trim();
        String newDefinition = definitionArea.getText() == null ? "" : definitionArea.getText().trim();

        if (newTerm.isBlank()) {
            statusLabel.setText("El termino no puede estar vacio.");
            return;
        }
        if (newDefinition.isBlank()) {
            statusLabel.setText("La definicion no puede estar vacia.");
            return;
        }

        int touched = rewriteAcrossChapters(selected.normalized, token -> {
            if (!normalize(token.term).equals(selected.normalized)) return token.original;
            return tokenMarkup(newTerm, newDefinition, token.label);
        });

        statusLabel.setText(touched > 0
                ? "Actualizado en " + touched + " capitulo(s)."
                : "No se encontraron ocurrencias para actualizar.");

        refresh();
        reselectTerm(newTerm);
    }

    private void unlinkSelectedTerm() {
        GlossaryEntry selected = termsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Primero selecciona un termino.");
            return;
        }

        int touched = rewriteAcrossChapters(selected.normalized, token -> {
            if (!normalize(token.term).equals(selected.normalized)) return token.original;
            return token.label;
        });

        statusLabel.setText(touched > 0
                ? "Termino desvinculado en " + touched + " capitulo(s)."
                : "No se encontraron ocurrencias para desvincular.");

        refresh();
    }

    private int rewriteAcrossChapters(String normalizedTerm, Function<GlossaryToken, String> replacer) {
        int touched = 0;

        for (Chapter chapter : book.getChapters()) {
            String body = chapter.getBody();
            if (body == null || body.isBlank()) continue;

            String updated = rewriteGlossaryTokens(body, normalizedTerm, replacer);
            if (!updated.equals(body)) {
                chapter.setBody(updated);
                touched++;
            }
        }

        return touched;
    }

    private String rewriteGlossaryTokens(String text, String normalizedTerm, Function<GlossaryToken, String> replacer) {
        Matcher matcher = GLOSS_PATTERN.matcher(text);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean changed = false;

        while (matcher.find()) {
            out.append(text, cursor, matcher.start());

            GlossaryToken token = new GlossaryToken(
                    matcher.group(1).trim(),
                    matcher.group(3).trim(),
                    matcher.group(0)
            );

            String replacement = token.original;
            if (normalize(token.term).equals(normalizedTerm)) {
                replacement = replacer.apply(token);
                changed = true;
            }

            out.append(replacement);
            cursor = matcher.end();
        }

        if (!changed) return text;

        out.append(text.substring(cursor));
        return out.toString();
    }

    private void reselectTerm(String term) {
        String normalized = normalize(term);
        for (GlossaryEntry entry : visibleEntries) {
            if (entry.normalized.equals(normalized)) {
                termsList.getSelectionModel().select(entry);
                return;
            }
        }
    }

    private String tokenMarkup(String term, String definition, String label) {
        return "[[GLOSS:" + sanitize(term) + "|" + sanitize(definition) + "|" + sanitize(label) + "]]";
    }

    private String sanitize(String value) {
        return value.replace("|", "/").replace("]]", "] ]").trim();
    }

    private String normalize(String term) {
        return term.trim().toLowerCase();
    }

    private static class GlossaryEntry {
        private final String normalized;
        private String term;
        private String definition;
        private int usages;

        private GlossaryEntry(String term, String definition, String normalized) {
            this.term = term;
            this.definition = definition;
            this.normalized = normalized;
            this.usages = 0;
        }
    }

    private static class GlossaryToken {
        private final String term;
        private final String label;
        private final String original;

        private GlossaryToken(String term, String label, String original) {
            this.term = term;
            this.label = label;
            this.original = original;
        }
    }
}