package de.erdbeerbaerlp.curseforgeBot.storage;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.project.CurseProject;
import de.erdbeerbaerlp.curseforgeBot.CurseforgeProject;
import de.erdbeerbaerlp.curseforgeBot.Main;
import de.erdbeerbaerlp.curseforgeBot.storage.json.Root;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseInterface implements AutoCloseable {
    private final Connection conn;
    public Gson gson = new GsonBuilder().create();

    public DatabaseInterface() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://" + Config.instance().database.ip + ":" + Config.instance().database.port + "/" + Config.instance().database.dbName, Config.instance().database.username, Config.instance().database.password);
        if (conn == null) {
            throw new SQLException();
        }
        runUpdate("create table if not exists cfcache\n" +
                "(\n" +
                "\tprojectid int not null,\n" +
                "\tlatestFileID int default 0 not null\n" +
                ");");
        runUpdate("create unique index cfcache_projectid_uindex\n" +
                "\ton cfcache (projectid);");
        runUpdate("alter table cfcache\n" +
                "\tadd constraint cfcache_pk\n" +
                "\t\tprimary key (projectid);");

        runUpdate("create table if not exists channels\n" +
                "(\n" +
                "\tchannelid bigint not null,\n" +
                "\tchanneldata json default ('') not null\n" +
                ");");
        runUpdate("create unique index channels_channelid_uindex\n" +
                "\ton channels (channelid);");
        runUpdate("alter table channels\n" +
                "\tadd constraint channels_pk\n" +
                "\t\tprimary key (channelid);");

        runUpdate("alter table channels modify column channeldata json default ('" + gson.toJson(new Root()) + "') not null;");
    }

    public void addChannel(CurseforgeProject.CFChannel channel) {
        runUpdate("INSERT INTO channels (channelid,channeldata) values (" + channel.channelID + ",'" + gson.toJson(channel.data) + "')");
    }

    public void delChannel(long serverID) {
        runUpdate("DELETE FROM channels WHERE channelid = " + serverID);
    }

    public int getLatestFile(int projectid) throws SQLException {
        final ResultSet query = query("SELECT latestFileID FROM cfcache WHERE projectid = " + projectid + ";");
        if (query.next()) {
            return query.getInt(1);
        }
        return -1;
    }

    public void updateCache(int projectid, long fileid) {
        runUpdate("REPLACE INTO cfcache (projectid,latestFileID) VALUES(" + projectid + "," + fileid + ")");
    }

    public CurseforgeProject.CFChannel addChannelToProject(int projectID, long channel) {
        final CurseforgeProject.CFChannel chan = getOrCreateCFChannel(channel);
        final ArrayList<Integer> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.add(projectID);
        chan.data.projects = projs.toArray(new Integer[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateCFChannel(channel);
    }

    public CurseforgeProject.CFChannel deleteChannelFromProject(int projectID, long channel) {
        final CurseforgeProject.CFChannel chan = getOrCreateCFChannel(channel);
        final ArrayList<Integer> projs = new ArrayList<>(List.of(chan.data.projects));
        projs.remove(projectID);
        chan.data.projects = projs.toArray(new Integer[0]);
        runUpdate("REPLACE INTO channels (channelid,channeldata) values (" + channel + ",'" + gson.toJson(chan.data) + "')");
        return getOrCreateCFChannel(channel);
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

    public boolean isNewFile(CurseProject proj) throws CurseException, SQLException {
        return Main.ifa.getLatestFile(proj.id()) < proj.files().first().id();
    }
}
