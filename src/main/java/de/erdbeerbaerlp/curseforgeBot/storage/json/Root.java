package de.erdbeerbaerlp.curseforgeBot.storage.json;


import java.util.Arrays;

public class Root {
    public Integer[] projects = new Integer[0];
    public Settings settings = new Settings();

    @Override
    public String toString() {
        return "Root{" +
                "projects=" + Arrays.toString(projects) +
                ", settings=" + settings +
                '}';
    }
}
