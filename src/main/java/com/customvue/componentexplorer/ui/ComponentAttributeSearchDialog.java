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

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.GridLayout;
import java.util.List;

/**
 * Asks for an attribute condition before searching a component in the whole project.
 *
 * <p>The attribute names are collected from the usages of the component, but the combo box stays
 * editable so an attribute that is not used yet can be searched as well.</p>
 */
public final class ComponentAttributeSearchDialog extends DialogWrapper {

    private final @NotNull VueComponentInfo myComponent;
    private final @NotNull ComboBox<String> myAttributeCombo;
    private final @NotNull JRadioButton myContains;
    private final @NotNull JRadioButton myNotContains;

    public ComponentAttributeSearchDialog(@Nullable Project project,
                                          @NotNull VueComponentInfo component,
                                          @NotNull List<String> attributes) {
        super(project, true);
        myComponent = component;

        myAttributeCombo = new ComboBox<>(attributes.toArray(new String[0]));
        myAttributeCombo.setEditable(true);
        if (!attributes.isEmpty()) {
            myAttributeCombo.setSelectedItem(attributes.get(0));
        }

        myContains = new JRadioButton(CustomVueComponentExplorerBundle.message(
                "dialog.attributeSearch.contains"), true);
        myNotContains = new JRadioButton(CustomVueComponentExplorerBundle.message(
                "dialog.attributeSearch.notContains"));
        ButtonGroup conditionGroup = new ButtonGroup();
        conditionGroup.add(myContains);
        conditionGroup.add(myNotContains);

        setTitle(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.title"));
        setOKButtonText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.search"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel conditions = new JPanel(new GridLayout(2, 1, 0, 4));
        conditions.add(myContains);
        conditions.add(myNotContains);

        JBLabel componentLabel = new JBLabel(myComponent.getName() + " (" + myComponent.getTag() + ")");
        JBLabel hint = new JBLabel(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.hint"));

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.component")), componentLabel)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.attribute")), myAttributeCombo)
                .addLabeledComponent(new JBLabel(CustomVueComponentExplorerBundle.message(
                        "dialog.attributeSearch.condition")), conditions)
                .addComponentToRightColumn(hint)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        if (getAttributeName().isEmpty()) {
            setErrorText(CustomVueComponentExplorerBundle.message("dialog.attributeSearch.emptyAttribute"));
            return;
        }
        super.doOKAction();
    }

    /** The normalized name of the attribute that was typed or selected. */
    public @NotNull String getAttributeName() {
        Object item = myAttributeCombo.getEditor().getItem();
        return item == null ? "" : ComponentParser.normalizeAttributeName(item.toString());
    }

    /** Whether the usage must contain the attribute or must not contain it. */
    public boolean isContainsAttribute() {
        return myContains.isSelected();
    }
}
