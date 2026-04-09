package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.PreliminaryPage;
import org.epubBuilder.model.PreliminaryPage.Type;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class PreliminariesPane extends HBox {

    private final Book  book;
    private final Stage stage;
    private final ObservableList<PreliminaryPage> pages = FXCollections.observableArrayList();
    private final ListView<PreliminaryPage> listView    = new ListView<>(pages);

    // Editor
    private final Label     editorTitle  = new Label("Ninguna página seleccionada");
    private final TextField titleField   = new TextField();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextArea  textArea     = new TextArea();
    private final Label     imgPathLabel = new Label("Sin imagen seleccionada");
    private final ImageView imgPreview   = new ImageView();
    private final VBox      editorPane   = new VBox(24);

    private PreliminaryPage current = null;
    private boolean loading         = false;

    public PreliminariesPane(Book book, Stage stage) {
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

        Label label = new Label("PRELIMINARES");
        label.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                -fx-letter-spacing: 1.5px;
                """);

        Label hint = new Label("Se insertan después de la portada y antes del primer capítulo.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #333344; -fx-font-size: 11px;");

        listView.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                """);
        listView.setFixedCellSize(40);
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new PageCell());
        listView.getSelectionModel().selectedItemProperty().addListener(
                (o, old, sel) -> { if (!loading) loadPage(sel); }
        );

        Button btnAdd = styledButton("+ Nueva página", "#5e5ce6", "#f0f0f5");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addPage());

        Button btnDel = styledButton("Eliminar", "#1e1e28", "#e05c5c");
        btnDel.setMaxWidth(Double.MAX_VALUE);
        btnDel.setOnAction(e -> deletePage());

        sidebar.getChildren().addAll(label, hint, listView, btnAdd, btnDel);
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

        // Título de la página
        Label titleLbl = fieldLabel("TÍTULO DE LA PÁGINA");
        titleField.setStyle(fieldStyle());
        titleField.setPromptText("Ej: Dedicatoria");
        titleField.setDisable(true);

        // Tipo
        Label typeLbl = fieldLabel("TIPO DE PÁGINA");
        typeBox.getItems().addAll("Solo imagen", "Solo texto", "Imagen y texto");
        typeBox.setValue("Solo imagen");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setDisable(true);
        typeBox.setStyle("""
            -fx-background-color: #18181f;
            -fx-text-fill: #e8e8f0;
            -fx-border-color: #2a2a38;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            """);

        // Imagen
        Label imgLbl = fieldLabel("IMAGEN");

        imgPreview.setFitWidth(140);
        imgPreview.setFitHeight(180);
        imgPreview.setPreserveRatio(true);
        imgPreview.setVisible(false);

        imgPathLabel.setStyle("-fx-text-fill: #333344; -fx-font-size: 11px;");
        imgPathLabel.setWrapText(true);

        Button btnPickImg = new Button("Seleccionar imagen");
        btnPickImg.setStyle("""
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
        btnPickImg.setDisable(true);
        btnPickImg.setOnAction(e -> pickImage(btnPickImg));

        HBox imgRow = new HBox(16, imgPreview, new VBox(8, imgPathLabel, btnPickImg));
        imgRow.setAlignment(Pos.CENTER_LEFT);

        // Texto — más grande
        Label textLbl = fieldLabel("TEXTO");
        textArea.setStyle(fieldStyle() + "-fx-control-inner-background: #18181f;");
        textArea.setPromptText("Escribe el contenido de esta página...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        textArea.setMinHeight(200);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setDisable(true);

        VBox imgSection  = new VBox(12, imgLbl, imgRow);
        VBox textSection = new VBox(12, textLbl, textArea);
        VBox.setVgrow(textSection, Priority.ALWAYS);

        // Contenido scrollable
        VBox inner = new VBox(24,
                editorTitle, new Separator(),
                titleLbl, titleField,
                typeLbl, typeBox,
                imgSection, textSection
        );
        inner.setPadding(new Insets(40, 52, 40, 52));
        inner.setStyle("-fx-background-color: #0f0f13;");
        VBox.setVgrow(textSection, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(inner);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #0f0f13; -fx-background: #0f0f13;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        editorPane.getChildren().add(scroll);
        editorPane.setPadding(Insets.EMPTY);

        // Listeners
        typeBox.valueProperty().addListener((o, old, val) -> {
            if (loading || current == null) return;
            current.setType(mapType(val));
            updateSectionVisibility(imgSection, textSection, val);
        });

        titleField.textProperty().addListener((o, old, val) -> {
            if (loading || current == null) return;
            current.setTitle(val);
            editorTitle.setText(val.isBlank() ? "(sin título)" : val);
            editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #f0f0f5;
                """);
            listView.refresh();
        });

        textArea.textProperty().addListener((o, old, val) -> {
            if (loading || current == null) return;
            current.setText(val);
        });

        btnPickImg.setUserData("btnPickImg");
        editorPane.setUserData(new Object[]{ imgSection, textSection, btnPickImg });

        return editorPane;
    }

    // ── Acciones ───────────────────────────────────────────────────
    private void addPage() {
        PreliminaryPage p = new PreliminaryPage("Página " + (pages.size() + 1), Type.IMAGEN);
        pages.add(p);
        book.getPreliminaryPages().add(p);
        Platform.runLater(() -> {
            listView.getSelectionModel().select(p);
            listView.scrollTo(p);
        });
    }

    private void deletePage() {
        PreliminaryPage sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = sel.getTitle().isBlank() ? "(sin título)" : sel.getTitle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + name + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                pages.remove(sel);
                book.getPreliminaryPages().remove(sel);
                clearEditor();
            }
        });
    }

    private void loadPage(PreliminaryPage p) {
        loading = true;
        current = null;
        if (p == null) { clearEditor(); loading = false; return; }

        Object[] refs = (Object[]) editorPane.getUserData();
        VBox imgSection  = (VBox) refs[0];
        VBox textSection = (VBox) refs[1];
        Button btnPickImg = (Button) refs[2];

        titleField.setDisable(false);
        typeBox.setDisable(false);
        textArea.setDisable(false);
        btnPickImg.setDisable(false);

        titleField.setText(p.getTitle());
        typeBox.setValue(mapTypeToString(p.getType()));
        textArea.setText(p.getText());
        editorTitle.setText(p.getTitle().isBlank() ? "(sin título)" : p.getTitle());
        editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #f0f0f5;
                """);

        // Preview de imagen si existe
        if (!p.getImagePath().isBlank()) {
            File f = new File(p.getImagePath());
            if (f.exists()) {
                imgPreview.setImage(new Image(f.toURI().toString()));
                imgPreview.setVisible(true);
                imgPathLabel.setText(f.getName());
            }
        } else {
            imgPreview.setVisible(false);
            imgPathLabel.setText("Sin imagen seleccionada");
        }

        updateSectionVisibility(imgSection, textSection, typeBox.getValue());
        current = p;
        loading = false;
    }

    private void clearEditor() {
        current = null;
        editorTitle.setText("Ninguna página seleccionada");
        editorTitle.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                """);
        titleField.setText("");
        titleField.setDisable(true);
        typeBox.setDisable(true);
        textArea.setText("");
        textArea.setDisable(true);
        imgPreview.setVisible(false);
        imgPathLabel.setText("Sin imagen seleccionada");

        Object[] refs = (Object[]) editorPane.getUserData();
        if (refs != null) ((Button) refs[2]).setDisable(true);
    }

    private void pickImage(Button btn) {
        if (current == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        current.setImagePath(f.getAbsolutePath());
        imgPreview.setImage(new Image(f.toURI().toString()));
        imgPreview.setVisible(true);
        imgPathLabel.setText(f.getName());
    }

    private void updateSectionVisibility(VBox imgSection, VBox textSection, String val) {
        imgSection.setVisible(!val.equals("Solo texto"));
        imgSection.setManaged(!val.equals("Solo texto"));
        textSection.setVisible(!val.equals("Solo imagen"));
        textSection.setManaged(!val.equals("Solo imagen"));
    }

    private Type mapType(String val) {
        return switch (val) {
            case "Solo texto"      -> Type.TEXTO;
            case "Imagen y texto"  -> Type.IMAGEN_Y_TEXTO;
            default                -> Type.IMAGEN;
        };
    }

    private String mapTypeToString(Type t) {
        return switch (t) {
            case TEXTO          -> "Solo texto";
            case IMAGEN_Y_TEXTO -> "Imagen y texto";
            default             -> "Solo imagen";
        };
    }

    // ── Helpers ────────────────────────────────────────────────────
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
    private class PageCell extends ListCell<PreliminaryPage> {
        PageCell() {
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
                        PreliminaryPage moved = pages.remove(from);
                        pages.add(to, moved);
                        book.getPreliminaryPages().clear();
                        book.getPreliminaryPages().addAll(pages);
                        Platform.runLater(() -> listView.getSelectionModel().select(to));
                    }
                    e.setDropCompleted(true);
                }
                e.consume();
            });
        }

        @Override
        protected void updateItem(PreliminaryPage item, boolean empty) {
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