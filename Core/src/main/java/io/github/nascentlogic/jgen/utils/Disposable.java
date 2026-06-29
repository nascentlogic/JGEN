package io.github.nascentlogic.jgen.utils;

/**
 * Disposable is meant for resources allocated outside the Java heap.
 * Native memory or GPU storage.
 * Freeing an object often if not always renders that object useless.
 * Frederik Dahl 12/5/2024
 */
public interface Disposable {

    static void free(Disposable disposable) {
        if (disposable != null) disposable.free();
    }

    static void free(Disposable ...disposables) {
        if (disposables != null) {
            for (Disposable disposable : disposables)
                free(disposable);
        }
    }

    void free();
}
