package com.customvue.componentexplorer.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates the content of the <em>Custom Vue Component Explorer</em> tool window.
 */
public final class CustomVueComponentExplorerToolWindowFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_ID = "Custom Vue Component Explorer";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CustomVueComponentExplorerPanel panel = new CustomVueComponentExplorerPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * Activates the tool window of the given project.
     */
    public static void activate(@Nullable Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.activate(null, true);
        }
    }
}
