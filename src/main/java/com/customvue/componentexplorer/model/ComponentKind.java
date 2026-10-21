package com.customvue.componentexplorer.model;

import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The kind of a discovered Vue component.
 */
public enum ComponentKind {

    /** A regular single file component ({@code .vue} file). */
    SINGLE_FILE,

    /** A component registered globally through {@code app.component('name', ...)}. */
    GLOBAL_REGISTRATION,

    /** A component declared through the {@code components: { ... }} option. */
    OPTIONS_REGISTRATION,

    /** A custom web element registered through {@code customElements.define('name', ...)}. */
    WEB_COMPONENT;

    public @NotNull String getLabel() {
        return CustomVueComponentExplorerBundle.message("kind." + name().toLowerCase(Locale.ROOT));
    }
}
