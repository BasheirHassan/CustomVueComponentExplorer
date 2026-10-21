package com.customvue.componentexplorer.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Alarm;
import com.customvue.componentexplorer.model.ComponentKind;
import com.customvue.componentexplorer.model.ComponentRelation;
import com.customvue.componentexplorer.model.VueComponentInfo;
import com.customvue.componentexplorer.model.VueComponentRow;
import com.customvue.componentexplorer.scan.ParsedFile;
import com.customvue.componentexplorer.scan.CustomVueComponentExplorerScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Keeps the list of the custom Vue components of a project up to date.
 *
 * <p>The service scans the project in the background and notifies its listeners on the EDT as soon
 * as a new result is available.</p>
 */
public final class CustomVueComponentExplorerService implements Disposable {

    private static final Logger LOG = Logger.getInstance(CustomVueComponentExplorerService.class);

    private static final long REFRESH_DELAY_MS = 400L;

    private static final List<String> RELEVANT_EXTENSIONS =
            List.of("vue", "js", "mjs", "cjs", "jsx", "ts", "tsx", "mts", "cts");

    private final @NotNull Project myProject;
    private final @NotNull CustomVueComponentExplorerScanner myScanner;
    private final @NotNull Alarm myAlarm;
    private volatile @NotNull List<VueComponentInfo> myComponents = List.of();
    private final @NotNull List<Consumer<List<VueComponentInfo>>> myListeners = new CopyOnWriteArrayList<>();

    private volatile boolean myScanning;
    private volatile long myLastScanTimestamp;

    public CustomVueComponentExplorerService(@NotNull Project project) {
        myProject = project;
        myScanner = new CustomVueComponentExplorerScanner(project);
        myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        subscribeToFileChanges();
    }

    public static @NotNull CustomVueComponentExplorerService getInstance(@NotNull Project project) {
        return project.getService(CustomVueComponentExplorerService.class);
    }

    /**
     * The components that were found during the last scan.
     */
    public @NotNull List<VueComponentInfo> getComponents() {
        return myComponents;
    }

    /**
     * Builds the rows for the given file: every component of the project together with its relation
     * to that file. The most relevant components come first.
     *
     * <p>The navigation target of a row points to the place where the component is used inside the
     * open file. Only components that are not referenced by the open file point to their own
     * declaration.</p>
     */
    public @NotNull List<VueComponentRow> getRowsFor(@Nullable VirtualFile openFile) {
        List<VueComponentInfo> components = myComponents;
        ParsedFile openParsed = openFile == null ? null : myScanner.getParsedFile(openFile);

        List<VueComponentRow> rows = new ArrayList<>(components.size());
        for (VueComponentInfo component : components) {
            ComponentRelation relation = getRelation(component, openFile, openParsed);
            VirtualFile targetFile = component.getFile();
            int targetLine = Math.max(0, component.getLine() - 1);

            if (openFile != null && openParsed != null) {
                Integer usageLine = openParsed.getTagLines().get(component.getTag());
                if (usageLine != null) {
                    // The component is used inside the open page, jump to that place.
                    targetFile = openFile;
                    targetLine = Math.max(0, usageLine - 1);
                } else {
                    Integer importLine = openParsed.getImportLines().get(component.getName());
                    if (importLine != null) {
                        // The component is imported by the open page, jump to the import.
                        targetFile = openFile;
                        targetLine = Math.max(0, importLine - 1);
                    }
                }
            }

            rows.add(new VueComponentRow(component, relation, targetFile, targetLine));
        }
        rows.sort(Comparator.comparingInt((VueComponentRow row) -> row.getRelation().getPriority())
                .thenComparing(row -> row.getComponent().getName(), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private static @NotNull ComponentRelation getRelation(@NotNull VueComponentInfo component,
                                                           @Nullable VirtualFile openFile,
                                                           @Nullable ParsedFile openParsed) {
        if (openFile == null) {
            return ComponentRelation.AVAILABLE;
        }
        if (component.getFile().equals(openFile)) {
            return ComponentRelation.CURRENT_FILE;
        }
        if (openParsed != null) {
            if (openParsed.getImports().contains(component.getName())) {
                return ComponentRelation.IMPORTED;
            }
            if (openParsed.getTagUsages().containsKey(component.getTag())) {
                return ComponentRelation.USED;
            }
        }
        if (component.getKind() == ComponentKind.GLOBAL_REGISTRATION) {
            return ComponentRelation.GLOBAL;
        }
        return ComponentRelation.AVAILABLE;
    }

    public boolean isScanning() {
        return myScanning;
    }

    public long getLastScanTimestamp() {
        return myLastScanTimestamp;
    }

    /**
     * Schedules a new scan. Repeated calls are merged into a single scan.
     */
    public void requestRefresh() {
        if (myProject.isDisposed()) {
            return;
        }
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(this::runScan, REFRESH_DELAY_MS);
    }

    /**
     * Registers a listener that is notified on the EDT whenever new results are available.
     */
    public void addListener(@NotNull Consumer<List<VueComponentInfo>> listener) {
        myListeners.add(listener);
    }

    public void removeListener(@NotNull Consumer<List<VueComponentInfo>> listener) {
        myListeners.remove(listener);
    }

    private void runScan() {
        if (myProject.isDisposed()) {
            return;
        }
        if (DumbService.isDumb(myProject)) {
            // Wait until the project finished indexing, otherwise the file index is not available.
            DumbService.getInstance(myProject).runWhenSmart(this::requestRefresh);
            return;
        }

        myScanning = true;
        List<VueComponentInfo> result;
        try {
            result = myScanner.scan();
        } catch (com.intellij.openapi.application.ReadAction.CannotReadException e) {
            // The read action was cancelled, simply try again.
            myScanning = false;
            requestRefresh();
            return;
        } catch (IndexNotReadyException e) {
            // The project is still indexing, wait until it is ready.
            myScanning = false;
            DumbService.getInstance(myProject).runWhenSmart(this::requestRefresh);
            return;
        } catch (ProcessCanceledException e) {
            myScanning = false;
            throw e;
        } catch (Exception e) {
            LOG.warn("Scanning the Vue components failed", e);
            myScanning = false;
            return;
        }
        myScanning = false;
        myLastScanTimestamp = System.currentTimeMillis();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            // The list is replaced instead of modified, it is shared with the listeners.
            myComponents = List.copyOf(result);
            for (Consumer<List<VueComponentInfo>> listener : myListeners) {
                listener.accept(myComponents);
            }
        }, ModalityState.any());
    }

    private void subscribeToFileChanges() {
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(VirtualFileManager.VFS_CHANGES,
                new BulkFileListener() {
                    @Override
                    public void after(@NotNull List<? extends VFileEvent> events) {
                        for (VFileEvent event : events) {
                            if (isRelevant(event)) {
                                requestRefresh();
                                return;
                            }
                        }
                    }
                });
    }

    private static boolean isRelevant(@NotNull VFileEvent event) {
        String path = event.getPath();
        if (path == null) {
            return false;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return RELEVANT_EXTENSIONS.contains(extension);
    }

    @Override
    public void dispose() {
        myListeners.clear();
    }
}
