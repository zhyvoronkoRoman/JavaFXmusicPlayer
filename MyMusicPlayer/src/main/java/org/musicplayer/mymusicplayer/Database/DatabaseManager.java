package org.musicplayer.mymusicplayer.Database;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:src/music.db";
    private static Connection connection = null;
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ensureHistoryTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, song_id INTEGER, played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addToHistory(Connection conn, int songId) {
        try {
            // Додаємо новий запис
            String insertSQL = "INSERT INTO history (song_id) VALUES (?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setInt(1, songId);
                insertStmt.executeUpdate();
            }
            removeOldHistoryRecords(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Винесена логіка видалення старих записів
    private static void removeOldHistoryRecords(Connection conn) {
        String countSQL = "SELECT COUNT(*) FROM history";
        try (PreparedStatement countStmt = conn.prepareStatement(countSQL)) {
            ResultSet rs = countStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 10) {
                String deleteSQL = "DELETE FROM history WHERE id = (SELECT id FROM history ORDER BY id ASC LIMIT 1)";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {
                    deleteStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureHistoryTable() {
        try (Connection conn = getConnection()) {
            ensureHistoryTable(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addToHistory(int songId) {
        try (Connection conn = getConnection()) {
            addToHistory(conn, songId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 