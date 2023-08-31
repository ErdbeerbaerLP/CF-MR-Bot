package de.erdbeerbaerlp.curseforgeBot.storage.json;

public class DCChannel {

    public final long channelID;
    public final Root data;

    public DCChannel(long channelID, Root json) {
        this.channelID = channelID;
        this.data = json;
    }

    @Override
    public String toString() {
        return "CFChannel{" +
                "channelID=" + channelID +
                ", data=" + data +
                '}';
    }
}