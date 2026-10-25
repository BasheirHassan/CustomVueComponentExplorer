package com.customvue.componentexplorer.ui;

import com.intellij.icons.AllIcons;
import com.intellij.find.FindModel;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.actions.CopyPathAction;
import com.customvue.componentexplorer.actions.CopyTagAction;
import com.customvue.componentexplorer.actions.RefreshAction;
import com.customvue.componentexplorer.actions.ShowOnlyCurrentFileAction;
import com.customvue.componentexplorer.model.ComponentRelation;
import com.customvue.componentexplorer.model.VueComponentInfo;
import com.customvue.componentexplorer.model.VueComponentRow;
import com.customvue.componentexplorer.service.CustomVueComponentExplorerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The content of the <em>Custom Vue Component Explorer</em> tool window.
 *
 * <p>The list follows the file that is currently open in the editor: every component of the
 * project is shown together with its relation to that file (defined in it, imported by it, used
 * inside its template or registered globally).</p>
 */
public final class CustomVueComponentExplorerPanel extends JPanel implements Disposable {

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private final @NotNull Project myProject;
    private final @NotNull CustomVueComponentExplorerService myService;
    private final @NotNull CustomVueComponentExplorerTableModel myModel;
    private final @NotNull JBTable myTable;
    private final @NotNull SearchTextField mySearchField = new SearchTextField();
    private final @NotNull JBLabel myStatusLabel = new JBLabel();
    private final @NotNull CardLayout myCardLayout = new CardLayout();
    private final @NotNull JPanel myCenter = new JPanel(myCardLayout);
    private final @NotNull List<VueComponentRow> myRows = new ArrayList<>();
    private final @NotNull Consumer<List<VueComponentInfo>> myListener;

    private @Nullable VirtualFile myOpenFile;
    private boolean myOnlyCurrentFile = true;

    public CustomVueComponentExplorerPanel(@NotNull Project project) {
        super(new BorderLayout());
        myProject = project;
        myService = CustomVueComponentExplorerService.getInstance(project);
        myModel = new CustomVueComponentExplorerTableModel(project);
        myTable = new JBTable(myModel);

        add(createHeader(), BorderLayout.NORTH);
        myCenter.add(createTable(), CARD_TABLE);
        myCenter.add(createEmptyState(), CARD_EMPTY);
        add(myCenter, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        myListener = components -> refreshRows();
        myService.addListener(myListener);

        mySearchField.getTextEditor().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(@NotNull DocumentEvent event) {
                applyFilter();
            }
        });

        installPopupMenu();
        subscribeToEditor();

        myOpenFile = findSelectedFile();
        refreshRows();
        myService.requestRefresh();
    }

    /**
     * The file that is currently open in the editor.
     */
    public @Nullable VirtualFile getOpenFile() {
        return myOpenFile;
    }

    public boolean isOnlyCurrentFile() {
        return myOnlyCurrentFile;
    }

    public void setOnlyCurrentFile(boolean onlyCurrentFile) {
        myOnlyCurrentFile = onlyCurrentFile;
        applyFilter();
    }

    private @Nullable VirtualFile findSelectedFile() {
        VirtualFile[] selected = FileEditorManager.getInstance(myProject).getSelectedFiles();
        return selected.length > 0 ? selected[0] : null;
    }

    private void subscribeToEditor() {
        myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        myOpenFile = event.getNewFile();
                        refreshRows();
                    }

                    @Override
                    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
                        if (file.equals(myOpenFile)) {
                            myOpenFile = findSelectedFile();
                            refreshRows();
                        }
                    }
                });
    }

    private void refreshRows() {
        myRows.clear();
        myRows.addAll(myService.getRowsFor(myOpenFile));
        applyFilter();
    }

    private @NotNull JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(JBUI.Borders.empty(4, 6));

        mySearchField.getTextEditor().getEmptyText().setText(CustomVueComponentExplorerBundle.message("search.placeholder"));
        header.add(mySearchField, BorderLayout.CENTER);

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RefreshAction(myProject));
        group.addSeparator();
        group.add(new ShowOnlyCurrentFileAction(this));
        group.add(new CopyTagAction(this));
        group.add(new CopyPathAction(this));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("CustomVueComponentExplorerToolbar", group, true);
        toolbar.setTargetComponent(this);
        JComponent toolbarComponent = toolbar.getComponent();
        toolbarComponent.setBorder(JBUI.Borders.emptyLeft(4));
        header.add(toolbarComponent, BorderLayout.EAST);

        return header;
    }

    private @NotNull JComponent createTable() {
        myTable.setAutoCreateRowSorter(true);
        myTable.setShowGrid(false);
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myTable.setRowHeight(Math.max(myTable.getRowHeight(), JBUI.scale(22)));
        myTable.getEmptyText().setText(CustomVueComponentExplorerBundle.message("table.empty"));

        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_NAME, 200, new NameRenderer());
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_TAG, 130, null);
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_KIND, 110, null);
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_LOCATION, 220, new LocationRenderer());
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_LINE, 55, new LineRenderer());
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_USAGES, 80, null);
        configureColumn(CustomVueComponentExplorerTableModel.COLUMN_RELATION, 130, new RelationRenderer());

        myTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(@NotNull MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    navigateToSelectedComponent();
                }
            }

            @Override
            public void mousePressed(@NotNull MouseEvent event) {
                if (event.isPopupTrigger()) {
                    selectRowAt(event);
                }
            }

            @Override
            public void mouseReleased(@NotNull MouseEvent event) {
                if (event.isPopupTrigger()) {
                    selectRowAt(event);
                }
            }

            /** Makes the context menu act on the row that was right-clicked. */
            private void selectRowAt(@NotNull MouseEvent event) {
                int row = myTable.rowAtPoint(event.getPoint());
                if (row >= 0) {
                    myTable.setRowSelectionInterval(row, row);
                } else {
                    myTable.clearSelection();
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(myTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private void configureColumn(int index, int preferredWidth, @Nullable DefaultTableCellRenderer renderer) {
        TableColumn column = myTable.getColumnModel().getColumn(index);
        column.setPreferredWidth(JBUI.scale(preferredWidth));
        if (renderer != null) {
            column.setCellRenderer(renderer);
        }
    }

    private @NotNull JComponent createEmptyState() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(CustomVueComponentExplorerBundle.message("table.empty"));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(JBColor.GRAY);
        panel.add(label);
        return panel;
    }

    private @NotNull JComponent createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBorder(JBUI.Borders.empty(2, 8));
        myStatusLabel.setForeground(JBColor.GRAY);
        panel.add(myStatusLabel);
        return panel;
    }

    private void installPopupMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem open = new JMenuItem(CustomVueComponentExplorerBundle.message("action.openInEditor"));
        open.addActionListener(event -> navigateToSelectedComponent());
        popup.add(open);

        JMenuItem searchInProject = new JMenuItem(CustomVueComponentExplorerBundle.message("action.searchInProject"));
        searchInProject.addActionListener(event -> searchSelectionInProject());
        popup.add(searchInProject);

        popup.addSeparator();

        JMenuItem copyTag = new JMenuItem(CustomVueComponentExplorerBundle.message("action.copyTag"));
        copyTag.addActionListener(event -> copyTagOfSelection());
        popup.add(copyTag);

        JMenuItem copyPath = new JMenuItem(CustomVueComponentExplorerBundle.message("action.copyPath"));
        copyPath.addActionListener(event -> copyPathOfSelection());
        popup.add(copyPath);

        popup.addSeparator();

        JMenuItem refresh = new JMenuItem(CustomVueComponentExplorerBundle.message("action.refresh"));
        refresh.addActionListener(event -> myService.requestRefresh());
        popup.add(refresh);

        myTable.setComponentPopupMenu(popup);
    }

    private void applyFilter() {
        String query = mySearchField.getText().trim().toLowerCase(Locale.ROOT);
        List<VueComponentRow> visible = new ArrayList<>();

        for (VueComponentRow row : myRows) {
            if (isRelevant(row) && (query.isEmpty() || matches(row, query))) {
                visible.add(row);
            }
        }

        myModel.setRows(visible);
        myCardLayout.show(myCenter, visible.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        updateStatus(countComponents(visible), countComponents(myRows));
        updateToolWindowTitle();
    }

    /**
     * Counts the distinct components behind the given rows. A component that is used several times
     * inside the open page contributes several rows but is still a single component.
     */
    private static int countComponents(@NotNull List<VueComponentRow> rows) {
        Set<String> components = new HashSet<>();
        for (VueComponentRow row : rows) {
            VueComponentInfo component = row.getComponent();
            components.add(component.getFile().getPath() + '#' + component.getKind()
                    + '#' + component.getName() + '#' + component.getLine());
        }
        return components.size();
    }

    /**
     * A component belongs to the current page when it is defined in it, imported by it, used
     * inside its template or registered globally.
     */
    private boolean isRelevant(@NotNull VueComponentRow row) {
        if (!myOnlyCurrentFile || myOpenFile == null) {
            return true;
        }
        return row.getRelation() != ComponentRelation.AVAILABLE;
    }

    private boolean matches(@NotNull VueComponentRow row, @NotNull String query) {
        VueComponentInfo component = row.getComponent();
        return component.getName().toLowerCase(Locale.ROOT).contains(query)
                || component.getTag().toLowerCase(Locale.ROOT).contains(query)
                || component.getKind().getLabel().toLowerCase(Locale.ROOT).contains(query)
                || row.getRelation().getLabel().toLowerCase(Locale.ROOT).contains(query)
                || myModel.getRelativePath(component).toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateStatus(int visibleCount, int totalCount) {
        String text;
        if (myService.isScanning()) {
            text = CustomVueComponentExplorerBundle.message("status.scanning");
        } else if (totalCount == 0) {
            text = CustomVueComponentExplorerBundle.message("status.empty");
        } else if (myOpenFile == null) {
            text = CustomVueComponentExplorerBundle.message("status.count", totalCount);
        } else if (visibleCount == totalCount) {
            text = CustomVueComponentExplorerBundle.message("status.count", totalCount);
        } else {
            text = CustomVueComponentExplorerBundle.message("status.filtered", visibleCount, totalCount);
        }
        myStatusLabel.setText(text);
    }

    private void updateToolWindowTitle() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject)
                .getToolWindow(CustomVueComponentExplorerToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        String title = CustomVueComponentExplorerBundle.message("toolwindow.title");
        if (myOpenFile != null) {
            title = title + " — " + myOpenFile.getName();
        }
        toolWindow.setTitle(title);
    }

    private @Nullable VueComponentRow getSelectedRow() {
        int viewRow = myTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = myTable.convertRowIndexToModel(viewRow);
        return modelRow >= 0 && modelRow < myModel.getRowCount() ? myModel.getRow(modelRow) : null;
    }

    private void navigateToSelectedComponent() {
        VueComponentRow row = getSelectedRow();
        if (row == null) {
            return;
        }
        new OpenFileDescriptor(myProject, row.getTargetFile(), row.getTargetLine(), 0).navigate(true);
    }

    /**
     * Searches the selected component in the whole project.
     *
     * <p>Both the declared name and the kebab-case tag are searched, because Vue templates accept
     * both forms ({@code <AppButton>} and {@code <app-button>}). The results are shown in the
     * standard <em>Find</em> tool window.</p>
     */
    public void searchSelectionInProject() {
        VueComponentRow row = getSelectedRow();
        if (row == null) {
            return;
        }

        FindModel model = new FindModel();
        model.setStringToFind(buildComponentSearchPattern(row.getComponent()));
        model.setRegularExpressions(true);
        model.setCaseSensitive(false);
        model.setWholeWordsOnly(false);
        model.setMultiline(false);
        model.setCustomScope(false);
        model.setProjectScope(true);
        model.setSearchInProjectFiles(true);
        FindInProjectManager.getInstance(myProject).findInPath(model);
    }

    private static @NotNull String buildComponentSearchPattern(@NotNull VueComponentInfo component) {
        return "\\b(?:" + Pattern.quote(component.getName())
                + '|' + Pattern.quote(component.getTag()) + ")\\b";
    }

    public void copyTagOfSelection() {
        VueComponentRow row = getSelectedRow();
        if (row != null) {
            copyToClipboard(row.getComponent().getTag());
        }
    }

    public void copyPathOfSelection() {
        VueComponentRow row = getSelectedRow();
        if (row != null) {
            copyToClipboard(myModel.getRelativePath(row.getComponent()));
        }
    }

    private static void copyToClipboard(@NotNull String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

    @Override
    public void dispose() {
        myService.removeListener(myListener);
    }

    /**
     * Renders the component name together with a small icon and shows the navigation target.
     */
    private static final class NameRenderer extends DefaultTableCellRenderer {

        private NameRenderer() {
            setIcon(AllIcons.Nodes.Tag);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (component instanceof JLabel label && table.getModel() instanceof CustomVueComponentExplorerTableModel model) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < model.getRowCount()) {
                    VueComponentRow componentRow = model.getRow(modelRow);
                    label.setToolTipText(CustomVueComponentExplorerBundle.message("tooltip.jumpTo",
                            componentRow.getTargetFile().getName(), componentRow.getTargetLine() + 1));
                }
            }
            return component;
        }
    }

    /**
     * Shows the line of the navigation target. A component that is used several times inside the
     * open page contributes one row per line.
     */
    private static final class LineRenderer extends DefaultTableCellRenderer {

        private LineRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (component instanceof JLabel label) {
                label.setForeground(selected ? table.getSelectionForeground() : JBColor.GRAY);
            }
            return component;
        }
    }

    /**
     * Shows the full path of the file as a tooltip.
     */
    private static final class LocationRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (component instanceof JLabel label && table.getModel() instanceof CustomVueComponentExplorerTableModel model) {
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < model.getRowCount()) {
                    label.setToolTipText(model.getComponent(modelRow).getFile().getPath());
                }
            }
            return component;
        }
    }

    /**
     * Highlights the relation of the component to the file that is currently open.
     */
    private static final class RelationRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (component instanceof JLabel label && table.getModel() instanceof CustomVueComponentExplorerTableModel model) {
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < model.getRowCount()) {
                    ComponentRelation relation = model.getRow(modelRow).getRelation();
                    label.setIcon(relation == ComponentRelation.AVAILABLE ? null : AllIcons.Nodes.Tag);
                    label.setToolTipText(CustomVueComponentExplorerBundle.message("relation.tooltip", relation.getLabel()));
                }
            }
            return component;
        }
    }
}
