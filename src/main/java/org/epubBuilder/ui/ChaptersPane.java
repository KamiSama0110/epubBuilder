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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ChaptersPane extends HBox {

    private final Book book;
    private final Stage stage;
    private final ObservableList<Chapter> chapters = FXCollections.observableArrayList();
    private final ListView<Chapter> listView = new ListView<>(chapters);

    private final Label editorTitle = new Label();
    private final TextField titleField = new TextField();
    private final TextArea bodyArea = new TextArea();
    private final Button btnInsertImg = new Button("Insertar imagen en cursor");
    private final VBox editorPane = new VBox();

    private Chapter currentChapter;
    private boolean loading;

    public ChaptersPane(Book book, Stage stage) {
        this.book = book;
        this.stage = stage;
        this.chapters.setAll(book.getChapters());
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

        bodyArea.textProperty().addListener((o, old, val) -> {
            if (loading || currentChapter == null) return;
            currentChapter.setBody(val);
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

        Button btnAdd = new Button("+ Nuevo");
        btnAdd.getStyleClass().add("action-button");
        btnAdd.setPrefWidth(96);
        btnAdd.setOnAction(e -> addChapter());

        Button btnDelete = new Button("Eliminar");
        btnDelete.getStyleClass().add("danger-button");
        btnDelete.setPrefWidth(96);
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

        bodyArea.getStyleClass().add("custom-text-area");
        bodyArea.setWrapText(true);
        bodyArea.setPrefRowCount(16);
        VBox.setVgrow(bodyArea, Priority.ALWAYS);

        btnInsertImg.getStyleClass().add("action-button");
        btnInsertImg.setOnAction(e -> insertImageAtCursor());

        VBox card = new VBox(10, editorTitle, titleLabel, titleField, bodyLabel, bodyArea, btnInsertImg);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

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
        bodyArea.setDisable(false);
        btnInsertImg.setDisable(false);

        titleField.setText(chapter.getTitle());
        bodyArea.setText(chapter.getBody());
        updateEditorTitle(chapter.getTitle());

        currentChapter = chapter;
        loading = false;
    }

    private void clearEditor() {
        currentChapter = null;
        updateEditorTitle("");
        titleField.clear();
        titleField.setDisable(true);
        bodyArea.clear();
        bodyArea.setDisable(true);
        btnInsertImg.setDisable(true);
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

    private void insertImageAtCursor() {
        if (currentChapter == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        String mark = "\n[IMAGEN:" + file.getAbsolutePath() + "]\n";
        int caret = bodyArea.getCaretPosition();
        bodyArea.insertText(caret, mark);
        bodyArea.positionCaret(caret + mark.length());
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
