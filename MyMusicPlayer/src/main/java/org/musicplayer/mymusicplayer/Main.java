package org.musicplayer.mymusicplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import javafx.stage.Stage;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {
    private static final Logger logger = LogManager.getLogger(Main.class);
    @Override
    public void start(Stage playerStage) throws IOException {
        logger.info("Program started.");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("music-player.fxml"));
            Scene playerScene = new Scene(fxmlLoader.load());
            javafx.application.Platform.runLater(() -> {
                playerStage.setTitle("MP3 плеєр Романа Живоронка");
                playerStage.setScene(playerScene);
                playerStage.show();
            });
        } catch (Exception e) {
            logger.error("Critical error in application.", e);
        }
        logger.info("Program finished.");
    }
    public static void main(String[] args) {
        launch();
    }
}
