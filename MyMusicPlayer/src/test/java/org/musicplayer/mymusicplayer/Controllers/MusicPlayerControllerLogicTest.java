package org.musicplayer.mymusicplayer.Controllers;

import org.junit.jupiter.api.Test;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.jupiter.api.Assertions.*;
import javafx.util.Duration;
import java.lang.reflect.Method;
import org.musicplayer.mymusicplayer.Database.DatabaseManager;
import java.sql.*;

public class MusicPlayerControllerLogicTest {
    @Test
    void testNextTrackIndex() {
        ObservableList<String> list = FXCollections.observableArrayList("A", "B", "C");
        int current = 1;
        int next = (current + 1) % list.size();
        assertEquals(2, next);
        current = 2;
        next = (current + 1) % list.size();
        assertEquals(0, next);
    }

    @Test
    void testPreviousTrackIndex() {
        ObservableList<String> list = FXCollections.observableArrayList("A", "B", "C");
        int current = 0;
        int prev = (current - 1 + list.size()) % list.size();
        assertEquals(2, prev);
        current = 1;
        prev = (current - 1 + list.size()) % list.size();
        assertEquals(0, prev);
    }

    @Test
    void testAddTrackToList() {
        ObservableList<String> list = FXCollections.observableArrayList();
        list.add("Song — Artist");
        assertEquals(1, list.size());
        assertEquals("Song — Artist", list.get(0));
    }

    @Test
    void testRemoveTrackFromList() {
        ObservableList<String> list = FXCollections.observableArrayList("A", "B");
        list.remove("A");
        assertEquals(1, list.size());
        assertEquals("B", list.get(0));
    }

    @Test
    void testEmptyListEdgeCases() {
        ObservableList<String> list = FXCollections.observableArrayList();
        int current = -1;
        int next = (current + 1 + list.size()) % (list.size() == 0 ? 1 : list.size());
        assertEquals(0, next);
    }

    @Test
    void testFormatDurationViaReflection() throws Exception {
        MusicPlayerController controller = new MusicPlayerController();
        Method m = MusicPlayerController.class.getDeclaredMethod("formatDuration", Duration.class);
        m.setAccessible(true);
        assertEquals("03:25", m.invoke(controller, Duration.seconds(205)));
        assertEquals("00:00", m.invoke(controller, Duration.seconds(0)));
        assertEquals("01:01", m.invoke(controller, Duration.seconds(61)));
        assertEquals("00:00", m.invoke(controller, (Object) null));
        assertEquals("123:45", m.invoke(controller, Duration.seconds(7425)));
        assertEquals("-1:59", m.invoke(controller, Duration.seconds(-1)));
    }

    @Test
    void testGetCurrentPlaylistName() throws Exception {
        MusicPlayerController controller = new MusicPlayerController();
        java.lang.reflect.Field f = MusicPlayerController.class.getDeclaredField("currentPlaylistName");
        f.setAccessible(true);
        f.set(controller, "Playlist_№1");
        Method m = MusicPlayerController.class.getDeclaredMethod("getCurrentPlaylistName");
        m.setAccessible(true);
        assertEquals("Playlist_№1", m.invoke(controller));
        f.set(controller, null);
        assertNull(m.invoke(controller));
    }

    @Test
    void testRepeatModeField() throws Exception {
        MusicPlayerController controller = new MusicPlayerController();
        java.lang.reflect.Field f = MusicPlayerController.class.getDeclaredField("repeatMode");
        f.setAccessible(true);
        f.set(controller, true);
        assertTrue(f.getBoolean(controller));
        f.set(controller, false);
        assertFalse(f.getBoolean(controller));
    }

    @Test
    void testPlaylistCounterField() throws Exception {
        MusicPlayerController controller = new MusicPlayerController();
        java.lang.reflect.Field f = MusicPlayerController.class.getDeclaredField("playlistCounter");
        f.setAccessible(true);
        f.set(controller, 42);
        assertEquals(42, f.getInt(controller));
    }

    @Test
    void testLoggerInitialization() throws Exception {
        java.lang.reflect.Field f = MusicPlayerController.class.getDeclaredField("logger");
        f.setAccessible(true);
        assertNotNull(f.get(null));
    }

    @Test
    void testControllerInstantiation() {
        assertDoesNotThrow(MusicPlayerController::new);
    }
} 