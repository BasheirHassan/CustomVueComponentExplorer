package com.customvue.componentexplorer.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.model.VueComponentInfo;
import com.customvue.componentexplorer.model.VueComponentRow;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * The table model behind the list of the discovered components.
 */
public final class CustomVueComponentExplorerTableModel extends AbstractTableModel {

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_TAG = 1;
    public static final int COLUMN_KIND = 2;
    public static final int COLUMN_LOCATION = 3;
    public static final int COLUMN_USAGES = 4;
    public static final int COLUMN_RELATION = 5;

    private final @NotNull Project myProject;
    private final @NotNull List<VueComponentRow> myRows = new ArrayList<>();
    private final @NotNull String[] myColumnNames = {
            CustomVueComponentExplorerBundle.message("table.column.name"),
            CustomVueComponentExplorerBundle.message("table.column.tag"),
            CustomVueComponentExplorerBundle.message("table.column.kind"),
            CustomVueComponentExplorerBundle.message("table.column.location"),
            CustomVueComponentExplorerBundle.message("table.column.usages"),
            CustomVueComponentExplorerBundle.message("table.column.relation")
    };

    public CustomVueComponentExplorerTableModel(@NotNull Project project) {
        myProject = project;
    }

    public void setRows(@NotNull List<VueComponentRow> rows) {
        myRows.clear();
        myRows.addAll(rows);
        fireTableDataChanged();
    }

    public @NotNull List<VueComponentRow> getRows() {
        return myRows;
    }

    public @NotNull VueComponentRow getRow(int index) {
        return myRows.get(index);
    }

    public @NotNull VueComponentInfo getComponent(int index) {
        return myRows.get(index).getComponent();
    }

    /**
     * The path of the file relative to the project directory.
     */
    public @NotNull String getRelativePath(@NotNull VueComponentInfo component) {
        String path = ProjectUtil.calcRelativeToProjectPath(component.getFile(), myProject);
        return path == null ? component.getFile().getPath() : path;
    }

    @Override
    public int getRowCount() {
        return myRows.size();
    }

    @Override
    public int getColumnCount() {
        return myColumnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return myColumnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == COLUMN_USAGES ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        VueComponentRow row = myRows.get(rowIndex);
        VueComponentInfo info = row.getComponent();
        return switch (columnIndex) {
            case COLUMN_NAME -> info.getName();
            case COLUMN_TAG -> info.getTag();
            case COLUMN_KIND -> info.getKind().getLabel();
            case COLUMN_LOCATION -> getRelativePath(info);
            case COLUMN_USAGES -> info.getUsageCount();
            case COLUMN_RELATION -> row.getRelation().getLabel();
            default -> "";
        };
    }
}
