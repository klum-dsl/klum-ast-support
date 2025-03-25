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
//file:noinspection GrPackage
package com.blackbuild.klum.astscheduler

class AstSchedulerTest extends AbstractASTSpec {

    @Override
    def cleanup() {
        AstScheduler.removeInstance()
    }

    def "transformations are correctly ordered"() {
        given:
        createClass '''
package annos

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import com.blackbuild.klum.astscheduler.*

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass("annos.Ast1")
@interface Anno1 {}

@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass("annos.Ast2")
@interface Anno2 {}

@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass("annos.Ast3")
@interface Anno3 {}

class AstCollector {
    static List<String> collected = []
}

abstract class TestAst extends AbstractSchedulableAstTransformation {
    @Override
    void deferredVisit(ASTNode[] nodes, SourceUnit source){
        AstCollector.collected << ((AnnotationNode) nodes[0]).classNode.nameWithoutPackage + ":" + order
    }
    @Override
    void visit(ASTNode[] nodes, SourceUnit source){
        AstScheduler.deferVisit(this, nodes, source);
    }
}

@GroovyASTTransformation
class Ast1 extends TestAst {
    int order = 1
}
@GroovyASTTransformation
class Ast2 extends TestAst {
    int order = 2
}
@GroovyASTTransformation
class Ast3 extends TestAst {
    int order = 3
}
'''
        when:
        createClass '''
package impl

@annos.Anno2
@annos.Anno3
@annos.Anno1
class MyClass {
}

'''
        then:
        AstCollector.collected == [
                "Anno1:1",
                "Anno2:2",
                "Anno3:3"
        ]
    }


}
