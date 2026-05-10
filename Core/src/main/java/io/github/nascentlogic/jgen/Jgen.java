package io.github.nascentlogic.jgen;

import io.github.nascentlogic.jgen.io.Disk;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Objects;

import static io.github.nascentlogic.jgen.utils.Utils.formatBytes;

/**
 * F.Dahl, 5/10/2026
 */
public class Jgen {


    private static Jgen insntance;
    private Jgen() { /* */ }
    public static Jgen get() {
        if (insntance == null) {
            insntance = new Jgen();
        } return insntance;
    }

    private Game game;
    public Game game() { return game; }
    public <T extends Game> T game(Class<T> clazz) {
        if (game.getClass() != clazz) {
            throw new ClassCastException("");
        } return clazz.cast(game);
    }

    public void start(Game game, String[] args) {
        Objects.requireNonNull(game,"game cannot be null");
        args = args == null ? new String[0] : args;
        try {
            Disk.initialize();
            logSystemInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void logSystemInfo() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n\n=== SYSTEM & JVM INFORMATION ===");
        sb.append("\n\tUsername          : ").append(System.getProperty("user.name"));
        sb.append("\n\tOS                : ").append(System.getProperty("os.name"))
                .append(" (").append(System.getProperty("os.version")).append(")");
        sb.append("\n\tOS Architecture   : ").append(System.getProperty("os.arch"));
        String cpu = System.getenv("PROCESSOR_IDENTIFIER"); // windows specific
        if (cpu != null) sb.append("\n\tCPU               : ").append(cpu);
        sb.append("\n\tJava Version      : ").append(System.getProperty("java.version"));
        sb.append("\n\tJava Home         : ").append(System.getProperty("java.home"));
        sb.append("\n\tJVM Vendor        : ").append(System.getProperty("java.vm.vendor"));
        Runtime runtime = Runtime.getRuntime();
        sb.append("\n--- Memory ---");
        sb.append("\n\tMax Heap (-Xmx)   : ").append(formatBytes(runtime.maxMemory()));
        sb.append("\n--- Paths ---");
        sb.append("\n\tRoot Path         : ").append(Paths.get("").toAbsolutePath());
        sb.append("\n\tWorking Dir       : ").append(System.getProperty("user.dir"));
        sb.append("\n\tUser Data         : ").append(Disk.userDataDirectory());
        sb.append("\n\tGame Root         : ").append(Disk.gameRootDirectory());
        sb.append("\n--- JVM Arguments ---");
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            sb.append("\n\t   ").append(arg);
        } sb.append("\n");
        Logger.info(sb.toString());
    }


}
