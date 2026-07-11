package erydev.eryStaffDefender.storage;

import erydev.eryStaffDefender.EryStaffDefender;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class KeyDatabase {

    private final EryStaffDefender plugin;
    private Connection connection;
    private final SecureRandom random = new SecureRandom();

    public KeyDatabase(EryStaffDefender plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        String fileName = plugin.getConfig().getString("storage.file", "keys.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (PreparedStatement st = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS esd_keys (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "name TEXT, " +
                        "salt TEXT NOT NULL, " +
                        "hash TEXT NOT NULL, " +
                        "skip INTEGER NOT NULL DEFAULT 0)")) {
            st.executeUpdate();
        }
        try (PreparedStatement st = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS esd_sessions (" +
                        "uuid TEXT NOT NULL, " +
                        "ip TEXT NOT NULL, " +
                        "last_auth INTEGER NOT NULL, " +
                        "PRIMARY KEY (uuid, ip))")) {
            st.executeUpdate();
        }
    }

    public synchronized void saveSession(UUID uuid, String ip, long time) {
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO esd_sessions (uuid, ip, last_auth) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid, ip) DO UPDATE SET last_auth = excluded.last_auth")) {
            st.setString(1, uuid.toString());
            st.setString(2, ip);
            st.setLong(3, time);
            st.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("saveSession failed: " + e.getMessage());
        }
    }

    public synchronized long getSessionTime(UUID uuid, String ip) {
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT last_auth FROM esd_sessions WHERE uuid = ? AND ip = ?")) {
            st.setString(1, uuid.toString());
            st.setString(2, ip);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getLong("last_auth") : 0L;
            }
        } catch (SQLException e) {
            return 0L;
        }
    }

    public synchronized void clearSessions(UUID uuid) {
        try (PreparedStatement st = connection.prepareStatement("DELETE FROM esd_sessions WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            st.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("clearSessions failed: " + e.getMessage());
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public synchronized boolean hasKey(UUID uuid) {
        try (PreparedStatement st = connection.prepareStatement("SELECT 1 FROM esd_keys WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("hasKey failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized void setKey(UUID uuid, String name, String key) {
        String salt = newSalt();
        String hash = hash(key, salt);
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO esd_keys (uuid, name, salt, hash, skip) VALUES (?, ?, ?, ?, 0) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, salt = excluded.salt, hash = excluded.hash")) {
            st.setString(1, uuid.toString());
            st.setString(2, name);
            st.setString(3, salt);
            st.setString(4, hash);
            st.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("setKey failed: " + e.getMessage());
        }
    }

    public synchronized boolean deleteKey(UUID uuid) {
        try (PreparedStatement st = connection.prepareStatement("DELETE FROM esd_keys WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("deleteKey failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean checkKey(UUID uuid, String key) {
        try (PreparedStatement st = connection.prepareStatement("SELECT salt, hash FROM esd_keys WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String salt = rs.getString("salt");
                String hash = rs.getString("hash");
                return constantEquals(hash, hash(key, salt));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("checkKey failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean isSkip(UUID uuid) {
        try (PreparedStatement st = connection.prepareStatement("SELECT skip FROM esd_keys WHERE uuid = ?")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() && rs.getInt("skip") == 1;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized void setSkip(UUID uuid, String name, boolean skip) {
        try (PreparedStatement st = connection.prepareStatement(
                "INSERT INTO esd_keys (uuid, name, salt, hash, skip) VALUES (?, ?, '', '', ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET skip = excluded.skip, name = excluded.name")) {
            st.setString(1, uuid.toString());
            st.setString(2, name);
            st.setInt(3, skip ? 1 : 0);
            st.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("setSkip failed: " + e.getMessage());
        }
    }

    private String newSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return toHex(bytes);
    }

    private String hash(String key, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest((salt + key).getBytes("UTF-8"));
            return toHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean constantEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
