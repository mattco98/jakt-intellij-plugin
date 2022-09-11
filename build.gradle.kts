
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    idea
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.9.0"
    id("org.jetbrains.grammarkit") version "2021.2.2"
    kotlin("plugin.serialization") version "1.7.10"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
    }

    repositories {
        mavenCentral()
    }

    intellij {
        pluginName.set(properties("pluginName"))
        version.set(properties("platformVersion"))
        plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
    }

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
                    freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xjvm-default=all")
                }
            }
        }

        runIde {
            enabled = false
        }

        sourceSets["main"].java.srcDirs("src/main/gen")

        test {
            testLogging {
                events = setOf(TestLogEvent.FAILED)
                showExceptions = true
                exceptionFormat = TestExceptionFormat.FULL
                showCauses = true
                showStackTraces = true
            }
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
}

project(":") {
    intellij {
        type.set("IU")
        plugins.set(emptyList())
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.4.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
        implementation("net.java.dev.jna:jna:5.12.1")
        testImplementation("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    }

    tasks {
        generateLexer {
            // source flex file
            source.set("src/main/resources/grammar/Jakt.flex")

            // target directory for lexer
            targetDir.set("src/main/gen/org/serenityos/jakt/lexer/")

            // target classname, target file will be targetDir/targetClass.java
            targetClass.set("JaktLexer.java")

            // if set, plugin will remove a lexer output file before generating new one. Default: false
            purgeOldFiles.set(true)
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

        compileKotlin {
            dependsOn(generateParser, generateLexer)
        }
    }
}

project(":clion") {
    intellij {
        type.set("CL")
        plugins.set(listOf("com.intellij.cidr.base", "com.intellij.clion"))
    }

    dependencies {
        implementation(project(":"))
    }
}

grammarKit {
    jflexRelease.set("1.7.0-1")
    grammarKitRelease.set("2021.1.2")
    intellijRelease.set("203.7717.81")
}
