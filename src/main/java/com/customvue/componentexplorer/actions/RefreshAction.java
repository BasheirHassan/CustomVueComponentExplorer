package com.customvue.componentexplorer.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.service.CustomVueComponentExplorerService;
import org.jetbrains.annotations.NotNull;

/**
 * Starts a new scan of the project.
 */
public final class RefreshAction extends AnAction {

    private final @NotNull Project myProject;

    public RefreshAction(@NotNull Project project) {
        super(CustomVueComponentExplorerBundle.message("action.refresh"),
                CustomVueComponentExplorerBundle.message("action.refresh.description"),
                AllIcons.Actions.Refresh);
        myProject = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (!myProject.isDisposed()) {
            CustomVueComponentExplorerService.getInstance(myProject).requestRefresh();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(!myProject.isDisposed());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
