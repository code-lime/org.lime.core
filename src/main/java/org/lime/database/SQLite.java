package org.lime.database;

import org.lime.system.execute.ActionEx1;
import org.lime.system.execute.FuncEx1;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConnection;

import java.io.Closeable;
import java.io.File;
import java.sql.PreparedStatement;
import java.util.Properties;

public class SQLite implements Closeable {
    private final SQLiteConnection connection;
    public SQLite(File file) {
        this(file.getAbsolutePath());
    }
    public SQLite(String path) {
        String url = "jdbc:sqlite:" + path;
        try {
            connection = JDBC.createConnection(url, new Properties());
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void execute(String query, ActionEx1<PreparedStatement> execute) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            execute.invoke(statement);
        }
        catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
    public <T>T executeGet(String query, FuncEx1<PreparedStatement, T> execute) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            return execute.invoke(statement);
        }
        catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}
