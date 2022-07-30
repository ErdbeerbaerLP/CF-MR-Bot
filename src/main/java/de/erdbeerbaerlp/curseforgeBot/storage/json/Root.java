package de.erdbeerbaerlp.curseforgeBot.storage.json;


import java.util.Arrays;

public class Root {
    public Long[] projects = new Long[0];
    public Settings settings = new Settings();

    @Override
    public String toString() {
        return "Root{" +
                "projects=" + Arrays.toString(projects) +
                ", settings=" + settings +
                '}';
    }
}
