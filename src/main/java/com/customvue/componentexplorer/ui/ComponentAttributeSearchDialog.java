package com.customvue.componentexplorer.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
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
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Asks for one or more attribute conditions before searching a component in the whole project.
 *
 * <p>The attribute names are collected from the usages of the component, but the combo boxes stay
 * editable so an attribute that is not used yet can be searched as well.</p>
 */
public final class ComponentAttributeSearchDialog extends DialogWrapper {

    private final @NotNull VueComponentInfo myComponent;
    private final @NotNull List<String> myAttributes;
    private final @NotNull List<ConditionRow> myConditionRows = new ArrayList<>();
    private final @NotNull JPanel myConditionsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
    private final @NotNull ComboBox<MatchMode> myMatchMode = new ComboBox<>(MatchMode.values());

    public ComponentAttributeSearchDialog(@Nullable Project project,
                                          @NotNull VueComponentInfo component,
                                          @NotNull List<String> attributes) {
        super(project, true);
        myComponent = component;
        myAttributes = attributes;

        myMatchMode.setSelectedItem(MatchMode.ALL);
        addConditionRow(attributes.isEmpty() ? null : attributes.get(0));

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

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.component")), componentLabel)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.attributes")), myConditionsPanel)
                .addComponentToRightColumn(addPanel)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.match")), myMatchMode)
                .addComponentToRightColumn(hint)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        if (getConditions().isEmpty()) {
            setErrorText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.emptyAttribute"));
            return;
        }
        super.doOKAction();
    }

    /** The attribute conditions that were entered. Empty rows are ignored. */
    public @NotNull List<SearchCondition> getConditions() {
        List<SearchCondition> conditions = new ArrayList<>(myConditionRows.size());
        for (ConditionRow row : myConditionRows) {
            String attribute = row.getAttributeName();
            if (!attribute.isEmpty()) {
                conditions.add(new SearchCondition(attribute, row.isContains()));
            }
        }
        return conditions;
    }

    /** Whether all conditions have to match or only one of them. */
    public boolean isMatchAll() {
        return myMatchMode.getSelectedItem() == MatchMode.ALL;
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

    /** A single attribute condition row: the attribute name and the contains/does not contain mode. */
    private final class ConditionRow {

        private final @NotNull JPanel myPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        private final @NotNull ComboBox<String> myAttributeCombo;
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

            myConditionCombo = new ComboBox<>(AttributeCondition.values());
            myConditionCombo.setSelectedItem(AttributeCondition.CONTAINS);

            myRemoveButton.setToolTipText(CustomVueComponentExplorerBundle.message(
                    "dialog.attributeSearch.removeCondition"));
            myRemoveButton.addActionListener(event -> removeConditionRow(this));

            myPanel.add(myAttributeCombo);
            myPanel.add(myConditionCombo);
            myPanel.add(myRemoveButton);
        }

        private @NotNull String getAttributeName() {
            Object item = myAttributeCombo.getEditor().getItem();
            return item == null ? "" : ComponentParser.normalizeAttributeName(item.toString());
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
    public record SearchCondition(@NotNull String attribute, boolean contains) {
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
