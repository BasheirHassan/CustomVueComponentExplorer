package com.customvue.componentexplorer.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable description of a single custom Vue component found in the project.
 */
public final class VueComponentInfo {

    private final @NotNull String myName;
    private final @NotNull String myTag;
    private final @NotNull ComponentKind myKind;
    private final @NotNull VirtualFile myFile;
    private final int myLine;
    private final @NotNull String myDeclaration;
    private final int myUsageCount;

    public VueComponentInfo(@NotNull String name,
                            @NotNull String tag,
                            @NotNull ComponentKind kind,
                            @NotNull VirtualFile file,
                            int line,
                            @NotNull String declaration,
                            int usageCount) {
        myName = name;
        myTag = tag;
        myKind = kind;
        myFile = file;
        myLine = line;
        myDeclaration = declaration;
        myUsageCount = usageCount;
    }

    /** The declared name of the component, e.g. {@code MyButton}. */
    public @NotNull String getName() {
        return myName;
    }

    /** The kebab-case tag that is used inside templates, e.g. {@code my-button}. */
    public @NotNull String getTag() {
        return myTag;
    }

    public @NotNull ComponentKind getKind() {
        return myKind;
    }

    public @NotNull VirtualFile getFile() {
        return myFile;
    }

    /** One based line of the declaration inside {@link #getFile()}. */
    public int getLine() {
        return myLine;
    }

    /** The source line the component was declared on. */
    public @NotNull String getDeclaration() {
        return myDeclaration;
    }

    /** How many times the component tag is used inside the templates of the project. */
    public int getUsageCount() {
        return myUsageCount;
    }

    public @Nullable String getDirectory() {
        VirtualFile parent = myFile.getParent();
        return parent == null ? null : parent.getName();
    }

    @Override
    public String toString() {
        return myName;
    }
}
