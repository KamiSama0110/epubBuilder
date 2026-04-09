package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.Chapter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ChaptersPane extends HBox {

    private final Book  book;
    private final Stage stage;
    private final ObservableList<Chapter> chapters = FXCollections.observableArrayList();
    private final ListView<Chapter> listView = new ListView<>(chapters);

    private final Label     editorTitle  = new Label("Ningún capítulo seleccionado");
    private final TextField titleField   = new TextField();
    private final TextArea  bodyArea     = new TextArea();
    private final Button    btnInsertImg = new Button("⬜ Insertar imagen aquí");
    private final VBox      editorPane   = new VBox(24);

    private Chapter currentChapter = null;
    private boolean loading        = false;

    public ChaptersPane(Book book, Stage stage) {
        this.book  = book;
        this.stage = stage;
        buildUI();
    }

    private void buildUI() {
        setStyle("-fx-background-color: #0f0f13;");
        getChildren().addAll(buildSidebar(), buildEditor());
        HBox.setHgrow(editorPane, Priority.ALWAYS);
    }

    // ── Sidebar ────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(180);
        sidebar.setPadding(new Insets(32, 16, 24, 16));
        sidebar.setStyle("""
                -fx-background-color: #0c0c10;
                -fx-border-color: #1e1e28;
                -fx-border-width: 0 1 0 0;
                """);

        Label label = new Label("CAPÍTULOS");
        label.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                -fx-letter-spacing: 1.5px;
                """);

        listView.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                """);
        listView.setFixedCellSize(40);
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new ChapterCell());
        listView.getSelectionModel().selectedItemProperty().addListener(
                (o, old, sel) -> { if (!loading) loadChapter(sel); }
        );

        Button btnAdd = styledButton("+ Nuevo capítulo", "#5e5ce6", "#f0f0f5");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addChapter());

        Button btnDel = styledButton("Eliminar", "#1e1e28", "#e05c5c");
        btnDel.setMaxWidth(Double.MAX_VALUE);
        btnDel.setOnAction(e -> deleteChapter());

        sidebar.getChildren().addAll(label, listView, btnAdd, btnDel);
        return sidebar;
    }

    // ── Editor ─────────────────────────────────────────────────────
    private VBox buildEditor() {
        editorPane.setPadding(new Insets(40, 52, 40, 52));
        editorPane.setStyle("-fx-background-color: #0f0f13;");

        editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                """);

        // Campo título
        Label titleLbl = fieldLabel("TÍTULO DEL CAPÍTULO");
        titleField.setStyle(fieldStyle());
        titleField.setPromptText("Ej: El comienzo");
        titleField.setDisable(true);

        // Cuerpo
        Label bodyLbl = fieldLabel("CONTENIDO");

        // Hint de uso
        Label hint = new Label(
                "Tip: coloca el cursor donde quieras insertar una imagen y pulsa el botón."
        );
        hint.setStyle("-fx-text-fill: #333344; -fx-font-size: 11px;");
        hint.setWrapText(true);

        bodyArea.setStyle(fieldStyle() + "-fx-control-inner-background: #18181f;");
        bodyArea.setPromptText("Escribe el contenido del capítulo aquí...");
        bodyArea.setWrapText(true);
        bodyArea.setDisable(true);
        VBox.setVgrow(bodyArea, Priority.ALWAYS);

        // Botón insertar imagen
        Label imgLbl = fieldLabel("IMÁGENES INLINE");
        btnInsertImg.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #5e5ce6;
                -fx-border-color: #5e5ce6;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
                -fx-padding: 6 16 6 16;
                -fx-font-size: 12px;
                -fx-cursor: hand;
                """);
        btnInsertImg.setDisable(true);
        btnInsertImg.setOnAction(e -> insertImageAtCursor());

        editorPane.getChildren().addAll(
                editorTitle, new Separator(),
                titleLbl, titleField,
                bodyLbl, bodyArea,
                imgLbl, hint, btnInsertImg
        );

        // Listeners con bandera loading
        titleField.textProperty().addListener((o, old, val) -> {
            if (loading || currentChapter == null) return;
            currentChapter.setTitle(val);
            editorTitle.setText(val.isBlank() ? "(sin título)" : val);
            editorTitle.setStyle("""
                    -fx-font-size: 22px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #f0f0f5;
                    """);
            listView.refresh();
        });

        bodyArea.textProperty().addListener((o, old, val) -> {
            if (loading || currentChapter == null) return;
            currentChapter.setBody(val);
        });

        return editorPane;
    }

    // ── Insertar imagen en posición del cursor ─────────────────────
    private void insertImageAtCursor() {
        if (currentChapter == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        String mark   = "\n[IMAGEN:" + f.getAbsolutePath() + "]\n";
        int    caret  = bodyArea.getCaretPosition();
        String before = bodyArea.getText().substring(0, caret);
        String after  = bodyArea.getText().substring(caret);
        bodyArea.setText(before + mark + after);
        bodyArea.positionCaret(caret + mark.length());
    }

    // ── Acciones de capítulos ──────────────────────────────────────
    private void addChapter() {
        Chapter c = new Chapter("Capítulo " + (chapters.size() + 1));
        chapters.add(c);
        book.getChapters().add(c);
        Platform.runLater(() -> {
            listView.getSelectionModel().select(c);
            listView.scrollTo(c);
        });
    }

    private void deleteChapter() {
        Chapter sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.getTitle().isBlank() ? "(sin título)" : sel.getTitle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + name + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                chapters.remove(sel);
                book.getChapters().remove(sel);
                clearEditor();
            }
        });
    }

    private void loadChapter(Chapter c) {
        loading = true;
        currentChapter = null;
        if (c == null) { clearEditor(); loading = false; return; }

        titleField.setDisable(false);
        bodyArea.setDisable(false);
        btnInsertImg.setDisable(false);

        titleField.setText(c.getTitle());
        bodyArea.setText(c.getBody());
        editorTitle.setText(c.getTitle().isBlank() ? "(sin título)" : c.getTitle());
        editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #f0f0f5;
                """);

        currentChapter = c;
        loading = false;
    }

    private void clearEditor() {
        currentChapter = null;
        editorTitle.setText("Ningún capítulo seleccionado");
        editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                """);
        titleField.setText("");
        titleField.setDisable(true);
        bodyArea.setText("");
        bodyArea.setDisable(true);
        btnInsertImg.setDisable(true);
    }

    // ── Helpers de estilo ──────────────────────────────────────────
    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                -fx-letter-spacing: 1.5px;
                """);
        return l;
    }

    private String fieldStyle() {
        return """
                -fx-background-color: #18181f;
                -fx-text-fill: #e8e8f0;
                -fx-border-color: #2a2a38;
                -fx-border-width: 0 0 1 0;
                -fx-background-radius: 0;
                -fx-border-radius: 0;
                -fx-padding: 10 4 10 4;
                -fx-font-size: 14px;
                """;
    }

    private Button styledButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle(String.format("""
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 4;
                -fx-padding: 9 0 9 0;
                -fx-cursor: hand;
                """, bg, fg));
        return b;
    }

    // ── Celda con drag & drop ──────────────────────────────────────
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
                if (e.getGestureSource() != this && e.getDragboard().hasString())
                    e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            });
            setOnDragDropped(e -> {
                if (getItem() == null) return;
                Dragboard db = e.getDragboard();
                if (db.hasString()) {
                    int from = Integer.parseInt(db.getString());
                    int to   = getIndex();
                    if (from != to) {
                        Chapter moved = chapters.remove(from);
                        chapters.add(to, moved);
                        book.getChapters().clear();
                        book.getChapters().addAll(chapters);
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
                setStyle("-fx-background-color: transparent;");
                return;
            }
            setText(item.toString());
            boolean selected = getListView().getSelectionModel().getSelectedItem() == item;
            setStyle(String.format("""
                    -fx-text-fill: %s;
                    -fx-font-size: 13px;
                    -fx-padding: 0 8 0 8;
                    -fx-background-color: %s;
                    -fx-background-radius: 4;
                    """,
                    selected ? "#f0f0f5" : "#707088",
                    selected ? "#1e1e2e" : "transparent"
            ));
        }
    }
}