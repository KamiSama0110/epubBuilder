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

    private final Book      book;
    private final ImageView coverPreview = new ImageView();

    public MetadataPane(Book book, Stage stage) {
        this.book = book;
        buildUI(stage);
    }

    private void buildUI(Stage stage) {
        VBox container = new VBox(40);
        container.setPadding(new Insets(48, 56, 48, 56));
        container.setStyle("-fx-background-color: #0f0f13;");

        // ── Encabezado ─────────────────────────────────────────────
        Label heading = new Label("Información del libro");
        heading.setStyle("""
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-text-fill: #5e5ce6;
                -fx-letter-spacing: 2px;
                """);

        Label title = new Label("Metadatos");
        title.setStyle("""
                -fx-font-size: 28px;
                -fx-font-weight: bold;
                -fx-text-fill: #f0f0f5;
                """);

        VBox header = new VBox(4, heading, title);

        // ── Cuerpo: formulario + portada ───────────────────────────
        HBox body = new HBox(64);
        body.setAlignment(Pos.TOP_LEFT);

        VBox form = new VBox(28);
        form.setPrefWidth(480);

        form.getChildren().addAll(
                field("Título",        book.titleProperty(),       false),
                field("Autor",         book.authorProperty(),       false),
                field("Ilustrador",    book.illustratorProperty(),  false),
                field("Idioma",        book.languageProperty(),     false),
                field("Sinopsis",      book.synopsisProperty(),     true)
        );

        VBox cover = buildCover(stage);
        body.getChildren().addAll(form, cover);

        container.getChildren().addAll(header, body);

        setContent(container);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: #0f0f13; -fx-background: #0f0f13;");
    }

    private VBox field(String label, javafx.beans.property.StringProperty prop, boolean multi) {
        VBox box = new VBox(8);

        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                -fx-letter-spacing: 1.5px;
                """);

        String base = """
                -fx-background-color: #18181f;
                -fx-text-fill: #e8e8f0;
                -fx-border-color: #2a2a38;
                -fx-border-width: 0 0 1 0;
                -fx-background-radius: 0;
                -fx-border-radius: 0;
                -fx-padding: 10 4 10 4;
                -fx-font-size: 14px;
                """;

        if (multi) {
            TextArea area = new TextArea();
            area.setStyle(base + "-fx-control-inner-background: #18181f;");
            area.setPrefRowCount(4);
            area.setWrapText(true);
            area.textProperty().bindBidirectional(prop);
            box.getChildren().addAll(lbl, area);
        } else {
            TextField tf = new TextField();
            tf.setStyle(base);
            tf.textProperty().bindBidirectional(prop);
            box.getChildren().addAll(lbl, tf);
        }

        return box;
    }

    private VBox buildCover(Stage stage) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_CENTER);

        Label lbl = new Label("PORTADA");
        lbl.setStyle("""
                -fx-font-size: 10px;
                -fx-font-weight: bold;
                -fx-text-fill: #555566;
                -fx-letter-spacing: 1.5px;
                """);

        coverPreview.setFitWidth(150);
        coverPreview.setFitHeight(210);
        coverPreview.setPreserveRatio(true);

        StackPane frame = new StackPane();
        frame.setPrefSize(150, 210);
        frame.setStyle("""
                -fx-background-color: #18181f;
                -fx-border-color: #2a2a38;
                -fx-border-width: 1;
                """);

        Label placeholder = new Label("Sin imagen");
        placeholder.setStyle("-fx-text-fill: #333344; -fx-font-size: 12px;");
        frame.getChildren().addAll(placeholder, coverPreview);

        Button btn = new Button("Seleccionar");
        btn.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #5e5ce6;
                -fx-border-color: #5e5ce6;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
                -fx-padding: 7 20 7 20;
                -fx-font-size: 12px;
                -fx-cursor: hand;
                """);

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

        box.getChildren().addAll(lbl, frame, btn);
        return box;
    }
}