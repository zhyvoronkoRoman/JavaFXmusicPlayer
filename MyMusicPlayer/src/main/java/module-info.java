module org.musicplayer.mymusicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires java.sql;
    requires javafx.media;
    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.logging.log4j; 

    opens org.musicplayer.mymusicplayer to javafx.fxml;
    exports org.musicplayer.mymusicplayer;
    exports org.musicplayer.mymusicplayer.Controllers;
    opens org.musicplayer.mymusicplayer.Controllers to javafx.fxml;
}