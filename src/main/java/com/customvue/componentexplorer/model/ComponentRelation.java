package com.customvue.componentexplorer.model;

import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Describes how a component relates to the file that is currently open in the editor.
 */
public enum ComponentRelation {

    /** The component is defined by the open file itself. */
    CURRENT_FILE(0),

    /** The component is imported by the open file. */
    IMPORTED(1),

    /** The component is used inside the template of the open file. */
    USED(2),

    /** The component is registered globally and is therefore available on every page. */
    GLOBAL(3),

    /** The component belongs to the project but is not referenced by the open file. */
    AVAILABLE(4);

    private final int myPriority;

    ComponentRelation(int priority) {
        myPriority = priority;
    }

    public int getPriority() {
        return myPriority;
    }

    public @NotNull String getLabel() {
        return CustomVueComponentExplorerBundle.message("relation." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
