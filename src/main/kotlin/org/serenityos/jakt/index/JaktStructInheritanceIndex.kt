package org.serenityos.jakt.index

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.serenityos.jakt.psi.api.JaktStructDeclaration
import org.serenityos.jakt.psi.named.JaktNameIdentifierOwner
import org.serenityos.jakt.stubs.JaktFileStub

class JaktStructInheritanceIndex : StringStubIndexExtension<JaktNameIdentifierOwner>() {
    override fun getVersion() = JaktFileStub.Type.stubVersion
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, JaktNameIdentifierOwner>("JaktStructInheritanceIndex")

        fun hasInheritors(element: JaktStructDeclaration): Boolean {
            val project = element.project
            val elementPath = element.toPath()
            var found = false

            // Small optimization: instead of calling getInheritors(element).isNotEmpty() and
            // potentially processing hundreds of inherited structs, we call processElements
            // and stop on the first iteration
            StubIndex.getInstance().processElements(
                KEY,
                elementPath.toString(),
                project,
                GlobalSearchScope.allScope(project),
                JaktNameIdentifierOwner::class.java,
            ) {
                found = true
                false
            }

            return found
        }

        fun getInheritors(element: JaktStructDeclaration): List<JaktStructDeclaration> {
            val project = element.project

            val directInheritors = StubIndex.getElements(
                KEY,
                element.toPath().toString(),
                project,
                GlobalSearchScope.allScope(project),
                JaktNameIdentifierOwner::class.java,
            ).filterIsInstance<JaktStructDeclaration>()

            return directInheritors + directInheritors.flatMap(::getInheritors)
        }
    }
}
