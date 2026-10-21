package com.customvue.componentexplorer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.ui.CustomVueComponentExplorerToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the <em>Custom Vue Component Explorer</em> tool window.
 */
public final class ShowToolWindowAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        CustomVueComponentExplorerToolWindowFactory.activate(event.getProject());
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && !project.isDisposed());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
