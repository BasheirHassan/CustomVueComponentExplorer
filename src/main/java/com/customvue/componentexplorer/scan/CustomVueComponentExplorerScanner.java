package com.customvue.componentexplorer.scan;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.customvue.componentexplorer.model.VueComponentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Scans the project for custom Vue components.
 *
 * <p>The scanner keeps a cache of the already parsed files. Only files whose modification stamp
 * changed are parsed again, therefore refreshing the list is cheap even for large projects.</p>
 */
public final class CustomVueComponentExplorerScanner {

    private static final Logger LOG = Logger.getInstance(CustomVueComponentExplorerScanner.class);

    private static final String[] VUE_EXTENSIONS = {"vue"};
    private static final String[] SCRIPT_EXTENSIONS = {"js", "mjs", "cjs", "jsx", "ts", "tsx", "mts", "cts"};

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            "node_modules", "dist", "build", "out", "target", "coverage", "vendor",
            ".git", ".idea", ".nuxt", ".output", ".next", ".svelte-kit", "tmp", "__pycache__");

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_DIRECTORY_DEPTH = 32;

    private final @NotNull Project myProject;
    private final @NotNull Map<String, CachedFile> myCache = new HashMap<>();

    public CustomVueComponentExplorerScanner(@NotNull Project project) {
        myProject = project;
    }

    /**
     * Scans the project and returns the list of all discovered components.
     */
    public synchronized @NotNull List<VueComponentInfo> scan() {
        Map<String, CachedFile> cache = new HashMap<>();
        Map<String, Integer> globalUsages = new LinkedHashMap<>();
        List<VirtualFile> files = new ArrayList<>(collectFiles());

        for (VirtualFile file : files) {
            CachedFile cached = myCache.get(file.getUrl());
            if (cached == null || cached.getModificationStamp() != file.getModificationStamp()) {
                cached = parse(file);
            }
            cache.put(file.getUrl(), cached);
            for (Map.Entry<String, Integer> entry : cached.getParsedFile().getTagUsages().entrySet()) {
                globalUsages.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        myCache.clear();
        myCache.putAll(cache);

        List<VueComponentInfo> result = new ArrayList<>();
        for (VirtualFile file : files) {
            CachedFile cached = cache.get(file.getUrl());
            if (cached == null) {
                continue;
            }
            for (ComponentDeclaration declaration : cached.getParsedFile().getDeclarations()) {
                String tag = ComponentParser.toKebabCase(declaration.getName());
                int usages = globalUsages.getOrDefault(tag, 0);
                result.add(new VueComponentInfo(declaration.getName(), tag, declaration.getKind(), file,
                        declaration.getLine(), declaration.getSnippet(), usages));
            }
        }

        result.sort(Comparator.comparing(VueComponentInfo::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(VueComponentInfo::getTag));
        LOG.info("Vue components scan finished: " + files.size() + " files inspected, "
                + result.size() + " components found");
        return result;
    }

    /**
     * The already parsed content of a file, or {@code null} when the file was not scanned yet.
     */
    public synchronized @Nullable ParsedFile getParsedFile(@NotNull VirtualFile file) {
        CachedFile cached = myCache.get(file.getUrl());
        return cached == null ? null : cached.getParsedFile();
    }

    /**
     * Removes the cached content of the given file so it is parsed again on the next scan.
     */
    public synchronized void invalidate(@NotNull VirtualFile file) {
        myCache.remove(file.getUrl());
    }

    /**
     * Collects every attribute that is written on the given tag anywhere in the project. The
     * attributes are normalized the same way as the parser does it (binding prefixes removed) and
     * sorted case insensitively.
     */
    public synchronized @NotNull List<String> getAttributesForTag(@NotNull String tag) {
        Set<String> attributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (CachedFile cached : myCache.values()) {
            addAttributes(attributes, cached.getParsedFile(), tag);
        }
        return new ArrayList<>(attributes);
    }

    /**
     * Collects the attributes that are written on the given tag inside one file only. It is used
     * to populate the attribute filter of the search that is restricted to the open page.
     */
    public synchronized @NotNull List<String> getAttributesForTag(@NotNull String tag,
                                                                  @NotNull VirtualFile file) {
        Set<String> attributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        CachedFile cached = myCache.get(file.getUrl());
        if (cached != null) {
            addAttributes(attributes, cached.getParsedFile(), tag);
        }
        return new ArrayList<>(attributes);
    }

    private static void addAttributes(@NotNull Set<String> attributes,
                                      @NotNull ParsedFile parsedFile,
                                      @NotNull String tag) {
        List<TagUsage> usages = parsedFile.getTagOccurrences().get(tag);
        if (usages == null) {
            return;
        }
        for (TagUsage usage : usages) {
            attributes.addAll(usage.getAttributes());
        }
    }

    /**
     * Collects all files of the project that may contain Vue components.
     *
     * <p>The whole collection runs inside a read action because the file index and the project
     * file index may only be queried while holding the read lock.</p>
     */
    private @NotNull List<VirtualFile> collectFiles() {
        return ReadAction.computeCancellable(this::collectFilesUnsafe);
    }

    private @NotNull List<VirtualFile> collectFilesUnsafe() {
        Set<VirtualFile> result = new LinkedHashSet<>();
        FileTypeManager typeManager = FileTypeManager.getInstance();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);

        for (String extension : VUE_EXTENSIONS) {
            collectByExtension(typeManager, extension, scope, result);
        }
        for (String extension : SCRIPT_EXTENSIONS) {
            collectByExtension(typeManager, extension, scope, result);
        }

        if (result.isEmpty()) {
            // The index did not deliver anything, walk the content roots as a fallback.
            collectFromContentRoots(result);
        }

        List<VirtualFile> files = new ArrayList<>();
        for (VirtualFile file : result) {
            if (!file.isDirectory() && isInProjectContent(file) && !isExcluded(file)) {
                files.add(file);
            }
        }
        return files;
    }

    private static void collectByExtension(@NotNull FileTypeManager typeManager,
                                           @NotNull String extension,
                                           @NotNull GlobalSearchScope scope,
                                           @NotNull Set<VirtualFile> result) {
        FileType type = typeManager.getFileTypeByExtension(extension);
        if (type != FileTypes.UNKNOWN) {
            result.addAll(FileTypeIndex.getFiles(type, scope));
        }
    }

    /**
     * Collects the candidate files by walking the content roots of the project.
     */
    private void collectFromContentRoots(@NotNull Set<VirtualFile> result) {
        VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentRoots();
        for (VirtualFile root : roots) {
            walk(root, result, 0);
        }
    }

    private void walk(@NotNull VirtualFile directory, @NotNull Set<VirtualFile> result, int depth) {
        if (depth > MAX_DIRECTORY_DEPTH) {
            return;
        }
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                if (!EXCLUDED_DIRECTORIES.contains(child.getName().toLowerCase(Locale.ROOT))) {
                    walk(child, result, depth + 1);
                }
            } else if (isCandidate(child)) {
                result.add(child);
            }
        }
    }

    private static boolean isCandidate(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase(Locale.ROOT);
        for (String candidate : VUE_EXTENSIONS) {
            if (candidate.equals(extension)) {
                return true;
            }
        }
        for (String candidate : SCRIPT_EXTENSIONS) {
            if (candidate.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInProjectContent(@NotNull VirtualFile file) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(myProject);
        return index.isInSourceContent(file) || index.isInContent(file) || isUnderContentRoot(file);
    }

    /**
     * Checks whether the file is located below one of the content roots of the project. This is
     * more tolerant than the project index, which excludes folders that are marked as excluded.
     */
    private boolean isUnderContentRoot(@NotNull VirtualFile file) {
        for (VirtualFile root : ProjectRootManager.getInstance(myProject).getContentRoots()) {
            if (VfsUtilCore.isAncestor(root, file, false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExcluded(@NotNull VirtualFile file) {
        VirtualFile current = file.getParent();
        while (current != null) {
            if (EXCLUDED_DIRECTORIES.contains(current.getName().toLowerCase(Locale.ROOT))) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private @NotNull CachedFile parse(@NotNull VirtualFile file) {
        long stamp = file.getModificationStamp();
        if (file.getLength() > MAX_FILE_SIZE) {
            return new CachedFile(stamp, ParsedFile.EMPTY);
        }

        String text = readText(file);
        if (text == null || text.isEmpty()) {
            return new CachedFile(stamp, ParsedFile.EMPTY);
        }

        try {
            String extension = file.getExtension();
            String name = file.getNameWithoutExtension();
            ParsedFile parsed = "vue".equalsIgnoreCase(extension)
                    ? ComponentParser.parseVueFile(name, text)
                    : ComponentParser.parseScriptFile(name, text);
            return new CachedFile(stamp, parsed);
        } catch (Exception e) {
            LOG.warn("Unable to parse " + file.getPath(), e);
            return new CachedFile(stamp, ParsedFile.EMPTY);
        }
    }

    private @Nullable String readText(@NotNull VirtualFile file) {
        return ReadAction.computeCancellable(() -> {
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
            return psiFile == null ? null : psiFile.getText();
        });
    }

    /**
     * The cached result of a single file.
     */
    private static final class CachedFile {

        private final long myModificationStamp;
        private final @NotNull ParsedFile myParsedFile;

        private CachedFile(long modificationStamp, @NotNull ParsedFile parsedFile) {
            myModificationStamp = modificationStamp;
            myParsedFile = parsedFile;
        }

        private long getModificationStamp() {
            return myModificationStamp;
        }

        private @NotNull ParsedFile getParsedFile() {
            return myParsedFile;
        }
    }
}
