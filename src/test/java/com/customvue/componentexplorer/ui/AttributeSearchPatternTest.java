package com.customvue.componentexplorer.ui;

import com.customvue.componentexplorer.ui.ComponentAttributeSearchDialog.SearchCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeSearchPatternTest {

    private static final String TEXT = """
            <AppButton label="Primary" primary :disabled="flag" />
            <app-button label="Secondary" />
            <AppButton label="a > b" primary />
            <AppButton title="my primary thing" />
            """;

    @Test
    void matchesTagsThatContainAllAttributesInAnyVueForm() {
        String pattern = CustomVueComponentExplorerPanel.buildAttributeSearchPattern(
                "AppButton", "app-button",
                List.of(new SearchCondition("primary", true),
                        new SearchCondition("disabled", true)), true);

        assertEquals(List.of("<AppButton label=\"Primary\" primary :disabled=\"flag\" />"),
                matches(pattern));
    }

    @Test
    void excludesTagsThatContainAnExcludedAttribute() {
        String pattern = CustomVueComponentExplorerPanel.buildAttributeSearchPattern(
                "AppButton", "app-button",
                List.of(new SearchCondition("primary", true),
                        new SearchCondition("disabled", false)), true);

        assertEquals(List.of("<AppButton label=\"a > b\" primary />"), matches(pattern));
    }

    @Test
    void matchesTagsThatSatisfyAnyCondition() {
        String pattern = CustomVueComponentExplorerPanel.buildAttributeSearchPattern(
                "AppButton", "app-button",
                List.of(new SearchCondition("primary", true),
                        new SearchCondition("label", true)), false);

        assertEquals(List.of(
                        "<AppButton label=\"Primary\" primary :disabled=\"flag\" />",
                        "<app-button label=\"Secondary\" />",
                        "<AppButton label=\"a > b\" primary />"),
                matches(pattern));
    }

    @Test
    void doesNotMatchAttributeNamesThatOnlyAppearInsideAValue() {
        String pattern = CustomVueComponentExplorerPanel.buildAttributeSearchPattern(
                "AppButton", "app-button",
                List.of(new SearchCondition("primary", true)), true);

        assertFalse(matches(pattern).contains("<AppButton title=\"my primary thing\" />"));
    }

    @Test
    void matchesTheKebabCaseTagAsWell() {
        String pattern = CustomVueComponentExplorerPanel.buildAttributeSearchPattern(
                "AppButton", "app-button",
                List.of(new SearchCondition("label", true)), true);

        assertTrue(matches(pattern).contains("<app-button label=\"Secondary\" />"));
    }

    private static List<String> matches(String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(TEXT)
                .results()
                .map(match -> match.group())
                .toList();
    }
}
