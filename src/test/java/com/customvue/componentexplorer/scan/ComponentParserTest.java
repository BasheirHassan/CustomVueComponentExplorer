package com.customvue.componentexplorer.scan;

import com.customvue.componentexplorer.model.ComponentKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentParserTest {

    @Test
    void detectsNameFromDefineOptions() {
        ParsedFile parsed = ComponentParser.parseVueFile("app-button", """
                <script setup>
                defineOptions({
                  name: 'AppButton',
                })
                </script>
                """);

        ComponentDeclaration declaration = parsed.getDeclarations().get(0);
        assertEquals("AppButton", declaration.getName());
        assertEquals(ComponentKind.SINGLE_FILE, declaration.getKind());
        assertEquals(3, declaration.getLine());
    }

    @Test
    void detectsNameFromDefineComponent() {
        ParsedFile parsed = ComponentParser.parseVueFile("user-card", """
                <script>
                export default defineComponent({
                  name: 'UserCard',
                })
                </script>
                """);

        assertEquals("UserCard", parsed.getDeclarations().get(0).getName());
    }

    @Test
    void ignoresNestedNameProperty() {
        ParsedFile parsed = ComponentParser.parseVueFile("tricky", """
                <script>
                export default defineComponent({
                  props: { name: { type: String } },
                  name: 'RealName',
                })
                </script>
                """);

        assertEquals("RealName", parsed.getDeclarations().get(0).getName());
    }

    @Test
    void derivesNameFromFileName() {
        ParsedFile parsed = ComponentParser.parseVueFile("legacy-widget", """
                <template>
                  <div>no script block</div>
                </template>
                """);

        assertEquals("LegacyWidget", parsed.getDeclarations().get(0).getName());
        assertEquals("legacy-widget", ComponentParser.toKebabCase(parsed.getDeclarations().get(0).getName()));
    }

    @Test
    void detectsGlobalRegistrations() {
        ParsedFile parsed = ComponentParser.parseScriptFile("main", """
                const app = createApp(App)
                app.component('AppButton', AppButton)
                app.component('UserCard', UserCard)
                """);

        List<ComponentDeclaration> declarations = parsed.getDeclarations();
        assertEquals(2, declarations.size());
        assertEquals("AppButton", declarations.get(0).getName());
        assertEquals(ComponentKind.GLOBAL_REGISTRATION, declarations.get(0).getKind());
        assertEquals("UserCard", declarations.get(1).getName());
    }

    @Test
    void detectsCustomElements() {
        ParsedFile parsed = ComponentParser.parseScriptFile("elements", """
                customElements.define('my-element', MyElement)
                """);

        assertEquals(1, parsed.getDeclarations().size());
        assertEquals("my-element", parsed.getDeclarations().get(0).getName());
        assertEquals(ComponentKind.WEB_COMPONENT, parsed.getDeclarations().get(0).getKind());
    }

    @Test
    void detectsComponentsOption() {
        ParsedFile parsed = ComponentParser.parseScriptFile("options", """
                export default {
                  components: {
                    MyLocalComp,
                    'other-comp': OtherComp,
                  },
                }
                """);

        List<ComponentDeclaration> declarations = parsed.getDeclarations();
        assertEquals(2, declarations.size());
        assertEquals("MyLocalComp", declarations.get(0).getName());
        assertEquals("other-comp", declarations.get(1).getName());
        assertEquals(ComponentKind.OPTIONS_REGISTRATION, declarations.get(0).getKind());
    }

    @Test
    void countsTagUsages() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <AppButton label="a" />
                  <app-button label="b" />
                  <UserCard />
                </template>
                """);

        Map<String, Integer> usages = parsed.getTagUsages();
        assertEquals(2, usages.get("app-button"));
        assertEquals(1, usages.get("user-card"));
    }

    @Test
    void detectsImports() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <script setup>
                import AppButton from './components/AppButton.vue'
                import { UserCard, LegacyWidget as Old } from './components'
                import * as Icons from './icons'
                import type { Ref } from 'vue'
                import './style.css'
                </script>
                """);

        assertTrue(parsed.getImports().contains("AppButton"));
        assertTrue(parsed.getImports().contains("UserCard"));
        assertTrue(parsed.getImports().contains("Old"));
        assertTrue(parsed.getImports().contains("Icons"));
        assertTrue(parsed.getImports().contains("Ref"));
        assertFalse(parsed.getImports().contains("style.css"));
    }

    @Test
    void detectsDefaultImportWithNamedImports() {
        ParsedFile parsed = ComponentParser.parseScriptFile("main", """
                import Default, { Named } from './module'
                """);

        assertTrue(parsed.getImports().contains("Default"));
        assertTrue(parsed.getImports().contains("Named"));
    }

    @Test
    void remembersTheLineOfEveryTagUsage() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <AppButton />
                  <app-button />
                </template>
                """);

        assertEquals(List.of(2, 3), parsed.getTagLines().get("app-button"));
        assertEquals(2, parsed.getTagUsages().get("app-button"));
    }

    @Test
    void remembersAllUsageLinesOfAComponentUsedSeveralTimes() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <AppButton label="a" />
                  <UserCard />
                  <app-button label="b" />
                  <AppButton label="c" />
                </template>
                """);

        assertEquals(List.of(2, 4, 5), parsed.getTagLines().get("app-button"));
        assertEquals(3, parsed.getTagUsages().get("app-button"));
        assertEquals(List.of(3), parsed.getTagLines().get("user-card"));
    }

    @Test
    void storesOneLineWhenATagIsUsedTwiceOnTheSameLine() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <AppButton /><app-button />
                </template>
                """);

        assertEquals(List.of(2), parsed.getTagLines().get("app-button"));
        assertEquals(2, parsed.getTagUsages().get("app-button"));
    }

    @Test
    void collectsTheAttributesOfEveryTagUsage() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <AppButton label="Primary" primary :disabled="flag" @click.stop="go" />
                  <app-button label="Secondary" />
                </template>
                """);

        List<TagUsage> usages = parsed.getTagOccurrences().get("app-button");
        assertEquals(2, usages.size());
        assertEquals(Set.of("label", "primary", "disabled", "click.stop"), usages.get(0).getAttributes());
        assertEquals(Set.of("label"), usages.get(1).getAttributes());
        assertEquals(2, usages.get(0).getLine());
        assertEquals(3, usages.get(1).getLine());
    }

    @Test
    void readsAttributesAcrossLinesAndIgnoresValues() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <UserCard
                    :user-name="a > b"
                    v-bind:role="role"
                    @select="onSelect"
                  />
                </template>
                """);

        List<TagUsage> usages = parsed.getTagOccurrences().get("user-card");
        assertEquals(1, usages.size());
        assertEquals(2, usages.get(0).getLine());
        assertEquals(Set.of("user-name", "role", "select"), usages.get(0).getAttributes());
    }

    @Test
    void storesAnEmptyAttributeSetForTagsWithoutAttributes() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <UserCard />
                </template>
                """);

        assertTrue(parsed.getTagOccurrences().get("user-card").get(0).getAttributes().isEmpty());
    }

    @Test
    void remembersTheLineOfEveryImport() {
        ParsedFile parsed = ComponentParser.parseScriptFile("main", """
                import AppButton from './AppButton.vue'
                import { UserCard } from './UserCard.vue'
                """);

        assertEquals(1, parsed.getImportLines().get("AppButton"));
        assertEquals(2, parsed.getImportLines().get("UserCard"));
    }

    @Test
    void convertsBetweenNameAndTag() {
        assertEquals("my-button", ComponentParser.toKebabCase("MyButton"));
        assertEquals("MyButton", ComponentParser.toPascalCase("my-button"));
        assertEquals("MyButton", ComponentParser.toPascalCase("MyButton"));
    }

    @Test
    void doesNotTreatClosingTagsAsUsages() {
        ParsedFile parsed = ComponentParser.parseVueFile("app", """
                <template>
                  <div><span>x</span></div>
                </template>
                """);

        // Only the opening tags are counted, the closing ones are ignored.
        assertEquals(1, parsed.getTagUsages().get("div"));
        assertEquals(1, parsed.getTagUsages().get("span"));
    }
}
