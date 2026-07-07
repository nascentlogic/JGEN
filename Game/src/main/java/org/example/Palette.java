package org.example;

import io.github.nascentlogic.jgen.gfx.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * F.Dahl, 7/6/2026
 */
public class Palette {

    private String name; // Keep non-final so your setName() method compiles
    public final List<Color> colors; // Safe, clean, and highly recommended

    public Palette() {
        this("");
    }

    public Palette(String name) {
        this.name = name;
        this.colors = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Color> getColors() {
        return colors;
    }

    public void addColor(Color color) {
        colors.add(color);
    }
}
