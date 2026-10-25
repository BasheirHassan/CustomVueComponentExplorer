package com.customvue.componentexplorer.scan;

import com.customvue.componentexplorer.model.ComponentKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Vue component declarations and tag usages out of raw source code.
 *
 * <p>The parser is intentionally text based: it works on plain strings and does not depend on the
 * Vue plugin, therefore it also detects components of projects that do not use the official
 * Vue support.</p>
 */
public final class ComponentParser {

    private static final Pattern SCRIPT_BLOCK =
            Pattern.compile("<script\\b[^>]*>(.*?)</script>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern DEFINE_OPTIONS_CALL = Pattern.compile("\\bdefineOptions\\s*\\(");
    private static final Pattern DEFINE_COMPONENT_CALL = Pattern.compile("\\bdefineComponent\\s*\\(");
    private static final Pattern EXPORT_DEFAULT = Pattern.compile("\\bexport\\s+default\\b");

    /** {@code app.component('MyComponent', MyComponent)}. */
    private static final Pattern COMPONENT_CALL =
            Pattern.compile("\\.\\s*component\\s*\\(\\s*['\"`]([^'\"`]+)['\"`]");

    /** {@code customElements.define('my-element', MyElement)}. */
    private static final Pattern CUSTOM_ELEMENT_DEFINE =
            Pattern.compile("\\bcustomElements\\s*\\.\\s*define\\s*\\(\\s*['\"`]([^'\"`]+)['\"`]");

    /** The {@code components: { ... }} option of a component. */
    private static final Pattern COMPONENTS_OPTION = Pattern.compile("\\bcomponents\\s*:\\s*\\{");

    /** A single entry of the {@code components: { ... }} option. */
    private static final Pattern COMPONENTS_ENTRY =
            Pattern.compile("(?:['\"`]([A-Za-z][A-Za-z0-9-]*)['\"`]|\\b([A-Za-z][A-Za-z0-9]*))\\s*:\\s*([A-Za-z_$][A-Za-z0-9_$]*)");

    /** A shorthand entry of the {@code components: { ... }} option, e.g. {@code MyComponent,}. */
    private static final Pattern COMPONENTS_SHORTHAND =
            Pattern.compile("['\"`]?([A-Za-z][A-Za-z0-9]*)['\"`]?");

    /** Any element tag used inside a template. */
    private static final Pattern TAG_USAGE =
            Pattern.compile("<([A-Za-z][A-Za-z0-9]*(?:-[A-Za-z0-9]+)*)");

    /** An import statement, the first group contains the import clause. */
    private static final Pattern IMPORT_STATEMENT =
            Pattern.compile("\\bimport\\s+([^;'\"]+?)\\s+from\\s+['\"][^'\"]+['\"]");

    /** The named part of an import clause, e.g. {@code { Foo, Bar as Baz }}. */
    private static final Pattern IMPORT_NAMED = Pattern.compile("\\{([^}]*)}");

    /** A valid JavaScript identifier. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private static final int MAX_SNIPPET_LENGTH = 160;

    private ComponentParser() {
    }

    /**
     * Parses a single file component ({@code .vue}).
     */
    public static @NotNull ParsedFile parseVueFile(@NotNull String fileNameWithoutExtension, @NotNull String text) {
        List<ComponentDeclaration> declarations = new ArrayList<>();

        int[] scriptRegion = findScriptRegion(text);
        if (scriptRegion == null) {
            // A template only component without any script block.
            declarations.add(new ComponentDeclaration(toPascalCase(fileNameWithoutExtension),
                    ComponentKind.SINGLE_FILE, 1, fileNameWithoutExtension + ".vue"));
        } else {
            int nameOffset = findComponentNameOffset(text, scriptRegion[0], scriptRegion[1]);
            if (nameOffset < 0) {
                declarations.add(new ComponentDeclaration(toPascalCase(fileNameWithoutExtension),
                        ComponentKind.SINGLE_FILE, 1, fileNameWithoutExtension + ".vue"));
            } else {
                declarations.add(new ComponentDeclaration(readQuotedValue(text, nameOffset),
                        ComponentKind.SINGLE_FILE, lineOf(text, nameOffset), snippetAt(text, nameOffset)));
            }
            // Components that are registered from inside a single file component.
            collectRegistrations(text, scriptRegion[0], scriptRegion[1], declarations, false);
        }

        return new ParsedFile(declarations,
                collectTagUsages(text, 0, text.length()),
                collectTagLines(text, 0, text.length()),
                collectImports(text, 0, text.length()));
    }

    /**
     * Parses a plain script file ({@code .js}, {@code .ts}, ...).
     */
    public static @NotNull ParsedFile parseScriptFile(@NotNull String fileNameWithoutExtension, @NotNull String text) {
        List<ComponentDeclaration> declarations = new ArrayList<>();
        collectRegistrations(text, 0, text.length(), declarations, true);
        return new ParsedFile(declarations,
                collectTagUsages(text, 0, text.length()),
                collectTagLines(text, 0, text.length()),
                collectImports(text, 0, text.length()));
    }

    /**
     * Converts a component name into the kebab-case tag that is used inside templates.
     */
    public static @NotNull String toKebabCase(@NotNull String name) {
        StringBuilder builder = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '-') {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(c));
            } else if (c == '_' || c == ' ' || c == '.') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '-') {
                    builder.append('-');
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * Converts a file name into a component name, e.g. {@code my-button} into {@code MyButton}.
     */
    public static @NotNull String toPascalCase(@NotNull String name) {
        StringBuilder builder = new StringBuilder(name.length());
        boolean upperCase = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == ' ' || c == '.') {
                upperCase = true;
                continue;
            }
            builder.append(upperCase ? Character.toUpperCase(c) : c);
            upperCase = false;
        }
        return builder.toString();
    }

    /**
     * Locates the content of the first {@code <script>} block of a single file component.
     *
     * @return {@code {start, end}} of the script content or {@code null} when there is no script block.
     */
    private static int @Nullable [] findScriptRegion(@NotNull String text) {
        Matcher matcher = SCRIPT_BLOCK.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new int[]{matcher.start(1), matcher.end(1)};
    }

    /**
     * Looks for the name of the component that is declared inside the given region.
     *
     * @return the offset of the opening quote of the name or {@code -1} when there is none.
     */
    private static int findComponentNameOffset(@NotNull String text, int start, int end) {
        int offset = searchNameOffsetNear(text, start, end, DEFINE_OPTIONS_CALL);
        if (offset < 0) {
            offset = searchNameOffsetNear(text, start, end, DEFINE_COMPONENT_CALL);
        }
        if (offset < 0) {
            offset = searchNameOffsetNear(text, start, end, EXPORT_DEFAULT);
        }
        return offset;
    }

    /**
     * Returns the offset of the value of a top level {@code name} property that follows an anchor.
     */
    private static int searchNameOffsetNear(@NotNull String text,
                                            int start,
                                            int end,
                                            @NotNull Pattern anchor) {
        Matcher matcher = anchor.matcher(text);
        matcher.region(start, end);
        while (matcher.find()) {
            int brace = text.indexOf('{', matcher.end());
            if (brace < 0 || brace >= end) {
                continue;
            }
            int blockEnd = findBlockEnd(text, brace, end);
            int offset = findTopLevelNameValue(text, brace + 1, blockEnd - 1);
            if (offset >= 0) {
                return offset;
            }
        }
        return -1;
    }

    /**
     * Scans a block for a {@code name} property that is declared on the first level of the block.
     *
     * @return the offset of the opening quote of the value or {@code -1} when there is none.
     */
    private static int findTopLevelNameValue(@NotNull String text, int blockStart, int blockEnd) {
        int depth = 0;
        int index = blockStart;
        while (index < blockEnd) {
            char c = text.charAt(index);
            if (c == '{' || c == '[' || c == '(') {
                depth++;
                index++;
                continue;
            }
            if (c == '}' || c == ']' || c == ')') {
                depth--;
                index++;
                continue;
            }
            if (depth == 0 && isIdentifierStart(c)) {
                int identifierEnd = index;
                while (identifierEnd < blockEnd && isIdentifierPart(text.charAt(identifierEnd))) {
                    identifierEnd++;
                }
                String identifier = text.substring(index, identifierEnd);
                if ("name".equals(identifier)) {
                    int value = skipToValue(text, identifierEnd, blockEnd);
                    if (value >= 0) {
                        return value;
                    }
                }
                index = identifierEnd;
                continue;
            }
            index++;
        }
        return -1;
    }

    /**
     * Skips the colon of a property and returns the offset of the quoted value.
     */
    private static int skipToValue(@NotNull String text, int from, int blockEnd) {
        int index = from;
        while (index < blockEnd && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        if (index < blockEnd && text.charAt(index) == ':') {
            index++;
            while (index < blockEnd && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            if (index < blockEnd
                    && (text.charAt(index) == '\'' || text.charAt(index) == '"' || text.charAt(index) == '`')) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Reads the quoted value that starts at the given offset.
     */
    private static @NotNull String readQuotedValue(@NotNull String text, int quoteOffset) {
        char quote = text.charAt(quoteOffset);
        int close = text.indexOf(quote, quoteOffset + 1);
        return close < 0 ? "" : text.substring(quoteOffset + 1, close);
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * Collects global registrations, option based registrations and custom elements.
     */
    private static void collectRegistrations(@NotNull String text,
                                             int start,
                                             int end,
                                             @NotNull List<ComponentDeclaration> declarations,
                                             boolean includeOptions) {
        Matcher componentMatcher = COMPONENT_CALL.matcher(text);
        componentMatcher.region(start, end);
        while (componentMatcher.find()) {
            declarations.add(new ComponentDeclaration(componentMatcher.group(1),
                    ComponentKind.GLOBAL_REGISTRATION,
                    lineOf(text, componentMatcher.start(1)),
                    snippetAt(text, componentMatcher.start(1))));
        }

        Matcher customElementMatcher = CUSTOM_ELEMENT_DEFINE.matcher(text);
        customElementMatcher.region(start, end);
        while (customElementMatcher.find()) {
            declarations.add(new ComponentDeclaration(customElementMatcher.group(1),
                    ComponentKind.WEB_COMPONENT,
                    lineOf(text, customElementMatcher.start(1)),
                    snippetAt(text, customElementMatcher.start(1))));
        }

        if (!includeOptions) {
            return;
        }

        Matcher optionsMatcher = COMPONENTS_OPTION.matcher(text);
        optionsMatcher.region(start, end);
        while (optionsMatcher.find()) {
            int brace = optionsMatcher.end() - 1;
            int blockEnd = findBlockEnd(text, brace, end);
            parseComponentsBlock(text, brace, blockEnd, declarations);
        }
    }

    /**
     * Parses the entries of a {@code components: { ... }} option.
     */
    private static void parseComponentsBlock(@NotNull String text,
                                             int openBrace,
                                             int blockEnd,
                                             @NotNull List<ComponentDeclaration> declarations) {
        int depth = 0;
        int entryStart = openBrace + 1;
        int index = entryStart;
        while (index < blockEnd) {
            char c = text.charAt(index);
            if (c == '{' || c == '[' || c == '(') {
                depth++;
            } else if (c == '}' || c == ']' || c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                addComponentsEntry(text, entryStart, index, declarations);
                entryStart = index + 1;
            }
            index++;
        }
        addComponentsEntry(text, entryStart, blockEnd, declarations);
    }

    private static void addComponentsEntry(@NotNull String text,
                                           int start,
                                           int end,
                                           @NotNull List<ComponentDeclaration> declarations) {
        if (end <= start) {
            return;
        }
        Matcher matcher = COMPONENTS_ENTRY.matcher(text);
        matcher.region(start, end);
        if (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (key != null) {
                declarations.add(new ComponentDeclaration(key,
                        ComponentKind.OPTIONS_REGISTRATION,
                        lineOf(text, matcher.start()),
                        snippetAt(text, matcher.start())));
                return;
            }
        }

        // Shorthand notation: components: { MyComponent, }
        String raw = text.substring(start, end);
        String segment = raw.trim();
        Matcher shorthand = COMPONENTS_SHORTHAND.matcher(segment);
        if (shorthand.matches()) {
            int offset = start + (raw.length() - raw.stripLeading().length());
            declarations.add(new ComponentDeclaration(shorthand.group(1),
                    ComponentKind.OPTIONS_REGISTRATION,
                    lineOf(text, offset),
                    snippetAt(text, offset)));
        }
    }

    /**
     * Collects the local names of everything that is imported by the file together with the line
     * of the import statement.
     */
    private static @NotNull Map<String, Integer> collectImports(@NotNull String text, int start, int end) {
        Map<String, Integer> imports = new LinkedHashMap<>();
        Matcher matcher = IMPORT_STATEMENT.matcher(text);
        matcher.region(start, end);
        LineTracker tracker = new LineTracker(text);
        while (matcher.find()) {
            collectImportClause(matcher.group(1), imports, tracker.lineAt(matcher.start()));
        }
        return imports;
    }

    /**
     * Extracts the local names of an import clause such as
     * {@code Foo, { Bar, Baz as Qux }, * as everything}.
     */
    private static void collectImportClause(@NotNull String clause,
                                            @NotNull Map<String, Integer> imports,
                                            int line) {
        Matcher named = IMPORT_NAMED.matcher(clause);
        while (named.find()) {
            for (String part : named.group(1).split(",")) {
                String entry = part.trim().replaceFirst("^type\\s+", "").trim();
                if (entry.isEmpty()) {
                    continue;
                }
                int as = entry.indexOf(" as ");
                String name = as >= 0 ? entry.substring(as + 4).trim() : entry;
                addIfIdentifier(name, imports, line);
            }
        }

        String rest = IMPORT_NAMED.matcher(clause).replaceAll("").trim();
        for (String part : rest.split(",")) {
            String entry = part.trim().replaceFirst("^type\\s+", "").trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.startsWith("*")) {
                int as = entry.indexOf(" as ");
                if (as >= 0) {
                    addIfIdentifier(entry.substring(as + 4).trim(), imports, line);
                }
                continue;
            }
            int space = entry.indexOf(' ');
            addIfIdentifier(space >= 0 ? entry.substring(0, space).trim() : entry, imports, line);
        }
    }

    private static void addIfIdentifier(@NotNull String name, @NotNull Map<String, Integer> imports, int line) {
        if (IDENTIFIER.matcher(name).matches()) {
            imports.putIfAbsent(name, line);
        }
    }

    /**
     * Counts how often every tag is used inside the given region and remembers every line it is
     * used on. Multiple usages on the same line are stored once.
     */
    private static void collectTags(@NotNull String text,
                                    int start,
                                    int end,
                                    @NotNull Map<String, Integer> counts,
                                    @NotNull Map<String, List<Integer>> lines) {
        Matcher matcher = TAG_USAGE.matcher(text);
        matcher.region(start, end);
        LineTracker tracker = new LineTracker(text);
        while (matcher.find()) {
            String tag = toKebabCase(matcher.group(1));
            counts.merge(tag, 1, Integer::sum);
            List<Integer> tagLines = lines.computeIfAbsent(tag, key -> new ArrayList<>());
            int line = tracker.lineAt(matcher.start());
            if (tagLines.isEmpty() || tagLines.get(tagLines.size() - 1) != line) {
                tagLines.add(line);
            }
        }
    }

    /**
     * Counts how often every tag is used inside the given region.
     */
    private static @NotNull Map<String, Integer> collectTagUsages(@NotNull String text, int start, int end) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        collectTags(text, start, end, counts, new LinkedHashMap<>());
        return counts;
    }

    /**
     * Remembers every line on which a tag is used inside the given region.
     */
    private static @NotNull Map<String, List<Integer>> collectTagLines(@NotNull String text, int start, int end) {
        Map<String, List<Integer>> lines = new LinkedHashMap<>();
        collectTags(text, start, end, new LinkedHashMap<>(), lines);
        return lines;
    }

    /**
     * Calculates the line numbers of the given offsets while the text is scanned from left to right.
     */
    private static final class LineTracker {

        private final @NotNull String myText;
        private int myPosition;
        private int myLine = 1;

        private LineTracker(@NotNull String text) {
            myText = text;
        }

        private int lineAt(int offset) {
            int limit = Math.min(offset, myText.length());
            while (myPosition < limit) {
                if (myText.charAt(myPosition) == '\n') {
                    myLine++;
                }
                myPosition++;
            }
            return myLine;
        }
    }

    /**
     * Returns the index right behind the closing brace of the block that starts at {@code openBrace}.
     */
    private static int findBlockEnd(@NotNull String text, int openBrace, int limit) {
        int end = Math.min(text.length(), limit);
        int depth = 0;
        for (int i = openBrace; i < end; i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return end;
    }

    /**
     * Calculates the one based line of the given offset.
     */
    private static int lineOf(@NotNull String text, int offset) {
        int line = 1;
        int limit = Math.min(offset, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * Returns the trimmed source line of the given offset.
     */
    private static @NotNull String snippetAt(@NotNull String text, int offset) {
        int limit = Math.min(offset, text.length());
        int lineStart = text.lastIndexOf('\n', Math.max(0, limit - 1)) + 1;
        int lineEnd = text.indexOf('\n', limit);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        String snippet = text.substring(lineStart, lineEnd).trim();
        if (snippet.length() > MAX_SNIPPET_LENGTH) {
            snippet = snippet.substring(0, MAX_SNIPPET_LENGTH) + "…";
        }
        return snippet;
    }
}
