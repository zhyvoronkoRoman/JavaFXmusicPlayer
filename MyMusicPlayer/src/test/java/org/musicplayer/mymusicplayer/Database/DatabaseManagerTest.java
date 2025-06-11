package org.musicplayer.mymusicplayer.Database;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {
    private Connection testConn;

    @BeforeEach
    void setup() throws Exception {
        testConn = DriverManager.getConnection("jdbc:sqlite::memory:");
        // Створюємо таблицю songs
        try (Statement st = testConn.createStatement()) {
            st.execute("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, artist TEXT, file BLOB)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        testConn.close();
    }

    @Test
    void testEnsureHistoryTable() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        ResultSet rs = testConn.getMetaData().getTables(null, null, "history", null);
        assertTrue(rs.next());
    }

    @Test
    void testAddToHistoryAndLimit() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        // Додаємо 12 записів
        for (int i = 1; i <= 12; i++) {
            try (PreparedStatement st = testConn.prepareStatement("INSERT INTO songs (title, artist) VALUES (?, ?)")) {
                st.setString(1, "Song"+i);
                st.setString(2, "Artist"+i);
                st.executeUpdate();
                try (Statement lastIdStmt = testConn.createStatement()) {
                    ResultSet rs = lastIdStmt.executeQuery("SELECT last_insert_rowid()");
                    rs.next();
                    int songId = rs.getInt(1);
                    DatabaseManager.addToHistory(testConn, songId);
                }
            }
        }
        // Має залишитись лише 10 записів
        try (Statement st = testConn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM history");
            assertTrue(rs.next());
            assertEquals(10, rs.getInt(1));
        }
    }

    @Test
    void testCreateAndDropPlaylist() throws Exception {
        String playlist = "Playlist_Test";
        try (Statement st = testConn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + playlist + " (id INTEGER PRIMARY KEY AUTOINCREMENT, song_id INTEGER)");
        }
        ResultSet rs = testConn.getMetaData().getTables(null, null, playlist, null);
        assertTrue(rs.next());
        rs.close(); // Явно закриваю перед DROP
        // Видаляємо
        try (Statement st = testConn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + playlist);
        }
        rs = testConn.getMetaData().getTables(null, null, playlist, null);
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    void testGetConnectionAndCloseConnection() throws Exception {
        Connection conn = DatabaseManager.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        DatabaseManager.closeConnection();
        assertTrue(conn.isClosed());
    }

    @Test
    void testEnsureHistoryTableWithClosedConnection() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        conn.close();
        assertDoesNotThrow(() -> DatabaseManager.ensureHistoryTable(conn));
    }

    @Test
    void testAddToHistoryWithClosedConnection() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        conn.close();
        assertDoesNotThrow(() -> DatabaseManager.addToHistory(conn, 1));
    }

    @Test
    void testAddToHistoryWithInvalidSongId() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        // Додаємо некоректний songId (якого немає в songs)
        assertDoesNotThrow(() -> DatabaseManager.addToHistory(testConn, -100));
    }

    @Test
    void testAddToHistoryRemovesOldest() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        // Додаємо 11 записів
        for (int i = 1; i <= 11; i++) {
            try (PreparedStatement st = testConn.prepareStatement("INSERT INTO songs (title, artist) VALUES (?, ?)")) {
                st.setString(1, "Song"+i);
                st.setString(2, "Artist"+i);
                st.executeUpdate();
                try (Statement lastIdStmt = testConn.createStatement()) {
                    ResultSet rs = lastIdStmt.executeQuery("SELECT last_insert_rowid()");
                    rs.next();
                    int songId = rs.getInt(1);
                    DatabaseManager.addToHistory(testConn, songId);
                }
            }
        }
        // Має залишитись лише 10 записів
        try (Statement st = testConn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM history");
            rs.next();
            assertEquals(10, rs.getInt(1));
        }
    }

    @Test
    void testEnsureHistoryTableTwice() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        // Повторний виклик не повинен кидати винятків
        assertDoesNotThrow(() -> DatabaseManager.ensureHistoryTable(testConn));
    }

    @Test
    void testGetConnectionWithClosedConnection() throws Exception {
        Connection conn = DatabaseManager.getConnection();
        conn.close();
        Connection newConn = DatabaseManager.getConnection();
        assertNotNull(newConn);
        assertFalse(newConn.isClosed());
        DatabaseManager.closeConnection();
    }

    @Test
    void testCloseConnectionWithNullAndClosed() throws Exception {
        assertDoesNotThrow(DatabaseManager::closeConnection);
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        conn.close();
        assertDoesNotThrow(DatabaseManager::closeConnection);
    }

    @Test
    void testAddToHistoryNoDeleteIfLessThanOrEqual10() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        for (int i = 1; i <= 10; i++) {
            try (PreparedStatement st = testConn.prepareStatement("INSERT INTO songs (title, artist) VALUES (?, ?)")) {
                st.setString(1, "Song"+i);
                st.setString(2, "Artist"+i);
                st.executeUpdate();
                try (Statement lastIdStmt = testConn.createStatement()) {
                    ResultSet rs = lastIdStmt.executeQuery("SELECT last_insert_rowid()");
                    rs.next();
                    int songId = rs.getInt(1);
                    DatabaseManager.addToHistory(testConn, songId);
                }
            }
        }
        // Має залишитись 10 записів
        try (Statement st = testConn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM history");
            rs.next();
            assertEquals(10, rs.getInt(1));
        }
    }

    @Test
    void testAddToHistoryWithoutHistoryTable() throws Exception {
        // Не створюємо таблицю history
        try (PreparedStatement st = testConn.prepareStatement("INSERT INTO songs (title, artist) VALUES (?, ?)")) {
            st.setString(1, "SongX");
            st.setString(2, "ArtistX");
            st.executeUpdate();
        }
        try (Statement lastIdStmt = testConn.createStatement()) {
            ResultSet rs = lastIdStmt.executeQuery("SELECT last_insert_rowid()");
            rs.next();
            int songId = rs.getInt(1);
            assertDoesNotThrow(() -> DatabaseManager.addToHistory(testConn, songId));
        }
    }

    @Test
    void testEnsureHistoryTableWithNullConnection() {
        assertDoesNotThrow(() -> DatabaseManager.ensureHistoryTable(null));
    }

    @Test
    void testAddToHistoryWithNullConnection() {
        assertDoesNotThrow(() -> DatabaseManager.addToHistory(null, 1));
    }

    @Test
    void testAddToHistoryWithNullSongIdInHistory() throws Exception {
        DatabaseManager.ensureHistoryTable(testConn);
        try (Statement st = testConn.createStatement()) {
            st.execute("INSERT INTO history (song_id) VALUES (NULL)");
        }
        assertDoesNotThrow(() -> DatabaseManager.addToHistory(testConn, 1));
    }
} 