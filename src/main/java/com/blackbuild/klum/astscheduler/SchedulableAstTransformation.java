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
package com.blackbuild.klum.astscheduler;

import groovy.transform.CompilationUnitAware;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;

public interface SchedulableAstTransformation extends ASTTransformation, CompilationUnitAware, Comparable<SchedulableAstTransformation> {

    CompilationUnit getCompilationUnit();

    int getOrder();

    @Override
    default void visit(ASTNode[] nodes, SourceUnit source) {
        AstScheduler.deferVisit(this, nodes, source);
    }

    /**
     * The deferred invocation of the transformation. This should contain the logic usually found in {@link #visit(ASTNode[], SourceUnit)}.
     *
     * @param nodes The AstNodes when the call was triggered. Element 0 is the AnnotationNode that triggered this
     *      annotation to be activated. Element 1 is the AnnotatedNode decorated, such as a MethodNode or ClassNode. For
     *      global transformations it is usually safe to ignore this parameter.
     * @param source The source unit being compiled. The source unit may contain several classes. For global transformations,
     *      information about the AST can be retrieved from this object.
     * @see ASTTransformation#visit(ASTNode[], SourceUnit)
     */
    void deferredVisit(ASTNode[] nodes, SourceUnit source);

    default int compareTo(SchedulableAstTransformation o) {
        return Integer.compare(getOrder(), o.getOrder());
    }
}
