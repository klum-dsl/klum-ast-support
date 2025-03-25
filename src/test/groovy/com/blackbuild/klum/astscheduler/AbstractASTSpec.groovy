/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.klum.astscheduler

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.runtime.InvokerHelper
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Specification

class AbstractASTSpec extends Specification {

    @Rule TestName testName = new TestName()
    @Rule TemporaryFolder tempFolder = new TemporaryFolder()
    ClassLoader oldLoader
    GroovyClassLoader loader
    def instance
    Class<?> clazz
    CompilerConfiguration compilerConfiguration
    Map<String, Class<?>> classPool = [:]

    def setup() {
        oldLoader = Thread.currentThread().contextClassLoader
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStarImports(
                "com.blackbuild.groovy.configdsl.transform"
        )

        compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.addCompilationCustomizers(importCustomizer)
        loader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfiguration)
        Thread.currentThread().contextClassLoader = loader
        def outputDirectory = new File("build/test-classes/$GroovySystem.version/${getClass().simpleName}/$safeFilename")
        outputDirectory.deleteDir()
        outputDirectory.mkdirs()
        compilerConfiguration.targetDirectory = outputDirectory
        compilerConfiguration.optimizationOptions.groovydoc = Boolean.TRUE
    }

    def getSafeFilename() {
        testName.methodName.replaceAll("\\W+", "_")
    }

    def cleanup() {
        Thread.currentThread().contextClassLoader = oldLoader
    }

    def propertyMissing(String name) {
        if (classPool.containsKey(name))
            return classPool[name]
        throw new MissingPropertyException(name, getClass())
    }

    def newInstanceOf(String className) {
        return getClass(className).getDeclaredConstructor().newInstance()
    }

    def newInstanceOf(String className, Object... args) {
        return InvokerHelper.invokeConstructorOf(getClass(className), args)
    }

    Class<?> createClass(@Language("groovy") String code) {
        this.clazz = parseClass(code)
        return this.clazz
    }

    private Class parseClass(String code) {
        def clazz = loader.parseClass(code)
        updateClassPool()
        return clazz
    }

    private Class parseClass(String code, String filename) {
        def clazz = loader.parseClass(code, filename)
        updateClassPool()
        return clazz
    }

    private void updateClassPool() {
        loader.loadedClasses.each {
            classPool[it.name.tokenize(".").last()] = it
        }
    }

    Class<?> getClass(String classname) {
        if (classPool.containsKey(classname)) {
            return classPool[classname]
        }
        return loader.loadClass(classname)
    }
}
