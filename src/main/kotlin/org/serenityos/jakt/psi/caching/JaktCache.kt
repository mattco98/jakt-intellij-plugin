package org.serenityos.jakt.psi.caching

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

// This is largely taken from the intellij-rust plugin
// TODO: Allow same PsiElement to have multiple cache entries, so we
//      don't need a separate type/resolve cache
abstract class JaktCache(project: Project) : Disposable {
    private val cacheKey = Key.create<CachedValue<ConcurrentMap<PsiElement, Any>>>("CACHE_KEY")
    private val globalCache = AtomicReference(makeWeakMap())
    private val psiManager = project.service<JaktPsiManager>()

    init {
        @Suppress("LeakingThis")
        project.messageBus.connect(this).subscribe(JAKT_STRUCTURE_CHANGE_TOPIC, object : JaktStructureChangeListener {
            override fun onStructureChanged() {
                globalCache.set(makeWeakMap())
            }
        })
    }

    override fun dispose() {}

    @Suppress("UNCHECKED_CAST")
    fun <K : PsiElement, V : Any> resolveWithCaching(key: K, resolver: (K) -> V): V {
        ProgressManager.checkCanceled()
        val map = getCacheFor(key)
        return map.getOrPut(key) { resolver(key) } as V
    }

    private fun getCacheFor(element: PsiElement): ConcurrentMap<PsiElement, Any> {
        val owner = element.modificationBoundary

        return if (owner != null) {
            CachedValuesManager.getCachedValue(owner, cacheKey) {
                CachedValueProvider.Result.create(makeWeakMap(), owner.tracker, psiManager.globalModificationTracker)
            }
        } else globalCache.get()
    }

    private fun makeWeakMap() = ContainerUtil.createConcurrentWeakKeySoftValueMap<PsiElement, Any>()

}

class JaktResolveCache(project: Project) : JaktCache(project)
class JaktTypeCache(project: Project) : JaktCache(project)

fun PsiElement.resolveCache() = project.service<JaktResolveCache>()
fun PsiElement.typeCache() = project.service<JaktTypeCache>()
