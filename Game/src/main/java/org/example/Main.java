package org.example;

import io.github.nascentlogic.jgen.Game;
import io.github.nascentlogic.jgen.Jgen;

/**
 * F.Dahl, 5/10/2026
 */
public class Main {

    static void main(String[] args) {

        for (String s : args) System.out.println(s);
        Jgen.get().start(new Game() { }, args);

    }
}
