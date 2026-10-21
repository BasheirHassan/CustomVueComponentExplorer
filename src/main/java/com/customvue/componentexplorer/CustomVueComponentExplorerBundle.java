package com.customvue.componentexplorer;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class CustomVueComponentExplorerBundle extends DynamicBundle {

    private static final String BUNDLE = "messages.CustomVueComponentExplorerBundle";

    private static final CustomVueComponentExplorerBundle INSTANCE = new CustomVueComponentExplorerBundle();

    private CustomVueComponentExplorerBundle() {
        super(BUNDLE);
    }

    public static @Nls @NotNull String message(@PropertyKey(resourceBundle = BUNDLE) @NotNull String key,
                                               Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static @NotNull Supplier<@Nls String> lazy(@PropertyKey(resourceBundle = BUNDLE) @NotNull String key,
                                                       Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}
