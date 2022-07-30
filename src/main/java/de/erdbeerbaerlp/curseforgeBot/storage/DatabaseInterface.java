package de.erdbeerbaerlp.curseforgeBot.storage;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.Root;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DatabaseInterface implements AutoCloseable {
    public final StatusThread status;
    private Connection conn;
    public Gson gson = new GsonBuilder().create();

    public DatabaseInterface() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connect();
        if (conn == null) {
            throw new SQLException();
        }
        status = new StatusThread();
        status.start();
        if (conn == null) {
            throw new SQLException();
        }
        runUpdate("CREATE TABLE IF NOT EXISTS `cfcache` (\n" +
                "  `latestFileID` bigint NOT NULL DEFAULT '0',\n" +
                "  `projectid` bigint NOT NULL,\n" +
                "  PRIMARY KEY (`projectid`),\n" +
                "  UNIQUE KEY `cfcache_projectid_uindex` (`projectid`)\n" +
                ");");
        runUpdate("CREATE TABLE IF NOT EXISTS `channels` (\n" +
                "  `channeldata` json NOT NULL,\n" +
                "  `channelid` bigint NOT NULL,\n" +
                "  PRIMARY KEY (`channelid`),\n" +
                "  UNIQUE KEY `channels_channelid_uindex` (`channelid`)\n" +
                ");");
        runUpdate("alter table channels modify column channeldata json default ('" + gson.toJson(new Root()) + "') not null;");
    }

    private void connect() throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://" + Config.instance().database.ip + ":" + Config.instance().database.port + "/" + Config.instance().database.dbName, Config.instance().database.username, Config.instance().database.password);
    }

    private boolean isConnected() {
        try {
            return conn.isValid(10);
        } catch (SQLException e) {
            return false;
        }
    }

    public CurseforgeProject.CFChannel deleteChannelFromProject(long projectID, long channel) {
        final CurseforgeProject.CFChannel chan = getOrCreateCFChannel(channel);
        final ArrayList<Long> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.remove(projectID);
        chan.data.projects = projs.toArray(new Long[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateCFChannel(channel);
    }

    public void addChannel(CurseforgeProject.CFChannel channel) {
        runUpdate("INSERT INTO channels (channelid,channeldata) values (" + channel.channelID + ",'" + gson.toJson(channel.data) + "')");
    }

    public void delChannel(long serverID) {
        runUpdate("DELETE FROM channels WHERE channelid = " + serverID);
    }

    public int getLatestFile(long projectid) throws SQLException {
        final ResultSet query = query("SELECT latestFileID FROM cfcache WHERE projectid = " + projectid + ";");
        if (query.next()) {
            return query.getInt(1);
        }
        return -1;
    }

    public void updateCache(long projectid, long fileid) {
        runUpdate("REPLACE INTO cfcache (projectid,latestFileID) VALUES(" + projectid + "," + fileid + ")");
    }

    public CurseforgeProject.CFChannel addChannelToProject(long projectID, long channel) {
        final CurseforgeProject.CFChannel chan = getOrCreateCFChannel(channel);
        final ArrayList<Long> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.add(projectID);
        chan.data.projects = projs.toArray(new Long[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateCFChannel(channel);
    }

    public class StatusThread extends Thread {
        private boolean alive = true;

        public boolean isDBAlive() {
            return alive;
        }

        @Override
        public void run() {
            while (true) {
                alive = DatabaseInterface.this.isConnected();
                if (!alive) try {
                    System.err.println("Attempting Database reconnect...");
                    DatabaseInterface.this.connect();
                } catch (SQLException e) {
                    System.err.println("Failed to reconnect to database: " + e.getMessage());
                    try {
                        TimeUnit.SECONDS.sleep(15);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    public CurseforgeProject.CFChannel getOrCreateCFChannel(long channelID) {
        try (final ResultSet res = query("SELECT channelid,channeldata FROM channels WHERE channelid=" + channelID)) {
            while (res != null && res.next()) {
                final Root json = gson.fromJson(res.getString(2), Root.class);
                return new CurseforgeProject.CFChannel(res.getLong(1), json);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new CurseforgeProject.CFChannel(channelID, new Root());
    }

    public ArrayList<CurseforgeProject.CFChannel> getAllChannels() {
        final ArrayList<CurseforgeProject.CFChannel> channels = new ArrayList<>();
        try (final ResultSet res = query("SELECT channelid,channeldata FROM channels")) {
            while (res != null && res.next()) {
                final Root json = gson.fromJson(res.getString(2), Root.class);
                channels.add(new CurseforgeProject.CFChannel(res.getLong(1), json));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return channels;
    }

    private void runUpdate(final String sql) {
        try (final Statement statement = conn.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ResultSet query(final String sql) {
        try {
            final Statement statement = conn.createStatement();
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void close() throws Exception {
        conn.close();
    }

    public boolean isNewFile(CFMod proj) throws SQLException {
        return Main.ifa.getLatestFile(proj.id) < proj.latestFilesIndexes[0].fileId;
    }
}
