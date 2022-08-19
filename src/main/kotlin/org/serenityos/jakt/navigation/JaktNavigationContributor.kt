package org.serenityos.jakt.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.serenityos.jakt.index.JaktNamedElementIndex
import org.serenityos.jakt.index.JaktStructElementIndex
import org.serenityos.jakt.psi.named.JaktNamedElement

abstract class JaktNavigationContributor(
    private val key: StubIndexKey<String, JaktNamedElement>
) : ChooseByNameContributorEx, GotoClassContributor {
    override fun getElementKind() = "Structs"

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        StubIndex.getInstance().processAllKeys(key, processor, scope, null)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        StubIndex.getInstance().processElements(
            key,
            name,
            parameters.project,
            null,
            JaktNamedElement::class.java,
        ) { processor.process(it) }
    }

    override fun getQualifiedName(item: NavigationItem?) = (item as? JaktNamedElement)?.name

    override fun getQualifiedNameSeparator() = "::"
}

class JaktClassNavigationContributor : JaktNavigationContributor(JaktStructElementIndex.KEY)
class JaktSymbolNavigationContributor : JaktNavigationContributor(JaktNamedElementIndex.KEY)
