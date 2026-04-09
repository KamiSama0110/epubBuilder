package org.epubBuilder.ui;

import org.epubBuilder.model.Book;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MetadataPane extends ScrollPane {

    private final Book book;
    private final ImageView coverPreview = new ImageView();

    public MetadataPane(Book book, Stage stage) {
        this.book = book;
        buildUI(stage);
    }

    private void buildUI(Stage stage) {
        VBox container = new VBox(24);
        container.setPadding(new Insets(34, 40, 34, 40));
        container.getStyleClass().add("pane-bg");

        VBox head = new VBox(6);
        Label title = new Label("Metadatos");
        title.getStyleClass().add("heading-label");
        Label subtitle = new Label("Completa la informacion principal del libro antes de exportar.");
        subtitle.getStyleClass().add("title-label");
        head.getChildren().addAll(title, subtitle);

        HBox body = new HBox(20);
        body.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(18);
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox basicCard = new VBox(14);
        basicCard.getStyleClass().add("card");
        Label basicTitle = new Label("Informacion Basica");
        basicTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        VBox titleField = field("Titulo", book.titleProperty(), false);
        GridPane.setColumnSpan(titleField, 2);
        grid.add(titleField, 0, 0);
        grid.add(field("Autor", book.authorProperty(), false), 0, 1);
        grid.add(field("Ilustrador", book.illustratorProperty(), false), 1, 1);

        VBox langField = field("Idioma", book.languageProperty(), false);
        GridPane.setColumnSpan(langField, 2);
        grid.add(langField, 0, 2);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        basicCard.getChildren().addAll(basicTitle, grid);

        VBox synopsisCard = new VBox(14);
        synopsisCard.getStyleClass().add("card");
        Label synopsisTitle = new Label("Sinopsis");
        synopsisTitle.getStyleClass().add("section-title");
        synopsisCard.getChildren().addAll(synopsisTitle, field("Resumen", book.synopsisProperty(), true));

        left.getChildren().addAll(basicCard, synopsisCard);

        VBox cover = buildCover(stage);
        body.getChildren().addAll(left, cover);

        container.getChildren().addAll(head, body);

        setContent(container);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("pane-bg");
    }

    private VBox field(String label, javafx.beans.property.StringProperty prop, boolean multi) {
        VBox box = new VBox(6);

        Label lbl = new Label(label.toUpperCase());
        lbl.getStyleClass().add("field-label");

        if (multi) {
            TextArea area = new TextArea();
            area.getStyleClass().add("custom-text-area");
            area.setPrefRowCount(5);
            area.setWrapText(true);
            area.textProperty().bindBidirectional(prop);
            box.getChildren().addAll(lbl, area);
        } else {
            TextField tf = new TextField();
            tf.getStyleClass().add("custom-text-field");
            tf.textProperty().bindBidirectional(prop);
            box.getChildren().addAll(lbl, tf);
        }

        return box;
    }

    private VBox buildCover(Stage stage) {
        VBox box = new VBox(14);
        box.setPrefWidth(290);
        box.setMinWidth(260);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().add("card");

        Label lbl = new Label("Portada");
        lbl.getStyleClass().add("section-title");

        Label hint = new Label("Formato recomendado: JPG o PNG vertical.");
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        coverPreview.setFitWidth(190);
        coverPreview.setFitHeight(260);
        coverPreview.setPreserveRatio(true);

        StackPane frame = new StackPane();
        frame.setPrefSize(210, 280);
        frame.getStyleClass().add("image-frame");

        Label placeholder = new Label("Sin imagen");
        placeholder.getStyleClass().add("hint-label");
        frame.getChildren().addAll(placeholder, coverPreview);

        Button btn = new Button("Seleccionar portada");
        btn.getStyleClass().add("action-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar portada");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
            );
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                book.setCoverPath(f.getAbsolutePath());
                coverPreview.setImage(new Image(f.toURI().toString()));
                placeholder.setVisible(false);
            }
        });

        box.getChildren().addAll(lbl, hint, frame, btn);
        return box;
    }
}