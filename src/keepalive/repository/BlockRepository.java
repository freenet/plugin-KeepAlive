package keepalive.repository;

import keepalive.Plugin;

import java.sql.*;

public class BlockRepository {

    private final Plugin plugin;

    private static BlockRepository instance;

    private static final String SQL_SAVE = "INSERT INTO Block (uri, data) VALUES (?, ?)";
    private static final String SQL_FIND = "SELECT data FROM Block WHERE uri = ?";
    private static final String SQL_UPDATE = "UPDATE Block SET data = ? WHERE uri = ?";
    private static final String SQL_DELETE = "DELETE FROM Block WHERE uri = ?;";
    private static final String SQL_LAST_ACCESS_DIFF = "SELECT TIMESTAMPDIFF(MILLISECOND, last_access, CURRENT_TIMESTAMP) FROM Block WHERE uri = ?";
    private static final String SQL_LAST_ACCESS_UPDATE = "UPDATE Block SET last_access = CURRENT_TIMESTAMP WHERE uri = ?";

    private BlockRepository(Plugin plugin) {
        this.plugin = plugin;
    }

    public static synchronized BlockRepository getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new BlockRepository(plugin);
        }
        return instance;
    }

    public void saveOrUpdate(String uri, byte[] data) {
        try (Connection connection = DB.getConnection();
             PreparedStatement findPreparedStatement = connection.prepareStatement(SQL_FIND)) {
            findPreparedStatement.setString(1, uri);
            ResultSet resultSet = findPreparedStatement.executeQuery();

            if (resultSet.next()) {
                try (PreparedStatement updatePreparedStatement = connection.prepareStatement(SQL_UPDATE)) {
                    updatePreparedStatement.setBytes(1, data);
                    updatePreparedStatement.setString(2, uri);
                    updatePreparedStatement.executeUpdate();
                }
            } else {
                try (PreparedStatement savePreparedStatement = connection.prepareStatement(SQL_SAVE)) {
                    savePreparedStatement.setString(1, uri);
                    savePreparedStatement.setBytes(2, data);
                    savePreparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.log(e.getMessage(), e);
        }
    }

    public byte[] findOne(String uri) {
        try (Connection connection = DB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND)) {
            preparedStatement.setString(1, uri);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                byte[] data = resultSet.getBytes("data");

                if (resultSet.next()) {
                    plugin.log("Not unique uri: " + uri);
                    return null;
                }

                return data;
            } else {
                return null;
            }
        } catch (SQLException e) {
            plugin.log(e.getMessage() + " " + uri, e);
        }

        return null;
    }

    public void delete(String uri) {
        try (Connection connection = DB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_DELETE)) {
            preparedStatement.setString(1, uri);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.log(e.getMessage() + " " + uri, e);
        }
    }

    public long lastAccessDiff(String uri) {
        try (Connection connection = DB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_LAST_ACCESS_DIFF)) {
            preparedStatement.setString(1, uri);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                long diff = resultSet.getLong(1);

                if (resultSet.next()) {
                    plugin.log("Not unique uri: " + uri);
                    return 0;
                }

                return diff;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            plugin.log(e.getMessage() + " " + uri, e);
        }

        return 0;
    }

    public void lastAccessUpdate(String uri) {
        try (Connection connection = DB.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_LAST_ACCESS_UPDATE)) {
            preparedStatement.setString(1, uri);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.log(e.getMessage() + " " + uri, e);
        }
    }
}
