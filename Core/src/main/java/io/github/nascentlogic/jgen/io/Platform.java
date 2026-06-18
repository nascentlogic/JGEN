package io.github.nascentlogic.jgen.io;

/** Target platforms */
public enum Platform {

    WINDOWS, LINUX, MAC;

    private static final Platform PLATFORM;

    static {
        try {
            PLATFORM = determinePlatform();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e.getMessage());
        }
    }
    /**
     * @return Client Platform (Operative System). One of (Windwos, Linux, macOS).
     */
    public static Platform get() { return PLATFORM; }

    /**
     * @return Client platform. One of (Windwos, Linux, macOS).
     */
    private static Platform determinePlatform() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return Platform.WINDOWS;
        if (osName.contains("mac") || osName.contains("darwin")) return Platform.MAC;
        if (osName.contains("nux") || osName.contains("nix") ||
                osName.contains("aix") || osName.contains("sunos") ||
                osName.contains("solaris") || osName.contains("freebsd")) {
            return Platform.LINUX;
        } throw new Exception("Unsupported Operating System: " + osName);
    }

}
