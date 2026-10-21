package com.customvue.componentexplorer.scan;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The result of parsing a single file: the components it declares, the tags it uses and the
 * names it imports.
 */
public final class ParsedFile {

    /** An empty result, used for files that cannot be parsed. */
    public static final ParsedFile EMPTY = new ParsedFile(List.of(), Map.of(), Map.of(), Map.of());

    private final @NotNull List<ComponentDeclaration> myDeclarations;
    private final @NotNull Map<String, Integer> myTagUsages;
    private final @NotNull Map<String, Integer> myTagLines;
    private final @NotNull Map<String, Integer> myImportLines;

    public ParsedFile(@NotNull List<ComponentDeclaration> declarations,
                      @NotNull Map<String, Integer> tagUsages,
                      @NotNull Map<String, Integer> tagLines,
                      @NotNull Map<String, Integer> importLines) {
        myDeclarations = Collections.unmodifiableList(declarations);
        myTagUsages = Collections.unmodifiableMap(new LinkedHashMap<>(tagUsages));
        myTagLines = Collections.unmodifiableMap(new LinkedHashMap<>(tagLines));
        myImportLines = Collections.unmodifiableMap(new LinkedHashMap<>(importLines));
    }

    public @NotNull List<ComponentDeclaration> getDeclarations() {
        return myDeclarations;
    }

    /** Maps a kebab-case tag to the number of times it is used inside the file. */
    public @NotNull Map<String, Integer> getTagUsages() {
        return myTagUsages;
    }

    /** Maps a kebab-case tag to the first line on which it is used inside the file. */
    public @NotNull Map<String, Integer> getTagLines() {
        return myTagLines;
    }

    /** Maps a local name to the line of the import statement that brings it in. */
    public @NotNull Map<String, Integer> getImportLines() {
        return myImportLines;
    }

    /** The local names of everything that is imported by the file. */
    public @NotNull Set<String> getImports() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(myImportLines.keySet()));
    }
}
