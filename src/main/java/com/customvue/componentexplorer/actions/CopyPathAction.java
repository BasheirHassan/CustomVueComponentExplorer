package com.customvue.componentexplorer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.ui.CustomVueComponentExplorerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Copies the path of the selected component into the clipboard.
 */
public final class CopyPathAction extends AnAction {

    private final @NotNull CustomVueComponentExplorerPanel myPanel;

    public CopyPathAction(@NotNull CustomVueComponentExplorerPanel panel) {
        super(CustomVueComponentExplorerBundle.message("action.copyPath"),
                CustomVueComponentExplorerBundle.message("action.copyPath.description"),
                null);
        myPanel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        myPanel.copyPathOfSelection();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(myPanel.isShowing());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
