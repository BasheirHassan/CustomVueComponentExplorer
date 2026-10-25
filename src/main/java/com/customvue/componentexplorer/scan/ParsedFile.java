package com.customvue.componentexplorer.scan;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    public static final ParsedFile EMPTY = new ParsedFile(List.of(), Map.of(), Map.of());

    private final @NotNull List<ComponentDeclaration> myDeclarations;
    private final @NotNull Map<String, Integer> myTagUsages;
    private final @NotNull Map<String, List<Integer>> myTagLines;
    private final @NotNull Map<String, List<TagUsage>> myTagOccurrences;
    private final @NotNull Map<String, Integer> myImportLines;

    public ParsedFile(@NotNull List<ComponentDeclaration> declarations,
                      @NotNull Map<String, List<TagUsage>> tagOccurrences,
                      @NotNull Map<String, Integer> importLines) {
        myDeclarations = Collections.unmodifiableList(declarations);
        myImportLines = Collections.unmodifiableMap(new LinkedHashMap<>(importLines));

        Map<String, List<TagUsage>> occurrences = new LinkedHashMap<>(tagOccurrences.size());
        Map<String, Integer> usages = new LinkedHashMap<>(tagOccurrences.size());
        Map<String, List<Integer>> lines = new LinkedHashMap<>(tagOccurrences.size());
        for (Map.Entry<String, List<TagUsage>> entry : tagOccurrences.entrySet()) {
            List<TagUsage> copied = List.copyOf(entry.getValue());
            occurrences.put(entry.getKey(), copied);
            usages.put(entry.getKey(), copied.size());

            List<Integer> tagLines = new ArrayList<>();
            for (TagUsage usage : copied) {
                if (tagLines.isEmpty() || tagLines.get(tagLines.size() - 1) != usage.getLine()) {
                    tagLines.add(usage.getLine());
                }
            }
            lines.put(entry.getKey(), List.copyOf(tagLines));
        }
        myTagOccurrences = Collections.unmodifiableMap(occurrences);
        myTagUsages = Collections.unmodifiableMap(usages);
        myTagLines = Collections.unmodifiableMap(lines);
    }

    public @NotNull List<ComponentDeclaration> getDeclarations() {
        return myDeclarations;
    }

    /** Maps a kebab-case tag to the number of times it is used inside the file. */
    public @NotNull Map<String, Integer> getTagUsages() {
        return myTagUsages;
    }

    /** Maps a kebab-case tag to all lines on which it is used inside the file. */
    public @NotNull Map<String, List<Integer>> getTagLines() {
        return myTagLines;
    }

    /** Maps a kebab-case tag to every place it is used inside the file, including its attributes. */
    public @NotNull Map<String, List<TagUsage>> getTagOccurrences() {
        return myTagOccurrences;
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
