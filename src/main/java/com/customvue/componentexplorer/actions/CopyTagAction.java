package com.customvue.componentexplorer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.ui.CustomVueComponentExplorerPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Copies the tag of the selected component into the clipboard.
 */
public final class CopyTagAction extends AnAction {

    private final @NotNull CustomVueComponentExplorerPanel myPanel;

    public CopyTagAction(@NotNull CustomVueComponentExplorerPanel panel) {
        super(CustomVueComponentExplorerBundle.message("action.copyTag"),
                CustomVueComponentExplorerBundle.message("action.copyTag.description"),
                null);
        myPanel = panel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        myPanel.copyTagOfSelection();
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
