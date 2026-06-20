package io.github.nascentlogic.jgen;

import io.github.nascentlogic.jgen.utils.IntQueue;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

/**
 * F.Dahl, 6/7/2026
 */
public final class Keyboard {

    public static final int MAX_PROCESSORS = 16;
    private final TextProcessor[] textProcessors = new TextProcessor[MAX_PROCESSORS];
    private int processorCount = 0;

    private final IntQueue queuedKeys = new IntQueue(48);       // queued key events
    private final IntQueue queuedChar = new IntQueue(16);       // queued chars
    private final boolean[] cKeys = new boolean[GLFW_KEY_LAST];         // currently pressed
    private final boolean[] pkeys = new boolean[GLFW_KEY_LAST];         // previously pressed

    Keyboard() { /* */ }

    void processInput() {
        System.arraycopy(cKeys,0, pkeys,0, GLFW_KEY_LAST);
        while (!queuedKeys.isEmpty()) {
            int k = queuedKeys.dequeue();
            int m = queuedKeys.dequeue();
            int a = queuedKeys.dequeue();
            if (a == GLFW_PRESS) cKeys[k] = true;
            else if (a == GLFW_RELEASE) cKeys[k] = false;
            // Iterating backwards handles mid-callback removals cleanly
            // without skipping elements or risking NullPointerExceptions
            for (int i = processorCount - 1; i >= 0; i--)
                textProcessors[i].onKeyEvent(k, m, a);
        } while (!queuedChar.isEmpty()) {
            int c = queuedChar.dequeue();
            int count = processorCount;
            for (int i = processorCount - 1; i >= 0; i--) {
                textProcessors[i].onCharType((byte) c);
            }
        }
    }

    void onKeyEvent(int key, int mods, int action) {
        if (inRange(key)) {
            if (queuedKeys.size() == 48)
                queuedKeys.dequeue(3);
            queuedKeys.enqueue(key);
            queuedKeys.enqueue(mods);
            queuedKeys.enqueue(action);
        }
    }

    void onCharPress(int codepoint) {
        switch (codepoint) {
            // --- Norwegian, Danish & Swedish ---
            // 'ae' ligature: æ
            case 230: codepoint = 'e'; break;
            // 'ae' ligature: Æ
            case 198: codepoint = 'E'; break;
            // 'o' variations: ø, ö
            case 248: case 246: codepoint = 'o'; break;
            // 'o' variations: Ø, Ö
            case 216: case 214: codepoint = 'O'; break;
            // 'a' variations: å, ä
            case 229: case 228: codepoint = 'a'; break;
            // 'a' variations: Å, Ä
            case 197: case 196: codepoint = 'A'; break;
            // --- German ---
            // 's' sharp: ß
            case 223: codepoint = 's'; break;
            // 's' sharp uppercase: ẞ
            case 7838: codepoint = 'S'; break;
            // --- French, Spanish, Italian, Portuguese accents ---
            // 'a' accents: à, á, â, ã
            case 224: case 225: case 226: case 227: codepoint = 'a'; break;
            // 'a' accents: À, Á, Â, Ã
            case 192: case 193: case 194: case 195: codepoint = 'A'; break;
            // 'e' accents: è, é, ê, ë
            case 232: case 233: case 234: case 235: codepoint = 'e'; break;
            // 'e' accents: È, É, Ê, Ë
            case 200: case 201: case 202: case 203: codepoint = 'E'; break;
            // 'i' accents: ì, í, î, ï
            case 236: case 237: case 238: case 239: codepoint = 'i'; break;
            // 'i' accents: Ì, Í, Î, Ï
            case 204: case 205: case 206: case 207: codepoint = 'I'; break;
            // 'o' accents: ò, ó, ô, õ
            case 242: case 243: case 244: case 245: codepoint = 'o'; break;
            // 'o' accents: Ò, Ó, Ô, Õ
            case 210: case 211: case 212: case 213: codepoint = 'O'; break;
            // 'u' accents: ù, ú, û, ü
            case 249: case 250: case 251: case 252: codepoint = 'u'; break;
            // 'u' accents: Ù, Ú, Û, Ü
            case 217: case 218: case 219: case 220: codepoint = 'U'; break;
            // 'y' accents: ÿ
            case 255: codepoint = 'y'; break;
            // 'y' accents: Ÿ
            case 376: codepoint = 'Y'; break;
            // --- Special Consonants ---
            // 'n' tilde: ñ
            case 241: codepoint = 'n'; break;
            // 'n' tilde: Ñ
            case 209: codepoint = 'N'; break;
            // 'c' cedilla: ç
            case 231: codepoint = 'c'; break;
            // 'c' cedilla: Ç
            case 199: codepoint = 'C'; break;
        } // filtering out characters outside ascii range (128 bit)
        if ((codepoint & 0x7F) == codepoint) {
            if (queuedChar.size() == 16) {
                queuedChar.dequeue();
            } queuedChar.enqueue(codepoint);
        }
    }

    public void addTextProcessor(TextProcessor processor) {
        Objects.requireNonNull(processor, "processor cannot be null");
        for (int i = 0; i < processorCount; i++) {
            if (textProcessors[i] == processor) return;
        } if (processorCount >= MAX_PROCESSORS) {
            throw new IllegalStateException("processor capacity reached (" + MAX_PROCESSORS + ")");
        } textProcessors[processorCount++] = processor;
    }

    public boolean removeTextProcessor(TextProcessor processor) {
        Objects.requireNonNull(processor, "processor cannot be null");
        for (int i = 0; i < processorCount; i++) {
            if (textProcessors[i] == processor) {
                int numMoved = processorCount - i - 1;
                if (numMoved > 0) {
                    System.arraycopy(textProcessors, i + 1, textProcessors, i, numMoved);
                } textProcessors[--processorCount] = null;
                return true;
            }
        } return false;
    }

    public void removeAllTextProcessors() {
        for (int i = 0; i < processorCount; i++) {
            textProcessors[i] = null;
        } processorCount = 0;
    }

    public boolean pressed(int key) {
        return inRange(key) && cKeys[key];
    } public boolean pressed(int key0, int key1) {
        return pressed(key0) && pressed(key1);
    } public boolean pressed(int key0, int key1, int key2) {
        return pressed(key0) && pressed(key1) && pressed(key2);
    } public boolean justPressed(int key) {
        return inRange(key) && cKeys[key] && !pkeys[key];
    }
    /**
     * @param key key
     * @param mod mod (E.g. ctrl, alt, shift etc.)
     * @return mod is pressed, and key is just pressed
     */
    public boolean justPressed(int key, int mod) {
        return pressed(mod) && justPressed(key);
    } public boolean justReleased(int key) {
        return inRange(key) && pkeys[key] && !cKeys[key];
    } private boolean inRange(int key) {
        return (key < GLFW_KEY_LAST && key >= 0);
    }


}
