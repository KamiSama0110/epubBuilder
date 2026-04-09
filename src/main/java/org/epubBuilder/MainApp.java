package org.epubBuilder;

import javafx.application.Application;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.epubBuilder.io.ProjectStorage;
import org.epubBuilder.model.Book;
import org.epubBuilder.ui.ChaptersPane;
import org.epubBuilder.ui.ExportPane;
import org.epubBuilder.ui.GlossaryPane;
import org.epubBuilder.ui.MetadataPane;
import org.epubBuilder.ui.PreliminariesPane;

import java.io.File;
import java.io.IOException;

public class MainApp extends Application {

    private final Book book = new Book();

    private BorderPane root;
    private MetadataPane metadataPane;
    private PreliminariesPane preliminariesPane;
    private ChaptersPane chaptersPane;
    private GlossaryPane glossaryPane;
    private ExportPane exportPane;

    private File currentProjectFile;
    private Timeline autosaveTimeline;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        root.getStyleClass().add("pane-bg");

        HBox toolbar = buildProjectToolbar(primaryStage);
        root.setTop(toolbar);

        tryRecoverAutosave(primaryStage);
        rebuildTabs(primaryStage, 0);

        Scene scene = new Scene(root, 980, 680);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(javafx.scene.paint.Color.web("#f4f6f8"));

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(560);
        updateTitle(primaryStage);
        primaryStage.show();

        startAutosave();
        primaryStage.setOnCloseRequest(e -> autosaveSilently());
    }

    private HBox buildProjectToolbar(Stage stage) {
        HBox bar = new HBox(10);
        bar.setStyle("-fx-padding: 10 14; -fx-background-color: #ffffff; -fx-border-color: #d9dee5; -fx-border-width: 0 0 1 0;");

        Button btnNew = new Button("Nuevo");
        btnNew.getStyleClass().add("action-button");
        btnNew.setOnAction(e -> newProject(stage));

        Button btnOpen = new Button("Abrir");
        btnOpen.getStyleClass().add("action-button");
        btnOpen.setOnAction(e -> openProject(stage));

        Button btnSave = new Button("Guardar");
        btnSave.getStyleClass().add("action-button");
        btnSave.setOnAction(e -> saveProject(stage, false));

        Button btnSaveAs = new Button("Guardar como");
        btnSaveAs.getStyleClass().add("action-button");
        btnSaveAs.setOnAction(e -> saveProject(stage, true));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(btnNew, btnOpen, btnSave, btnSaveAs, spacer);
        return bar;
    }

    private void rebuildTabs(Stage stage, int selectedTabIndex) {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("main-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        metadataPane = new MetadataPane(book, stage);
        preliminariesPane = new PreliminariesPane(book, stage);
        chaptersPane = new ChaptersPane(book, stage);
        glossaryPane = new GlossaryPane(book);
        exportPane = new ExportPane(book, stage);

        Tab metaTab = new Tab("  Metadatos  ");
        metaTab.setContent(metadataPane);

        Tab prelimTab = new Tab("  Preliminares  ");
        prelimTab.setContent(preliminariesPane);
        prelimTab.setOnSelectionChanged(e -> {
            if (prelimTab.isSelected()) {
                preliminariesPane.reloadFromBook();
            }
        });

        Tab chaptersTab = new Tab("  Capítulos  ");
        chaptersTab.setContent(chaptersPane);
        chaptersTab.setOnSelectionChanged(e -> {
            if (chaptersTab.isSelected()) {
                chaptersPane.reloadFromBook();
            }
        });

        Tab glossaryTab = new Tab("  Glosario  ");
        glossaryTab.setContent(glossaryPane);
        glossaryTab.setOnSelectionChanged(e -> {
            if (glossaryTab.isSelected()) {
                glossaryPane.refresh();
            }
        });

        Tab exportTab = new Tab("  Exportar  ");
        exportTab.setContent(exportPane);
        exportTab.setOnSelectionChanged(e -> {
            if (exportTab.isSelected()) {
                exportPane.refresh();
            }
        });

        tabPane.getTabs().addAll(metaTab, prelimTab, chaptersTab, glossaryTab, exportTab);

        if (selectedTabIndex >= 0 && selectedTabIndex < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(selectedTabIndex);
        }

        root.setCenter(tabPane);
    }

    private void newProject(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Se creara un proyecto nuevo. Continuar?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        clearBook();
        currentProjectFile = null;
        rebuildTabs(stage, 0);
        updateTitle(stage);
    }

    private void openProject(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir proyecto");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Proyecto EPUB Builder", "*.epubbuilder", "*.epb")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            ProjectStorage.loadInto(book, file);
            currentProjectFile = file;
            rebuildTabs(stage, 0);
            updateTitle(stage);
        } catch (IOException ex) {
            showError("No se pudo abrir el proyecto", ex.getMessage());
        }
    }

    private void saveProject(Stage stage, boolean forceChoose) {
        File target = currentProjectFile;

        if (forceChoose || target == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Guardar proyecto");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Proyecto EPUB Builder", "*.epubbuilder")
            );
            chooser.setInitialFileName("mi-libro.epubbuilder");
            target = chooser.showSaveDialog(stage);
            if (target == null) return;

            if (!target.getName().contains(".")) {
                target = new File(target.getAbsolutePath() + ".epubbuilder");
            }
            currentProjectFile = target;
        }

        try {
            ProjectStorage.save(book, target);
            updateTitle(stage);
        } catch (IOException ex) {
            showError("No se pudo guardar el proyecto", ex.getMessage());
        }
    }

    private void tryRecoverAutosave(Stage stage) {
        File autosave = ProjectStorage.autosaveFile();
        if (!autosave.exists()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Se encontro un borrador recuperable. Quieres cargarlo?",
                ButtonType.YES,
                ButtonType.NO);
        alert.setHeaderText("Recuperacion automatica");

        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            ProjectStorage.loadInto(book, autosave);
            currentProjectFile = null;
            updateTitle(stage);
        } catch (IOException ex) {
            showError("No se pudo recuperar el borrador", ex.getMessage());
        }
    }

    private void startAutosave() {
        autosaveTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> autosaveSilently()));
        autosaveTimeline.setCycleCount(Animation.INDEFINITE);
        autosaveTimeline.play();
    }

    private void autosaveSilently() {
        try {
            ProjectStorage.save(book, ProjectStorage.autosaveFile());
        } catch (IOException ignored) {
        }
    }

    private void clearBook() {
        book.setTitle("");
        book.setAuthor("");
        book.setIllustrator("");
        book.setSynopsis("");
        book.setLanguage("es");
        book.setCoverPath("");
        book.getChapters().clear();
        book.getPreliminaryPages().clear();
    }

    private void updateTitle(Stage stage) {
        if (currentProjectFile == null) {
            stage.setTitle("EPUB Builder");
        } else {
            stage.setTitle("EPUB Builder - " + currentProjectFile.getName());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}