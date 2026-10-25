package com.customvue.componentexplorer.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.customvue.componentexplorer.CustomVueComponentExplorerBundle;
import com.customvue.componentexplorer.model.VueComponentInfo;
import com.customvue.componentexplorer.scan.ComponentParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Asks for one or more attribute conditions and an optional search folder before searching a
 * component in the whole project.
 *
 * <p>The attribute names are collected from the usages of the component, but the combo boxes stay
 * editable so an attribute that is not used yet can be searched as well. The value and the search
 * folder are optional.</p>
 */
public final class ComponentAttributeSearchDialog extends DialogWrapper {

    private final @Nullable Project myProject;
    private final @NotNull VueComponentInfo myComponent;
    private final @NotNull List<String> myAttributes;
    private final @NotNull List<ConditionRow> myConditionRows = new ArrayList<>();
    private final @NotNull JPanel myConditionsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
    private final @NotNull ComboBox<MatchMode> myMatchMode = new ComboBox<>(MatchMode.values());
    private final @NotNull TextFieldWithBrowseButton myDirectoryField = new TextFieldWithBrowseButton();
    private @NotNull String mySearchDirectory = "";

    public ComponentAttributeSearchDialog(@Nullable Project project,
                                          @NotNull VueComponentInfo component,
                                          @NotNull List<String> attributes) {
        super(project, true);
        myProject = project;
        myComponent = component;
        myAttributes = attributes;

        myMatchMode.setSelectedItem(MatchMode.ALL);
        addConditionRow(attributes.isEmpty() ? null : attributes.get(0));

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.browseTitle"));
        descriptor.setDescription(CustomVueComponentExplorerBundle.message(
                "dialog.attributeSearch.browseDescription"));
        myDirectoryField.addBrowseFolderListener(new TextBrowseFolderListener(descriptor, project));
        myDirectoryField.setTextFieldPreferredWidth(45);
        myDirectoryField.getTextField().setToolTipText(
                CustomVueComponentExplorerBundle.message("dialog.attributeSearch.scopeHint"));

        setTitle(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.title"));
        setOKButtonText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.search"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JButton addButton = new JButton(CustomVueComponentExplorerBundle.message(
                "dialog.attributeSearch.addCondition"));
        addButton.addActionListener(event -> addConditionRow(null));
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addPanel.add(addButton);

        JBLabel componentLabel = new JBLabel(myComponent.getName() + " (" + myComponent.getTag() + ")");
        JBLabel hint = new JBLabel(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.hint"));
        JBLabel scopeHint = new JBLabel(CustomVueComponentExplorerBundle.message(
                "dialog.attributeSearch.scopeHint"));

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.component")), componentLabel)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.attributes")), myConditionsPanel)
                .addComponentToRightColumn(addPanel)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.match")), myMatchMode)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.scope")), myDirectoryField)
                .addComponentToRightColumn(scopeHint)
                .addComponentToRightColumn(hint)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        if (getConditions().isEmpty()) {
            setErrorText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.emptyAttribute"));
            return;
        }

        String directory = resolveSearchDirectory();
        if (directory == null) {
            setErrorText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.invalidScope"));
            return;
        }
        mySearchDirectory = directory;

        super.doOKAction();
    }

    /** The attribute conditions that were entered. Empty rows are ignored. */
    public @NotNull List<SearchCondition> getConditions() {
        List<SearchCondition> conditions = new ArrayList<>(myConditionRows.size());
        for (ConditionRow row : myConditionRows) {
            String attribute = row.getAttributeName();
            if (!attribute.isEmpty()) {
                conditions.add(new SearchCondition(attribute, row.getValue(), row.isContains()));
            }
        }
        return conditions;
    }

    /** Whether all conditions have to match or only one of them. */
    public boolean isMatchAll() {
        return myMatchMode.getSelectedItem() == MatchMode.ALL;
    }

    /**
     * The chosen search folder, or an empty string when the whole project should be searched.
     */
    public @NotNull String getSearchDirectory() {
        return mySearchDirectory;
    }

    private void addConditionRow(@Nullable String attribute) {
        ConditionRow row = new ConditionRow(attribute);
        myConditionRows.add(row);
        myConditionsPanel.add(row.getPanel());
        updateRemoveButtons();
        myConditionsPanel.revalidate();
        myConditionsPanel.repaint();
        packIfShowing();
    }

    private void removeConditionRow(@NotNull ConditionRow row) {
        if (!myConditionRows.remove(row)) {
            return;
        }
        myConditionsPanel.remove(row.getPanel());
        updateRemoveButtons();
        myConditionsPanel.revalidate();
        myConditionsPanel.repaint();
        packIfShowing();
    }

    private void updateRemoveButtons() {
        boolean canRemove = myConditionRows.size() > 1;
        for (ConditionRow row : myConditionRows) {
            row.setRemoveEnabled(canRemove);
        }
    }

    private void packIfShowing() {
        Window window = getWindow();
        if (window != null && window.isShowing()) {
            pack();
        }
    }

    private @Nullable String resolveSearchDirectory() {
        String text = myDirectoryField.getText();
        if (text == null || text.isBlank()) {
            return "";
        }

        String entered = text.trim();
        if (entered.length() >= 2 && entered.startsWith("\"") && entered.endsWith("\"")) {
            entered = entered.substring(1, entered.length() - 1).trim();
        }

        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        String path = FileUtil.toSystemIndependentName(entered);
        VirtualFile directory = fileSystem.findFileByPath(path);
        if (directory == null && myProject != null && myProject.getBasePath() != null) {
            directory = fileSystem.findFileByPath(FileUtil.toSystemIndependentName(
                    myProject.getBasePath() + "/" + entered));
        }
        return directory != null && directory.isDirectory() ? directory.getPath() : null;
    }

    /** A single condition row: attribute, optional value and the contains/does not contain mode. */
    private final class ConditionRow {

        private final @NotNull JPanel myPanel = new JPanel(new GridBagLayout());
        private final @NotNull ComboBox<String> myAttributeCombo;
        private final @NotNull JBTextField myValueField;
        private final @NotNull ComboBox<AttributeCondition> myConditionCombo;
        private final @NotNull JButton myRemoveButton = new JButton("\u00D7");

        private ConditionRow(@Nullable String attribute) {
            myAttributeCombo = new ComboBox<>(myAttributes.toArray(new String[0]));
            myAttributeCombo.setEditable(true);
            if (attribute != null) {
                myAttributeCombo.setSelectedItem(attribute);
            } else {
                myAttributeCombo.getEditor().setItem("");
            }

            myValueField = new JBTextField();
            myValueField.setColumns(10);
            myValueField.getEmptyText().setText(CustomVueComponentExplorerBundle.message(
                    "dialog.attributeSearch.valuePlaceholder"));
            myValueField.setToolTipText(CustomVueComponentExplorerBundle.message(
                    "dialog.attributeSearch.valueHint"));

            myConditionCombo = new ComboBox<>(AttributeCondition.values());
            myConditionCombo.setSelectedItem(AttributeCondition.CONTAINS);
            myConditionCombo.setToolTipText(CustomVueComponentExplorerBundle.message(
                    "dialog.attributeSearch.conditionHint"));

            myRemoveButton.setToolTipText(CustomVueComponentExplorerBundle.message(
                    "dialog.attributeSearch.removeCondition"));
            myRemoveButton.addActionListener(event -> removeConditionRow(this));

            addField(myAttributeCombo, 0, 0.34, 4);
            addField(myValueField, 1, 0.26, 4);
            addField(myConditionCombo, 2, 0.40, 4);
            addField(myRemoveButton, 3, 0, 0);
        }

        private void addField(@NotNull JComponent component, int column, double weight, int rightInset) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = column;
            constraints.gridy = 0;
            constraints.weightx = weight;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, 0, 0, rightInset);
            myPanel.add(component, constraints);
        }

        private @NotNull String getAttributeName() {
            Object item = myAttributeCombo.getEditor().getItem();
            return item == null ? "" : ComponentParser.normalizeAttributeName(item.toString());
        }

        private @NotNull String getValue() {
            return myValueField.getText() == null ? "" : myValueField.getText().trim();
        }

        private boolean isContains() {
            return myConditionCombo.getSelectedItem() == AttributeCondition.CONTAINS;
        }

        private void setRemoveEnabled(boolean enabled) {
            myRemoveButton.setEnabled(enabled);
        }

        private @NotNull JPanel getPanel() {
            return myPanel;
        }
    }

    /** One condition of the project wide attribute search. */
    public record SearchCondition(@NotNull String attribute, @NotNull String value, boolean contains) {

        public SearchCondition(@NotNull String attribute, boolean contains) {
            this(attribute, "", contains);
        }
    }

    private enum AttributeCondition {
        CONTAINS("dialog.attributeSearch.contains"),
        NOT_CONTAINS("dialog.attributeSearch.notContains");

        private final @NotNull String myKey;

        AttributeCondition(@NotNull String key) {
            myKey = key;
        }

        @Override
        public String toString() {
            return CustomVueComponentExplorerBundle.message(myKey);
        }
    }

    private enum MatchMode {
        ALL("dialog.attributeSearch.matchAll"),
        ANY("dialog.attributeSearch.matchAny");

        private final @NotNull String myKey;

        MatchMode(@NotNull String key) {
            myKey = key;
        }

        @Override
        public String toString() {
            return CustomVueComponentExplorerBundle.message(myKey);
        }
    }
}
