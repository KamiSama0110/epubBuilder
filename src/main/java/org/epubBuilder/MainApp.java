package org.epubBuilder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.epubBuilder.model.Book;
import org.epubBuilder.ui.ChaptersPane;
import org.epubBuilder.ui.ExportPane;
import org.epubBuilder.ui.MetadataPane;
import org.epubBuilder.ui.PreliminariesPane;

public class MainApp extends Application {

    private final Book book = new Book(); // UNA sola instancia

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab metaTab = new Tab("  Metadatos  ");
        metaTab.setContent(new MetadataPane(book, primaryStage));

        Tab prelimTab = new Tab("  Preliminares  ");
        prelimTab.setContent(new PreliminariesPane(book, primaryStage));

        Tab chaptersTab = new Tab("  Capítulos  ");
        chaptersTab.setContent(new ChaptersPane(book, primaryStage));

        ExportPane exportPane = new ExportPane(book, primaryStage);
        Tab exportTab = new Tab("  Exportar  ");
        exportTab.setContent(exportPane);
        exportTab.setOnSelectionChanged(e -> {
            if (exportTab.isSelected()) exportPane.refresh();
        });

        tabPane.getTabs().addAll(metaTab, prelimTab, chaptersTab, exportTab);

        Scene scene = new Scene(new BorderPane(tabPane), 980, 680);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(javafx.scene.paint.Color.web("#f4f6f8"));

        primaryStage.setTitle("EPUB Builder");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(560);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}