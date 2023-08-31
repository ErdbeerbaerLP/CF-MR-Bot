package de.erdbeerbaerlp.curseforgeBot.storage.json;


import java.util.Arrays;

public class Root {
    public Long[] projects = new Long[0];
    public String[] mrProjects = new String[0];
    public Settings settings = new Settings();

    @Override
    public String toString() {
        return "Root{" +
                "cfProjects=" + Arrays.toString(projects) +
                ", mrProjects=" + Arrays.toString(mrProjects) +
                ", settings=" + settings +
                '}';
    }
}
