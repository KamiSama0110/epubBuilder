package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.Chapter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChaptersPane extends HBox {

    private static final Pattern GLOSS_PATTERN = Pattern.compile("\\[\\[GLOSS:([^|\\]]+)\\|([^|\\]]*)\\|([^\\]]+)]]");

    private final Book book;
    private final Stage stage;
    private final ObservableList<Chapter> chapters = FXCollections.observableArrayList();
    private final ListView<Chapter> listView = new ListView<>(chapters);

    private final Label editorTitle = new Label();
    private final TextField titleField = new TextField();
    private final RichTextEditor richEditor;
    private final ObservableList<GlossaryToken> glossaryTokens = FXCollections.observableArrayList();
    private final ListView<GlossaryToken> glossaryList = new ListView<>(glossaryTokens);
    private final VBox editorPane = new VBox();

    private Chapter currentChapter;
    private boolean loading;

    public ChaptersPane(Book book, Stage stage) {
        this.book = book;
        this.stage = stage;
        this.chapters.setAll(book.getChapters());
        this.richEditor = new RichTextEditor(stage, this::insertImageAtCursor, this::insertGlossaryReference);
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("pane-bg");

        VBox sidebar = buildSidebar();
        VBox editor = buildEditor();
        getChildren().addAll(sidebar, editor);
        HBox.setHgrow(editor, Priority.ALWAYS);

        listView.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> loadChapter(val));

        titleField.textProperty().addListener((o, old, val) -> {
            if (loading || currentChapter == null) return;
            currentChapter.setTitle(val);
            updateEditorTitle(val);
            listView.refresh();
        });

        // Escuchar cambios en el contenido HTML del editor
        // Como HTMLEditor no tiene listener directo, usamos un listener en focus lost
        richEditor.focusedProperty().addListener((o, old, val) -> {
            if (!val && currentChapter != null) {
                currentChapter.setBody(richEditor.getHtmlContent());
                refreshGlossaryList();
            }
        });

        clearEditor();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(220);
        sidebar.setPadding(new Insets(24, 14, 24, 14));
        sidebar.getStyleClass().add("sidebar-bg");

        Label label = new Label("CAPITULOS");
        label.getStyleClass().add("field-label");

        Label hint = new Label("Crea, reordena y edita tus capitulos.");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint-label");

        Button btnAdd = new Button("+");
        btnAdd.getStyleClass().add("toolbar-button");
        btnAdd.setTooltip(new Tooltip("Crear capitulo"));
        btnAdd.setPrefSize(36, 36);
        btnAdd.setOnAction(e -> addChapter());

        Button btnDelete = new Button("x");
        btnDelete.getStyleClass().add("toolbar-danger-button");
        btnDelete.setTooltip(new Tooltip("Eliminar capitulo seleccionado"));
        btnDelete.setPrefSize(36, 36);
        btnDelete.setOnAction(e -> deleteChapter());

        HBox actions = new HBox(8, btnAdd, btnDelete);
        actions.setAlignment(Pos.CENTER);

        listView.getStyleClass().addAll("list-view", "entity-list");
        listView.setCellFactory(lv -> new ChapterCell());
        VBox.setVgrow(listView, Priority.ALWAYS);

        sidebar.getChildren().addAll(label, hint, actions, listView);
        return sidebar;
    }

    private VBox buildEditor() {
        editorPane.setPadding(new Insets(28, 32, 28, 32));
        editorPane.getStyleClass().add("pane-bg");

        editorTitle.getStyleClass().add("empty-state-label");

        Label titleLabel = new Label("TITULO");
        titleLabel.getStyleClass().add("field-label");

        titleField.getStyleClass().add("custom-text-field");

        Label bodyLabel = new Label("CONTENIDO");
        bodyLabel.getStyleClass().add("field-label");

        Label glossaryLabel = new Label("REFERENCIAS DE GLOSARIO");
        glossaryLabel.getStyleClass().add("field-label");

        glossaryList.getStyleClass().addAll("list-view", "entity-list", "glossary-list");
        glossaryList.setPlaceholder(new Label("Sin referencias"));
        glossaryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GlossaryToken item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.label + " -> " + item.term);
            }
        });
        glossaryList.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> {
            if (val == null) return;
            richEditor.requestEditorFocus();
        });

        Button btnEditRef = new Button("Editar");
        btnEditRef.getStyleClass().add("action-button");
        btnEditRef.setOnAction(e -> editSelectedGlossaryReference());

        Button btnRemoveRef = new Button("Quitar");
        btnRemoveRef.getStyleClass().add("danger-button");
        btnRemoveRef.setOnAction(e -> removeSelectedGlossaryReference());

        HBox glossaryActions = new HBox(10, btnEditRef, btnRemoveRef);
        glossaryActions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10,
                editorTitle,
                titleLabel,
                titleField,
                bodyLabel,
                richEditor,
                glossaryLabel,
                glossaryList,
                glossaryActions
        );
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        VBox.setVgrow(glossaryList, Priority.NEVER);
        glossaryList.setPrefHeight(120);

        editorPane.getChildren().add(card);
        return editorPane;
    }

    private void addChapter() {
        Chapter chapter = new Chapter("Capitulo " + (chapters.size() + 1));
        chapters.add(chapter);
        syncBookChapters();
        Platform.runLater(() -> {
            listView.getSelectionModel().select(chapter);
            listView.scrollTo(chapter);
        });
    }

    private void deleteChapter() {
        Chapter selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int removedIndex = listView.getSelectionModel().getSelectedIndex();

        String name = selected.getTitle().isBlank() ? "(sin titulo)" : selected.getTitle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + name + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                chapters.remove(selected);
                syncBookChapters();

                if (chapters.isEmpty()) {
                    listView.getSelectionModel().clearSelection();
                    clearEditor();
                    return;
                }

                int nextIndex = Math.min(removedIndex, chapters.size() - 1);
                Platform.runLater(() -> {
                    listView.getSelectionModel().select(nextIndex);
                    listView.scrollTo(nextIndex);
                    loadChapter(chapters.get(nextIndex));
                });
            }
        });
    }

    private void loadChapter(Chapter chapter) {
        loading = true;
        currentChapter = null;

        if (chapter == null) {
            clearEditor();
            loading = false;
            return;
        }

        titleField.setDisable(false);
        richEditor.setEditorEnabled(true);

        titleField.setText(chapter.getTitle());
        richEditor.setHtmlContent(chapter.getBody());
        updateEditorTitle(chapter.getTitle());
        refreshGlossaryList();

        currentChapter = chapter;
        loading = false;
    }

    private void clearEditor() {
        currentChapter = null;
        updateEditorTitle("");
        titleField.clear();
        titleField.setDisable(true);
        richEditor.setEditorEnabled(false);
        richEditor.setHtmlContent("");
        glossaryTokens.clear();
    }

    private void updateEditorTitle(String value) {
        boolean empty = value == null || value.isBlank();
        editorTitle.setText(empty ? "Ningun capitulo seleccionado" : value);
        editorTitle.getStyleClass().removeAll("editor-title", "empty-state-label");
        editorTitle.getStyleClass().add(empty ? "empty-state-label" : "editor-title");
    }

    private void syncBookChapters() {
        book.getChapters().clear();
        book.getChapters().addAll(chapters);
    }

    public void reloadFromBook() {
        Chapter selected = listView.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? "" : selected.getId();

        chapters.setAll(book.getChapters());
        listView.refresh();

        if (chapters.isEmpty()) {
            listView.getSelectionModel().clearSelection();
            clearEditor();
            return;
        }

        Chapter toSelect = chapters.get(0);
        if (!selectedId.isBlank()) {
            for (Chapter chapter : chapters) {
                if (chapter.getId().equals(selectedId)) {
                    toSelect = chapter;
                    break;
                }
            }
        }

        listView.getSelectionModel().select(toSelect);
        loadChapter(toSelect);
    }

    private void insertImageAtCursor() {
        if (currentChapter == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        richEditor.insertImageMarker(file.getAbsolutePath());
        // Guardar inmediatamente
        currentChapter.setBody(richEditor.getHtmlContent());
    }

    private void insertGlossaryReference() {
        if (currentChapter == null) return;

        // Obtener texto seleccionado del editor via JavaScript
        String selectedText = getSelectedTextFromEditor();
        String initialTerm = selectedText.isBlank() ? "" : selectedText.trim();

        TextInputDialog termDialog = new TextInputDialog(initialTerm);
        termDialog.setTitle("Nueva referencia");
        termDialog.setHeaderText("Palabra o termino a enlazar");
        termDialog.setContentText("Termino:");
        String term = termDialog.showAndWait().orElse("").trim();
        if (term.isBlank()) return;

        String definition = showLargeDefinitionDialog("");
        if (definition == null || definition.isBlank()) return;

        // Si hay texto seleccionado, reemplazarlo con el token; si no, insertar en cursor
        if (!selectedText.isBlank() && !initialTerm.isBlank()) {
            replaceSelectedTextWithGlossaryToken(term, definition, initialTerm);
        } else {
            richEditor.insertGlossaryMarker(term, definition, term);
        }
        currentChapter.setBody(richEditor.getHtmlContent());
        refreshGlossaryList();
    }

    private String getSelectedTextFromEditor() {
        try {
            Object result = richEditor.getWebViewEngine().executeScript(
                    "window.getSelection().toString();"
            );
            return result == null ? "" : result.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void replaceSelectedTextWithGlossaryToken(String term, String definition, String label) {
        try {
            String token = "[[GLOSS:"
                    + sanitizeGlossaryPart(term) + "|"
                    + sanitizeGlossaryPart(definition) + "|"
                    + sanitizeGlossaryPart(label) + "]]";
            String safeToken = token.replace("'", "\\'");
            richEditor.getWebViewEngine().executeScript(
                    "document.execCommand('insertHTML', false, '" + safeToken + "');"
            );
        } catch (Exception e) {
            // Fallback: insertar normalmente
            richEditor.insertGlossaryMarker(term, definition, label);
        }
    }

    private String showLargeDefinitionDialog(String initialText) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Definicion del termino");
        dialog.setHeaderText("Escribe la definicion o nota para el glosario");

        ButtonType saveButtonType = new ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextArea textArea = new TextArea(initialText);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(8);
        textArea.setPrefColumnCount(50);
        textArea.getStyleClass().add("custom-text-area");

        dialog.getDialogPane().setContent(textArea);

        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        textArea.textProperty().addListener((o, old, val) ->
                saveButton.setDisable(val == null || val.trim().isBlank())
        );

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
        });

        return textArea.getText().trim();
    }

    private String sanitizeGlossaryPart(String input) {
        return input.replace("|", "/").replace("]]", "] ]").trim();
    }

    private void editSelectedGlossaryReference() {
        GlossaryToken selected = glossaryList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog termDialog = new TextInputDialog(selected.term);
        termDialog.setTitle("Editar referencia");
        termDialog.setHeaderText("Termino del glosario");
        termDialog.setContentText("Termino:");
        String term = termDialog.showAndWait().orElse("").trim();
        if (term.isBlank()) return;

        String definition = showLargeDefinitionDialog(selected.definition);
        if (definition == null || definition.isBlank()) return;

        TextInputDialog labelDialog = new TextInputDialog(selected.label);
        labelDialog.setTitle("Editar referencia");
        labelDialog.setHeaderText("Texto visible en el capitulo");
        labelDialog.setContentText("Texto visible:");
        String label = labelDialog.showAndWait().orElse("").trim();
        if (label.isBlank()) return;

        try {
            String token = "[[GLOSS:"
                    + sanitizeGlossaryPart(term) + "|"
                    + sanitizeGlossaryPart(definition) + "|"
                    + sanitizeGlossaryPart(label) + "]]";
            String safeToken = token.replace("'", "\\'");
            // Seleccionar el token original por posicion y reemplazarlo
            selectAndReplaceInEditor(selected.start, selected.end, safeToken);
        } catch (Exception e) {
            // Fallback
            String replacement = "[[GLOSS:"
                    + sanitizeGlossaryPart(term) + "|"
                    + sanitizeGlossaryPart(definition) + "|"
                    + sanitizeGlossaryPart(label) + "]]";
            String content = richEditor.getHtmlContent();
            String newContent = content.substring(0, selected.start) + replacement + content.substring(selected.end);
            richEditor.setHtmlContent(newContent);
        }
        currentChapter.setBody(richEditor.getHtmlContent());
        refreshGlossaryList();
    }

    private void selectAndReplaceInEditor(int start, int end, String replacement) {
        String js = """
                (function() {
                    var body = document.body;
                    var text = body.innerHTML;
                    var before = text.substring(0, %d);
                    var after = text.substring(%d);
                    body.innerHTML = before + '%s' + after;
                })();
                """.formatted(start, end, replacement);
        richEditor.getWebViewEngine().executeScript(js);
    }

    private void removeSelectedGlossaryReference() {
        GlossaryToken selected = glossaryList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            String safeLabel = selected.label.replace("'", "\\'");
            String js = """
                    (function() {
                        var body = document.body;
                        var text = body.innerHTML;
                        var before = text.substring(0, %d);
                        var after = text.substring(%d);
                        body.innerHTML = before + '%s' + after;
                    })();
                    """.formatted(selected.start, selected.end, safeLabel);
            richEditor.getWebViewEngine().executeScript(js);
        } catch (Exception e) {
            String content = richEditor.getHtmlContent();
            String newContent = content.substring(0, selected.start) + selected.label + content.substring(selected.end);
            richEditor.setHtmlContent(newContent);
        }
        currentChapter.setBody(richEditor.getHtmlContent());
        refreshGlossaryList();
    }

    private void refreshGlossaryList() {
        glossaryTokens.setAll(parseGlossaryTokens(richEditor.getHtmlContent()));
    }

    private List<GlossaryToken> parseGlossaryTokens(String html) {
        List<GlossaryToken> tokens = new ArrayList<>();
        if (html == null || html.isBlank()) return tokens;

        Matcher matcher = GLOSS_PATTERN.matcher(html);
        while (matcher.find()) {
            String term = matcher.group(1).trim();
            String definition = matcher.group(2).trim();
            String label = matcher.group(3).trim();
            tokens.add(new GlossaryToken(matcher.start(), matcher.end(), term, definition, label));
        }
        return tokens;
    }

    private static class GlossaryToken {
        private final int start;
        private final int end;
        private final String term;
        private final String definition;
        private final String label;

        private GlossaryToken(int start, int end, String term, String definition, String label) {
            this.start = start;
            this.end = end;
            this.term = term;
            this.definition = definition;
            this.label = label;
        }
    }

    private class ChapterCell extends ListCell<Chapter> {

        ChapterCell() {
            setOnDragDetected(e -> {
                if (getItem() == null) return;
                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(getIndex()));
                db.setContent(cc);
                e.consume();
            });

            setOnDragOver(e -> {
                if (e.getGestureSource() != this && e.getDragboard().hasString()) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });

            setOnDragDropped(e -> {
                if (getItem() == null) return;
                Dragboard db = e.getDragboard();
                if (db.hasString()) {
                    int from = Integer.parseInt(db.getString());
                    int to = getIndex();
                    if (from != to) {
                        Chapter moved = chapters.remove(from);
                        chapters.add(to, moved);
                        syncBookChapters();
                        Platform.runLater(() -> listView.getSelectionModel().select(to));
                    }
                    e.setDropCompleted(true);
                }
                e.consume();
            });
        }

        @Override
        protected void updateItem(Chapter item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            setText(item.toString());
        }
    }
}
