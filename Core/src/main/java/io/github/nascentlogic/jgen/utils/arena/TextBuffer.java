package io.github.nascentlogic.jgen.utils.arena;

import org.jetbrains.annotations.NotNull;

/**
 * F.Dahl, 8/16/2026
 */
public class TextBuffer extends ManagedText {


    @Override
    public boolean set(byte c, int index) {
        return false;
    }

    @Override
    public int set(CharSequence str) {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public byte get(int index) {
        return 0;
    }

    @Override
    public int length() {
        return 0;
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}
