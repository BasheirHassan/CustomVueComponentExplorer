package com.customvue.componentexplorer.scan;

import com.customvue.componentexplorer.model.ComponentKind;
import org.jetbrains.annotations.NotNull;

/**
 * A component declaration found inside the source code of a file.
 */
public final class ComponentDeclaration {

    private final @NotNull String myName;
    private final @NotNull ComponentKind myKind;
    private final int myLine;
    private final @NotNull String mySnippet;

    public ComponentDeclaration(@NotNull String name,
                                @NotNull ComponentKind kind,
                                int line,
                                @NotNull String snippet) {
        myName = name;
        myKind = kind;
        myLine = line;
        mySnippet = snippet;
    }

    public @NotNull String getName() {
        return myName;
    }

    public @NotNull ComponentKind getKind() {
        return myKind;
    }

    public int getLine() {
        return myLine;
    }

    public @NotNull String getSnippet() {
        return mySnippet;
    }
}
