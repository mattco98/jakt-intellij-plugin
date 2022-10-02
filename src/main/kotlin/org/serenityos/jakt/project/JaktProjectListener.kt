package org.serenityos.jakt.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.serenityos.jakt.comptime.StringValue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class JaktProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        JaktProjectServiceImpl.copyPreludeFile(project)
        JaktUpdateNotification.showIfNecessary(project)

        tryDetermineSystemTargetTriple()
    }

    private fun tryDetermineSystemTargetTriple() = thread {
        if (targetTriple.get() != null)
            return@thread

        // First, try to get it from an installed compiler
        val commands = setOf(
            "clang++ -print-target-tuple",
            "clang++14 -print-target-tuple",
            "gcc -dumpmachine",
            "g++ -dumpmachine",
            "cc -dumpmachine",
            "c++ -dumpmachine",
        )

        for (command in commands) {
            try {
                val process = Runtime.getRuntime().exec(command)
                process.waitFor()
                if (process.exitValue() != 0)
                    continue
                val text = process.inputStream.reader().readText().trim()
                if ("-" in text) {
                    targetTriple.set(StringValue(text))
                    return@thread
                }
            } catch (e: Throwable) {
                // ignore
            }
        }

        // If that doesn't work, then try to pick an appropriate triple from the
        // environment information. This tries to model what Jakt does
        val name = System.getProperty("os.name")?.lowercase()

        if (name == null) {
            // Give up
            targetTriple.set(StringValue("unknown-unknown-unknown-unknown"))
            return@thread
        }

        val is64Bit =
            System.getProperty("os.arch")?.contains("64") == true ||
            System.getProperty("java.vm.name")?.contains("64") == true ||
            System.getProperty("sun.management.compiler")?.contains("64") == true ||
            System.getProperty("sun.arch.data.model")?.contains("64") == true

        val tripleGuess = when {
            "win" in name -> if (is64Bit) "i686-pc-windows-msvc" else "x86_64-pc-windows-msvc"
            "linux" in name -> "x86_64-pc-linux-gnu"
            "bsd" in name -> "x86_64-pc-bsd-unknown"
            "mac" in name || "darwin" in name -> "x86_64-apple-darwin-unknown"
            "unix" in name -> "x86_64-pc-unix-unknown"
            else -> "unknown-unknown-unknown-unknown"
        }

        targetTriple.set(StringValue(tripleGuess))
    }

    companion object {
        // The system triple (ex: "x64_64-pc-linux-gnu"). This is used in comptime
        // execution to populate the ___jakt_get_target_triple_string function
        val targetTriple = AtomicReference<StringValue>()
    }
}
