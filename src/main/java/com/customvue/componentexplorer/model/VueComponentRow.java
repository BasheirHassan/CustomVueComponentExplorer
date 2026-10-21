package com.customvue.componentexplorer.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * A component together with its relation to the file that is currently open and the place the
 * editor has to jump to.
 */
public final class VueComponentRow {

    private final @NotNull VueComponentInfo myComponent;
    private final @NotNull ComponentRelation myRelation;
    private final @NotNull VirtualFile myTargetFile;
    private final int myTargetLine;

    public VueComponentRow(@NotNull VueComponentInfo component,
                            @NotNull ComponentRelation relation,
                            @NotNull VirtualFile targetFile,
                            int targetLine) {
        myComponent = component;
        myRelation = relation;
        myTargetFile = targetFile;
        myTargetLine = targetLine;
    }

    public @NotNull VueComponentInfo getComponent() {
        return myComponent;
    }

    public @NotNull ComponentRelation getRelation() {
        return myRelation;
    }

    /**
     * The file the editor has to open when the row is activated.
     */
    public @NotNull VirtualFile getTargetFile() {
        return myTargetFile;
    }

    /**
     * The zero based line the editor has to jump to.
     */
    public int getTargetLine() {
        return myTargetLine;
    }
}
