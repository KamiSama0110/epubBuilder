package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import org.epubBuilder.model.PreliminaryPage;
import org.epubBuilder.model.PreliminaryPage.Type;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class PreliminariesPane extends HBox {

    private final Book book;
    private final Stage stage;
    private final ObservableList<PreliminaryPage> pages = FXCollections.observableArrayList();
    private final ListView<PreliminaryPage> listView = new ListView<>(pages);

    private final Label editorTitle = new Label();
    private final TextField titleField = new TextField();
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextArea textArea = new TextArea();
    private final Label imgPathLabel = new Label("Sin imagen seleccionada");
    private final ImageView imgPreview = new ImageView();
    private final Button btnPickImg = new Button("Seleccionar imagen");

    private final VBox editorPane = new VBox();
    private final VBox imgSection = new VBox(8);
    private final VBox textSection = new VBox(8);

    private PreliminaryPage current;
    private boolean loading;

    public PreliminariesPane(Book book, Stage stage) {
        this.book = book;
        this.stage = stage;
        this.pages.setAll(book.getPreliminaryPages());
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("pane-bg");

        VBox sidebar = buildSidebar();
        VBox editor = buildEditor();
        getChildren().addAll(sidebar, editor);
        HBox.setHgrow(editor, Priority.ALWAYS);

        listView.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> loadPage(val));

        titleField.textProperty().addListener((o, old, val) -> {
            if (loading || current == null) return;
            current.setTitle(val);
            updateEditorTitle(val);
            listView.refresh();
        });

        textArea.textProperty().addListener((o, old, val) -> {
            if (loading || current == null) return;
            current.setText(val);
        });

        typeBox.valueProperty().addListener((o, old, val) -> {
            if (val == null) return;
            updateSectionVisibility(val);
            if (loading || current == null) return;
            current.setType(mapType(val));
        });

        btnPickImg.setOnAction(e -> pickImage());

        clearEditor();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(220);
        sidebar.setPadding(new Insets(24, 14, 24, 14));
        sidebar.getStyleClass().add("sidebar-bg");

        Label label = new Label("PRELIMINARES");
        label.getStyleClass().add("field-label");

        Label hint = new Label("Paginas previas al primer capitulo.");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint-label");

        Button btnAdd = new Button("+");
        btnAdd.getStyleClass().add("toolbar-button");
        btnAdd.setTooltip(new Tooltip("Crear pagina preliminar"));
        btnAdd.setPrefSize(36, 36);
        btnAdd.setOnAction(e -> addPage());

        Button btnDelete = new Button("x");
        btnDelete.getStyleClass().add("toolbar-danger-button");
        btnDelete.setTooltip(new Tooltip("Eliminar pagina preliminar"));
        btnDelete.setPrefSize(36, 36);
        btnDelete.setOnAction(e -> deletePage());

        HBox actions = new HBox(8, btnAdd, btnDelete);
        actions.setAlignment(Pos.CENTER);

        listView.getStyleClass().addAll("list-view", "entity-list");
        listView.setCellFactory(lv -> new PageCell());
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

        Label typeLabel = new Label("TIPO DE CONTENIDO");
        typeLabel.getStyleClass().add("field-label");
        typeBox.getItems().setAll("Solo imagen", "Solo texto", "Imagen y texto");

        Label textLabel = new Label("TEXTO");
        textLabel.getStyleClass().add("field-label");
        textArea.getStyleClass().add("custom-text-area");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textSection.getChildren().addAll(textLabel, textArea);

        Label imgLabel = new Label("IMAGEN");
        imgLabel.getStyleClass().add("field-label");
        imgPreview.setFitWidth(220);
        imgPreview.setFitHeight(130);
        imgPreview.setPreserveRatio(true);
        imgPreview.setVisible(false);

        StackPane frame = new StackPane(imgPreview);
        frame.setMinHeight(140);
        frame.getStyleClass().add("image-frame");

        btnPickImg.getStyleClass().add("action-button");
        imgPathLabel.getStyleClass().add("hint-label");
        imgPathLabel.setWrapText(true);

        imgSection.getChildren().addAll(imgLabel, frame, imgPathLabel, btnPickImg);

        VBox card = new VBox(10,
                editorTitle,
                titleLabel,
                titleField,
                typeLabel,
                typeBox,
                imgSection,
                textSection
        );
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        editorPane.getChildren().add(card);
        return editorPane;
    }

    private void addPage() {
        PreliminaryPage page = new PreliminaryPage("Pagina " + (pages.size() + 1), Type.IMAGEN);
        pages.add(page);
        syncBookPages();
        Platform.runLater(() -> {
            listView.getSelectionModel().select(page);
            listView.scrollTo(page);
        });
    }

    private void deletePage() {
        PreliminaryPage selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        int removedIndex = listView.getSelectionModel().getSelectedIndex();

        String name = selected.getTitle().isBlank() ? "(sin titulo)" : selected.getTitle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + name + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                pages.remove(selected);
                syncBookPages();

                if (pages.isEmpty()) {
                    listView.getSelectionModel().clearSelection();
                    clearEditor();
                    return;
                }

                int nextIndex = Math.min(removedIndex, pages.size() - 1);
                Platform.runLater(() -> {
                    listView.getSelectionModel().select(nextIndex);
                    listView.scrollTo(nextIndex);
                    loadPage(pages.get(nextIndex));
                });
            }
        });
    }

    private void loadPage(PreliminaryPage page) {
        loading = true;
        current = null;

        if (page == null) {
            clearEditor();
            loading = false;
            return;
        }

        titleField.setDisable(false);
        typeBox.setDisable(false);
        textArea.setDisable(false);
        btnPickImg.setDisable(false);

        titleField.setText(page.getTitle());
        typeBox.setValue(mapTypeToString(page.getType()));
        textArea.setText(page.getText());
        updateEditorTitle(page.getTitle());

        updateImagePreview(page.getImagePath());
        updateSectionVisibility(typeBox.getValue());

        current = page;
        loading = false;
    }

    private void clearEditor() {
        current = null;
        updateEditorTitle("");

        titleField.clear();
        titleField.setDisable(true);

        typeBox.setValue("Solo imagen");
        typeBox.setDisable(true);

        textArea.clear();
        textArea.setDisable(true);

        imgPreview.setVisible(false);
        imgPathLabel.setText("Sin imagen seleccionada");
        btnPickImg.setDisable(true);

        updateSectionVisibility("Solo imagen");
    }

    private void updateEditorTitle(String value) {
        boolean empty = value == null || value.isBlank();
        editorTitle.setText(empty ? "Ninguna pagina seleccionada" : value);
        editorTitle.getStyleClass().removeAll("editor-title", "empty-state-label");
        editorTitle.getStyleClass().add(empty ? "empty-state-label" : "editor-title");
    }

    private void pickImage() {
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        current.setImagePath(file.getAbsolutePath());
        updateImagePreview(file.getAbsolutePath());
    }

    private void updateImagePreview(String path) {
        if (path == null || path.isBlank()) {
            imgPreview.setVisible(false);
            imgPathLabel.setText("Sin imagen seleccionada");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            imgPreview.setVisible(false);
            imgPathLabel.setText("Archivo no encontrado");
            return;
        }

        imgPreview.setImage(new Image(file.toURI().toString()));
        imgPreview.setVisible(true);
        imgPathLabel.setText(file.getName());
    }

    private void updateSectionVisibility(String val) {
        boolean onlyText = "Solo texto".equals(val);
        boolean onlyImage = "Solo imagen".equals(val);

        imgSection.setVisible(!onlyText);
        imgSection.setManaged(!onlyText);

        textSection.setVisible(!onlyImage);
        textSection.setManaged(!onlyImage);
    }

    private Type mapType(String val) {
        if ("Solo texto".equals(val)) return Type.TEXTO;
        if ("Imagen y texto".equals(val)) return Type.IMAGEN_Y_TEXTO;
        return Type.IMAGEN;
    }

    private String mapTypeToString(Type type) {
        if (type == Type.TEXTO) return "Solo texto";
        if (type == Type.IMAGEN_Y_TEXTO) return "Imagen y texto";
        return "Solo imagen";
    }

    private void syncBookPages() {
        book.getPreliminaryPages().clear();
        book.getPreliminaryPages().addAll(pages);
    }

    public void reloadFromBook() {
        PreliminaryPage selected = listView.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? "" : selected.getId();

        pages.setAll(book.getPreliminaryPages());
        listView.refresh();

        if (pages.isEmpty()) {
            listView.getSelectionModel().clearSelection();
            clearEditor();
            return;
        }

        PreliminaryPage toSelect = pages.get(0);
        if (!selectedId.isBlank()) {
            for (PreliminaryPage page : pages) {
                if (page.getId().equals(selectedId)) {
                    toSelect = page;
                    break;
                }
            }
        }

        listView.getSelectionModel().select(toSelect);
        loadPage(toSelect);
    }

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
                        PreliminaryPage moved = pages.remove(from);
                        pages.add(to, moved);
                        syncBookPages();
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
                setGraphic(null);
                return;
            }

            setText(item.toString());
        }
    }
}
