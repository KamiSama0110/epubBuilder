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
        tabPane.setStyle("""
            -fx-background-color: #0f0f13;
            -fx-tab-min-width: 120px;
            """);
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

        // CSS global para las tabs
        String tabCss = """
            .tab-pane .tab-header-area .tab-header-background {
                -fx-background-color: #0c0c10;
                -fx-border-color: #1e1e28;
                -fx-border-width: 0 0 1 0;
            }
            .tab-pane .tab {
                -fx-background-color: transparent;
                -fx-padding: 10 16 10 16;
            }
            .tab-pane .tab .tab-label {
                -fx-text-fill: #555566;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
            }
            .tab-pane .tab:selected .tab-label {
                -fx-text-fill: #f0f0f5;
            }
            .tab-pane .tab:selected {
                -fx-background-color: #0f0f13;
                -fx-border-color: #5e5ce6;
                -fx-border-width: 0 0 2 0;
            }
            .scroll-bar { -fx-opacity: 0; }
            """;

        Scene scene = new Scene(new BorderPane(tabPane), 980, 680);
        scene.getStylesheets().add(
                "data:text/css," + tabCss.replace(" ", "%20").replace("\n", "%0A")
        );
        scene.setFill(javafx.scene.paint.Color.web("#0f0f13"));

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