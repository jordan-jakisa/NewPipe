package dev.jordanempire.youflow.res

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preference and layout XML load classes by name at runtime, so a deleted class only crashes on
 * a device. This catches that on the JVM.
 */
class ResourceClassReferencesTest {
    private val resDir = File("src/main/res")
    private val classTag = Regex("""<(dev\.jordanempire\.[\w.$]+)[\s/>]""")
    private val classAttr = Regex("""(?:android:fragment|android:name|class)="(dev\.jordanempire\.[\w.$]+)"""")

    private fun referencedClasses(): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        resDir.walkTopDown().filter { it.extension == "xml" }.forEach { file ->
            val text = file.readText()
            (classTag.findAll(text) + classAttr.findAll(text)).forEach {
                result.getOrPut(it.groupValues[1]) { mutableSetOf() }.add(file.name)
            }
        }
        return result
    }

    @Test
    fun everyClassNamedInResourcesExists() {
        val missing = referencedClasses().filterKeys { name ->
            runCatching { Class.forName(name) }.isFailure
        }
        assertTrue("Resources reference classes that do not exist: $missing", missing.isEmpty())
    }

    @Test
    fun manifestComponentsExist() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val names = Regex("""android:name="(\.[\w.$]+)"""").findAll(manifest).map { it.groupValues[1] }.toList()
        val missing = names.filter { runCatching { Class.forName("dev.jordanempire.youflow$it") }.isFailure }
        assertTrue("Manifest names classes that do not exist: $missing", missing.isEmpty())
    }
}
