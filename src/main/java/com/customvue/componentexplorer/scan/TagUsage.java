package com.customvue.componentexplorer.scan;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A single occurrence of a tag inside a file: the line it starts on and the attributes that are
 * written on it.
 */
public final class TagUsage {

    private final int myLine;
    private final @NotNull Set<String> myAttributes;

    public TagUsage(int line, @NotNull Set<String> attributes) {
        myLine = line;
        myAttributes = Collections.unmodifiableSet(new LinkedHashSet<>(attributes));
    }

    /** One based line on which the opening tag starts. */
    public int getLine() {
        return myLine;
    }

    /**
     * The attributes of the tag with the common Vue prefixes removed, e.g. {@code :disabled} and
     * {@code v-bind:disabled} are stored as {@code disabled}.
     */
    public @NotNull Set<String> getAttributes() {
        return myAttributes;
    }
}
