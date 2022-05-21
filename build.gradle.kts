import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.grammarkit") version "2021.2.2"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.intellij") version "1.4.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("net.java.dev.jna:jna:5.11.0")
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

sourceSets["main"].java.srcDirs("src/main/gen")

tasks {
    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = it
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
        }
    }

    generateParser {
        source.set("src/main/resources/grammar/Jakt.bnf")

        // optional, task-specific root for the generated files. Default: none
        targetRoot.set("src/main/gen")

        // path to a parser file, relative to the targetRoot
        pathToParser.set("org/serenityos/jakt/parser/JaktParser.java")

        // path to a directory with generated psi files, relative to the targetRoot
        pathToPsiRoot.set("org/serenityos/jakt/psi")

        // if set, the plugin will remove a parser output file and psi output directory before generating new ones. Default: false
        purgeOldFiles.set(true)
    }

    generateLexer {
        // source flex file
        source.set("src/main/resources/grammar/Jakt.flex")

        // target directory for lexer
        targetDir.set("src/main/gen/org/serenityos/jakt/lexer/")

        // target classname, target file will be targetDir/targetClass.java
        targetClass.set("JaktLexer.java")

        skeleton

        // if set, plugin will remove a lexer output file before generating new one. Default: false
        purgeOldFiles.set(true)
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }
}

grammarKit {
    jflexRelease.set("1.7.0-1")
    grammarKitRelease.set("2021.1.2")
    intellijRelease.set("203.7717.81")
}