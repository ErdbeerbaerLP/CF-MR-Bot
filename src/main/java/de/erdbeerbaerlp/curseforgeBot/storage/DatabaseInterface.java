package de.erdbeerbaerlp.curseforgeBot.storage;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erdbeerbaerlp.cfcore.json.CFMod;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.DCChannel;
import de.erdbeerbaerlp.curseforgeBot.storage.json.Root;
import masecla.modrinth4j.model.project.Project;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
        if (conn == null) {
            throw new SQLException();
        }
        runUpdate("CREATE TABLE IF NOT EXISTS `cfcache` (\n" +
                "  `latestFileID` bigint NOT NULL DEFAULT '0',\n" +
                "  `projectid` bigint NOT NULL,\n" +
                "  PRIMARY KEY (`projectid`),\n" +
                "  UNIQUE KEY `cfcache_projectid_uindex` (`projectid`)\n" +
                ");");
        runUpdate("CREATE TABLE IF NOT EXISTS `mrcache` (\n" +
                "  `latestFileTime` bigint NOT NULL DEFAULT '0',\n" +
                "  `projectid` VARCHAR(8) NOT NULL,\n" +
                "  PRIMARY KEY (`projectid`),\n" +
                "  UNIQUE KEY `mrcache_projectid_uindex` (`projectid`)\n" +
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

    public DCChannel deleteChannelFromCFProject(long projectID, long channel) {
        final DCChannel chan = getOrCreateDCChannel(channel);
        final ArrayList<Long> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.remove(projectID);
        chan.data.projects = projs.toArray(new Long[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateDCChannel(channel);
    }

    public DCChannel deleteChannelFromMRProject(String projectID, long channel) {
        final DCChannel chan = getOrCreateDCChannel(channel);
        final ArrayList<String> projs = new ArrayList<>(List.of(chan.data.mrProjects));
        projs.remove(projectID);
        chan.data.mrProjects = projs.toArray(new String[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateDCChannel(channel);
    }

    public void addChannel(DCChannel channel) {
        runUpdate("INSERT INTO channels (channelid,channeldata) values (" + channel.channelID + ",'" + gson.toJson(channel.data) + "')");
    }

    public void delChannel(long serverID) {
        runUpdate("DELETE FROM channels WHERE channelid = " + serverID);
    }

    public int getLatestCFFile(long projectid) throws SQLException {
        final ResultSet query = query("SELECT latestFileID FROM cfcache WHERE projectid = " + projectid + ";");
        if (query.next()) {
            return query.getInt(1);
        }
        return -1;
    }

    public long getLatestMRFile(String projectid) throws SQLException {
        final ResultSet query = query("SELECT latestFileTime FROM mrcache WHERE projectid = '" + projectid + "';");
        if (query.next()) {
            return query.getLong(1);
        }
        return -1;
    }

    public void updateCFCache(long projectid, long fileid) {
        runUpdate("REPLACE INTO cfcache (projectid,latestFileID) VALUES(" + projectid + "," + fileid + ")");
    }

    public void updateMRCache(String projectid, long fileid) {
        runUpdate("REPLACE INTO mrcache (projectid,latestFileTime) VALUES('" + projectid + "'," + fileid + ")");
    }

    public DCChannel addChannelToCFProject(long projectID, long channel) {
        final DCChannel chan = getOrCreateDCChannel(channel);
        final ArrayList<Long> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.add(projectID);
        chan.data.projects = projs.toArray(new Long[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateDCChannel(channel);
    }

    public DCChannel addChannelToMRProject(String projectID, long channel) {
        final DCChannel chan = getOrCreateDCChannel(channel);
        final ArrayList<String> projs = new ArrayList<>(List.of(chan.data.mrProjects));
        projs.add(projectID);
        chan.data.mrProjects = projs.toArray(new String[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateDCChannel(channel);
    }

    public DCChannel getOrCreateDCChannel(long channelID) {
        try (final ResultSet res = query("SELECT channelid,channeldata FROM channels WHERE channelid=" + channelID)) {
            while (res != null && res.next()) {
                final Root json = gson.fromJson(res.getString(2), Root.class);
                return new DCChannel(res.getLong(1), json);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DCChannel(channelID, new Root());
    }

    public ArrayList<DCChannel> getAllChannels() {
        final ArrayList<DCChannel> channels = new ArrayList<>();
        try (final ResultSet res = query("SELECT channelid,channeldata FROM channels")) {
            while (res != null && res.next()) {
                final Root json = gson.fromJson(res.getString(2), Root.class);
                channels.add(new DCChannel(res.getLong(1), json));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return channels;
    }

    @Override
    public void close() throws Exception {
        if (conn != null) {
            conn.close();
        }
        if (status != null) {
            status.close();
        }
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

    public boolean isNewCFFile(CFMod proj) throws SQLException {
        return Main.ifa.getLatestCFFile(proj.id) < proj.latestFilesIndexes[0].fileId;
    }

    public boolean isNewMRFile(Project proj, long time) throws SQLException {
        return Main.ifa.getLatestMRFile(proj.getId()) < time;
    }

    public class StatusThread implements Closeable {
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private boolean alive = true;

        public StatusThread() {
            scheduler.scheduleAtFixedRate(this::checkDBStatus, 0, 1, TimeUnit.SECONDS);
        }

        public boolean isDBAlive() {
            return alive;
        }

        private void checkDBStatus() {
            alive = isConnected();
            if (!alive) {
                try {
                    System.err.println("Attempting Database reconnect...");
                    connect();
                } catch (SQLException e) {
                    System.err.println("Failed to reconnect to database: " + e.getMessage());
                }
            }
        }

        public void close() {
            scheduler.shutdown();
        }
    }
}
