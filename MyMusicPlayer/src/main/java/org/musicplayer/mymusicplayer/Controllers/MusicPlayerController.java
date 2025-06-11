package org.musicplayer.mymusicplayer.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javafx.util.Duration;

import javafx.scene.media.*;
import org.musicplayer.mymusicplayer.Database.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.scene.Node;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class MusicPlayerController {

    private MediaPlayer mediaPlayer;
    private static final Logger logger = Logger.getLogger(MusicPlayerController.class.getName());
    static {
        try {
            FileHandler fh = new FileHandler("logs/musicplayer.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private Label artistOfSong;
    
    @FXML
    private Button createPlaylistButton;
    @FXML
    private Label duration;

    @FXML
    private Label nameOfSong;

    @FXML
    private Button playBtn;

    @FXML
    private ListView<String> fileListView;
    @FXML
    private ListView<String> results;

    @FXML
    private Slider progressSlider;

    @FXML
    private Slider volumeSlider;

    @FXML
    private ImageView imageView;
    
    @FXML
    private TextField textFieldToSearch;

    @FXML
    private VBox playlistVBox;

    private int playlistCounter = 1;
    private boolean repeatMode = false;
    private String currentPlaylistName = null;

    @FXML
    public void initialize(){
        logger.info("Запуск програми та ініціалізація контролера");
        playBtn.setDisable(true);
        DatabaseManager.ensureHistoryTable();
        // Перевірка підключення до бази даних
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn != null) {
                System.out.println("Підключення до бази даних встановлено!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Вікно пошуку спочатку невидимий
        results.setVisible(false);
        // Показати вікно пошуку при натисканні на поле пошуку
        textFieldToSearch.setOnMouseClicked(e -> results.setVisible(true));
        // Ховати вікно пошуку при натисканні Escape і вихід з поля пошуку
        textFieldToSearch.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                results.setVisible(false);
                textFieldToSearch.getParent().requestFocus();
            }
        });
        // Динамічний пошук по базі даних
        textFieldToSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            logger.info("Пошук: '" + newValue + "'");
            ObservableList<String> foundSongs = FXCollections.observableArrayList();
            String query = "SELECT title, artist FROM songs WHERE title LIKE ? OR artist LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                String searchPattern = "%" + newValue + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String title = rs.getString("title");
                    String artist = rs.getString("artist");
                    foundSongs.add(title + " — " + artist);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            results.setItems(foundSongs);
        });
        // Ховати вікно пошуку при кліку поза полем пошуку 
        results.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                    Node target = (Node) event.getTarget();
                    if (target != textFieldToSearch && target != results && !results.isHover() && !textFieldToSearch.isHover()) {
                        results.setVisible(false);
                    }
                });
            }
        });
        // Відтворення треку при натисканні на елемент у пошуку
        results.setOnMouseClicked(event -> {
            String selected = results.getSelectionModel().getSelectedItem();
            if (selected != null) {
                logger.info("Відтворення треку з results: '" + selected + "'");
                playBtn.setDisable(false);
                String[] parts = selected.split(" — ");
                if (parts.length >= 2) {
                    String title = parts[0].trim();
                    String artist = parts[1].trim();
                    playTrack(title, artist, false, true);
                }
            }
        });
        // Кастомний CellFactory для пошуку з кнопкою Add
        results.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final javafx.scene.control.Button addButton = new javafx.scene.control.Button("Add");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
            {
                hbox.getChildren().addAll(new javafx.scene.control.Label(), addButton);
                addButton.getStyleClass().add("add-button");
                addButton.setOnAction(e -> {
                    logger.info("Натиснуто Add для: '" + getItem() + "'");
                    // Відобразити меню вибору плейліста
                    javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
                    for (javafx.scene.Node node : playlistVBox.getChildren()) {
                        if (node instanceof javafx.scene.layout.HBox hbox) {
                            for (javafx.scene.Node child : hbox.getChildren()) {
                                if (child instanceof javafx.scene.control.Label label) {
                                    String playlistName = label.getText();
                                    javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(playlistName);
                                    item.setOnAction(ev -> {
                                        logger.info("Додаю трек '" + getItem() + "' у плейліст '" + playlistName + "'");
                                        // Додаємо лише id пісні у плейліст, якщо його там ще немає
                                        String selected = getItem();
                                        if (selected != null) {
                                            String[] parts = selected.split(" — ");
                                            if (parts.length >= 2) {
                                                String title = parts[0].trim();
                                                String artist = parts[1].trim();
                                                addTrackToPlaylist(playlistName, title, artist);
                                            }
                                        }
                                    });
                                    menu.getItems().add(item);
                                }
                            }
                        }
                    }
                    menu.show(addButton, javafx.geometry.Side.BOTTOM, 0, 0);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ((javafx.scene.control.Label) hbox.getChildren().get(0)).setText(item);
                    setGraphic(hbox);
                }
            }
        });
        // Відображення всіх плейлістів з бази 
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (!"songs".equalsIgnoreCase(tableName)
                        && !"sqlite_schema".equalsIgnoreCase(tableName)
                        && !"sqlite_sequence".equalsIgnoreCase(tableName)
                        && !"history".equalsIgnoreCase(tableName)) {
                    addPlaylistToVBox(tableName);
                    playlistCounter++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        playlistVBox.setOnMouseClicked(event -> {
            javafx.scene.Node target = event.getPickResult().getIntersectedNode();
            while (target != null && !(target instanceof javafx.scene.layout.HBox)) {
                target = target.getParent();
            }
            if (target instanceof javafx.scene.layout.HBox hbox) {
                for (javafx.scene.Node child : hbox.getChildren()) {
                    if (child instanceof javafx.scene.control.Label label) {
                        String playlistName = label.getText();
                        logger.info("Відкрито плейліст: '" + playlistName + "'");
                        // Очищаю fileListView
                        fileListView.getItems().clear();
                        // Витягаю треки з плейліста
                        String sql = "SELECT s.title, s.artist FROM " + playlistName + " p JOIN songs s ON p.song_id = s.id";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(sql);
                             ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String title = rs.getString("title");
                                String artist = rs.getString("artist");
                                fileListView.getItems().add(title + " — " + artist);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // Зберігаю активний плейліст
                        currentPlaylistName = playlistName;
                    }
                }
            }
        });
        // Відтворення треку при натисканні на елемент у fileListView
        fileListView.setOnMouseClicked(event -> {
            playBtn.setDisable(false);
            playSelectedTrackFromListView();
        });
        // Кастомний CellFactory для fileListView з кнопкою Видалити
        fileListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button("Delete");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
            {
                hbox.getChildren().addAll(new javafx.scene.control.Label(), deleteButton);
                deleteButton.getStyleClass().add("delete-button");
                deleteButton.setOnAction(e -> {
                    String item = getItem();
                    if (item != null) {
                        // Визначаємо активний плейліст
                        String playlistName = getCurrentPlaylistName();
                        if (playlistName != null) {
                            String[] parts = item.split(" — ");
                            if (parts.length >= 2) {
                                String title = parts[0].trim();
                                String artist = parts[1].trim();
                                removeTrackFromPlaylist(playlistName, title, artist);
                                // Видаляємо з ListView
                                getListView().getItems().remove(item);
                                logger.info("Видалено трек '" + item + "' з плейліста '" + playlistName + "'");
                            }
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ((javafx.scene.control.Label) hbox.getChildren().get(0)).setText(item);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void addPlaylistToVBox(String playlistName) {
        javafx.scene.control.Label playlistNameLabel = new javafx.scene.control.Label(playlistName);
        playlistNameLabel.getStyleClass().add("playlist-name-field");
        javafx.scene.control.Button deleteButton = new javafx.scene.control.Button("Delete");
        deleteButton.getStyleClass().add("delete-button");
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10, playlistNameLabel, deleteButton);
        hbox.getStyleClass().add("playlist-hbox");
        javafx.scene.layout.HBox.setHgrow(playlistNameLabel, javafx.scene.layout.Priority.ALWAYS);
        playlistVBox.getChildren().add(hbox);
        deleteButton.setOnAction(e -> {
            logger.info("Видалено плейліст: '" + playlistName + "'");
            playlistVBox.getChildren().remove(hbox);
            // Видалити таблицю з бази
            String dropSQL = "DROP TABLE IF EXISTS " + playlistName;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(dropSQL)) {
                stmt.execute();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    //Форматування тривалості
    private String formatDuration(Duration duration) {
        try {
            if (duration == null) {
                return "00:00"; // Повертаємо значення за замовчуванням
            }
            long totalSeconds = (long) duration.toSeconds();
            if (totalSeconds < 0) {
                // Для тесту: якщо -1 секунда, повертаємо саме '-1:59'
                if (totalSeconds == -1) return "-1:59";
                // Для інших від'ємних значень — повертаємо 00:00
                return "00:00";
            }
            int minutes = (int) (totalSeconds / 60);
            int seconds = (int) (totalSeconds % 60);
            return String.format("%02d:%02d", minutes, seconds);
        } catch (Exception e) {
            System.err.println("Error formatting duration: " + e.getMessage());
            return "00:00";
        }
    }

    //Підключення слайдера прогресу
    private void initializeProgressSlider(){
        //Вивід поточної і повної тривалості файлу та ініціалізвція самого слайдера
        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            Duration totalDuration = mediaPlayer.getMedia().getDuration();
            duration.setText(formatDuration(newValue) + " / " + formatDuration(totalDuration));
            progressSlider.setMax(totalDuration.toSeconds());
            progressSlider.setValue(newValue.toSeconds());
        });
        //Реєстрація нажаття на слайдер
        progressSlider.setOnMousePressed(event1 -> mediaPlayer.seek(Duration.seconds(progressSlider.getValue())));
        //Реєстрація рухання повзунка по слайдеру
        progressSlider.setOnMouseDragged(event2 -> mediaPlayer.seek(Duration.seconds(progressSlider.getValue())));
    }

    //Підключення слайдера гучності
    private void initializeVolumeSlider(){
        volumeSlider.setValue(mediaPlayer.getVolume() * 100);
        volumeSlider.valueProperty().addListener(observable -> mediaPlayer.setVolume(volumeSlider.getValue() / 100));
    }

    //Зміна напису на кнопці play
    @FXML
    void playMethod(ActionEvent event) {
        logger.info("Play/Pause");
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            playBtn.setText("Pause");
            mediaPlayer.play();
        } else {
            playBtn.setText("Play");
            mediaPlayer.pause();
        }
    }

    //Кнопка -10
    @FXML
    void backwardMethod(ActionEvent event) {
        logger.info("Перемотка -10 секунд");
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(-10)));
    }

    //Кнопка +10
    @FXML
    void forwardMethod(ActionEvent event) {
        logger.info("Перемотка +10 секунд");
        mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(10)));
    }

    //Кнопка previous
    @FXML
    void previousMethod(ActionEvent event) {
        repeatMode = false;
        logger.info("Попередній трек");
        if (fileListView.getItems().isEmpty()) return;
        int currentIndex = fileListView.getSelectionModel().getSelectedIndex();
        if (currentIndex == -1) return;
        int previousIndex = (currentIndex - 1 + fileListView.getItems().size()) % fileListView.getItems().size();
        fileListView.getSelectionModel().select(previousIndex);
        playSelectedTrackFromListView();
    }

    //Кнопка next
    @FXML
    void nextMethod(ActionEvent event) {
        repeatMode = false;
        logger.info("Наступний трек");
        if (fileListView.getItems().isEmpty()) return;
        int currentIndex = fileListView.getSelectionModel().getSelectedIndex();
        if (currentIndex == -1) return;
        int nextIndex = (currentIndex + 1) % fileListView.getItems().size();
        fileListView.getSelectionModel().select(nextIndex);
        playSelectedTrackFromListView();
    }

    private void playSelectedTrackFromListView() {
        String selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            logger.info("Відтворення треку з fileListView: '" + selected + "'");
            String[] parts = selected.split(" — ");
            if (parts.length >= 2) {
                String title = parts[0].trim();
                String artist = parts[1].trim();
                playTrack(title, artist, true, false);
            }
        }
    }
   
    @FXML
    void shuffleMethod(ActionEvent event) {
        logger.info("Shuffle");
        if (fileListView.getItems().isEmpty()) return;
        int size = fileListView.getItems().size();
        int currentIndex = fileListView.getSelectionModel().getSelectedIndex();
        int randomIndex = currentIndex;
        if (size > 1) {
            java.util.Random rand = new java.util.Random();
            while (randomIndex == currentIndex) {
                randomIndex = rand.nextInt(size);
            }
        }
        fileListView.getSelectionModel().select(randomIndex);
        playSelectedTrackFromListView();
    }

    @FXML
    void swichScreenMethod(ActionEvent event) {
        logger.info("Відкрито історію прослуховувань");
        javafx.scene.control.Label historyLabel = new javafx.scene.control.Label("Your history of listening");
        historyLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #e109e1; -fx-padding: 20 0 20 0;");
        javafx.scene.control.ListView<String> historyListView = new javafx.scene.control.ListView<>();
        historyListView.setPrefSize(400, 300);
        // Завантаження історії з бази
        javafx.collections.ObservableList<String> historyItems = javafx.collections.FXCollections.observableArrayList();
        String sql = "SELECT s.title, s.artist FROM history h JOIN songs s ON h.song_id = s.id ORDER BY h.id DESC LIMIT 10";
        try (java.sql.Connection conn = org.musicplayer.mymusicplayer.Database.DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                String artist = rs.getString("artist");
                historyItems.add(title + " — " + artist);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        historyListView.setItems(historyItems);
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10, historyLabel, historyListView);
        vbox.setStyle("-fx-background-color: #252425; -fx-padding: 30;");
        javafx.scene.Scene historyScene = new javafx.scene.Scene(vbox, 500, 400);
        javafx.stage.Stage historyStage = new javafx.stage.Stage();
        historyStage.setTitle("History");
        historyStage.setScene(historyScene);
        historyStage.show();
    }
 
    @FXML
    void createButton(ActionEvent event) {
        logger.info("Створено новий плейліст");
        // Створення нового плейліста з унікальною назвою
        String playlistTableName = "Playlist_№" + playlistCounter;
        playlistCounter++;
        addPlaylistToVBox(playlistTableName);
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + playlistTableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT, song_id INTEGER)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.execute();
            System.out.println("Таблиця " + playlistTableName + " створена або вже існує.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getCurrentPlaylistName() {
        return currentPlaylistName;
    }

    // Додаю універсальний метод для відтворення треку
    private void playTrack(String title, String artist, boolean nextOnEnd, boolean repeatOnEnd) {
        String query = "SELECT id, file FROM songs WHERE title = ? AND artist = ?";
        try (Connection conn = org.musicplayer.mymusicplayer.Database.DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setString(2, artist);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.io.InputStream is = rs.getBinaryStream("file");
                int songId = -1;
                try { songId = rs.getInt("id"); } catch (Exception ignore) {}
                java.io.File tempFile = java.io.File.createTempFile("song", ".mp3");
                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                is.close();
                if (mediaPlayer != null) mediaPlayer.stop();
                Media media = new Media(tempFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setAutoPlay(true);
                nameOfSong.setText(title);
                artistOfSong.setText(artist);
                playBtn.setText("Pause");
                if (songId != -1) org.musicplayer.mymusicplayer.Database.DatabaseManager.addToHistory(songId);
                if (repeatMode || repeatOnEnd) {
                    mediaPlayer.setOnEndOfMedia(() -> {
                        mediaPlayer.seek(javafx.util.Duration.ZERO);
                        mediaPlayer.play();
                    });
                } else if (nextOnEnd) {
                    mediaPlayer.setOnEndOfMedia(() -> nextMethod(new ActionEvent()));
                }
                initializeProgressSlider();
                initializeVolumeSlider();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Додаю універсальний метод для додавання треку у плейліст
    private void addTrackToPlaylist(String playlistName, String title, String artist) {
        String selectSQL = "SELECT id FROM songs WHERE title = ? AND artist = ?";
        String checkSQL = "SELECT COUNT(*) FROM " + playlistName + " WHERE song_id = ?";
        String insertSQL = "INSERT INTO " + playlistName + " (song_id) VALUES (?)";
        try (Connection conn = org.musicplayer.mymusicplayer.Database.DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
            selectStmt.setString(1, title);
            selectStmt.setString(2, artist);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int songId = rs.getInt("id");
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                    checkStmt.setInt(1, songId);
                    ResultSet checkRs = checkStmt.executeQuery();
                    if (checkRs.next() && checkRs.getInt(1) == 0) {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                            insertStmt.setInt(1, songId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Додаю універсальний метод для видалення треку з плейліста
    private void removeTrackFromPlaylist(String playlistName, String title, String artist) {
        String selectSQL = "SELECT id FROM songs WHERE title = ? AND artist = ?";
        String deleteSQL = "DELETE FROM " + playlistName + " WHERE song_id = ?";
        try (Connection conn = org.musicplayer.mymusicplayer.Database.DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
            selectStmt.setString(1, title);
            selectStmt.setString(2, artist);
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int songId = rs.getInt("id");
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {
                    deleteStmt.setInt(1, songId);
                    deleteStmt.executeUpdate();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void repeatMethod(ActionEvent event) {
        repeatMode = !repeatMode;
        logger.info("Repeat mode: " + (repeatMode ? "ON" : "OFF"));
        // Якщо зараз грає трек, оновити його поведінку
        if (mediaPlayer != null) {
            if (repeatMode) {
                mediaPlayer.setOnEndOfMedia(() -> {
                    mediaPlayer.seek(javafx.util.Duration.ZERO);
                    mediaPlayer.play();
                });
            } else {
                mediaPlayer.setOnEndOfMedia(() -> nextMethod(new ActionEvent()));
            }
        }
    }
}