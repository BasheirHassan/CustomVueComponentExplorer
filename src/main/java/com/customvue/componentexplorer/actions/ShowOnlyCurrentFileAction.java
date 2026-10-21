package com.customvue.componentexplorer.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.ui.CustomVueComponentExplorerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Switches between showing only the components of the file that is currently open and showing
 * every component of the project.
 */
public final class ShowOnlyCurrentFileAction extends ToggleAction implements DumbAware {

    private final @NotNull CustomVueComponentExplorerPanel myPanel;

    public ShowOnlyCurrentFileAction(@NotNull CustomVueComponentExplorerPanel panel) {
        super(CustomVueComponentExplorerBundle.message("action.onlyCurrentFile"),
                CustomVueComponentExplorerBundle.message("action.onlyCurrentFile.description"),
                AllIcons.Nodes.Tag);
        myPanel = panel;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
        return myPanel.isOnlyCurrentFile();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean state) {
        myPanel.setOnlyCurrentFile(state);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(myPanel.getOpenFile() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
