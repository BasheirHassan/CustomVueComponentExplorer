plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.customvue.componentexplorer"
version = "1.2.0"
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Use the locally installed WebStorm so no IDE distribution has to be downloaded.
        // Replace with `create(IntelliJPlatformType.WebStorm, "2026.2.1")` to build against a remote IDE.
        local(file("C:/Program Files/JetBrains/WebStorm 2026.2.1"))
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.customvue.componentexplorer"
        name = "Custom Vue Component Explorer"
        version = project.version.toString()

        description = """
            <p>Displays a live list of the custom Vue components of the current project.</p>
            <ul>
                <li>Scans every <code>.vue</code> single file component and detects its declared name
                    (<code>defineOptions</code>, <code>defineComponent</code>, <code>export default</code>).</li>
                <li>Detects globally registered components (<code>app.component(...)</code>), components declared
                    through the <code>components</code> option and custom web elements
                    (<code>customElements.define(...)</code>).</li>
                <li>Counts how many times each component is used inside templates.</li>
                <li>Navigate to the declaration, copy the tag name or the file path directly from the list.</li>
                <li>Searches the selected component in the whole project from the context menu.</li>
                <li>Searches component usages that contain or do not contain a chosen attribute.</li>
            </ul>
        """

        vendor {
            name = "Custom Vue Component Explorer"
        }

        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }

    // The plugin has no settings pages, so there is nothing to index.
    buildSearchableOptions = false

    pluginVerification {
        ides {
            local(file("C:/Program Files/JetBrains/WebStorm 2026.2.1"))
        }
    }
}
