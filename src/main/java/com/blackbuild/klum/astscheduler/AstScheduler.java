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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.EnumCompletionVisitor;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.classgen.InnerClassCompletionVisitor;
import org.codehaus.groovy.control.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.SortedSet;

public class AstScheduler {

    private static ThreadLocal<AstScheduler> INSTANCE = new ThreadLocal<>();

    public static AstScheduler getInstance() {
        if (INSTANCE.get() == null)
            INSTANCE.set(new AstScheduler());
        return INSTANCE.get();
    }

    public static void deferVisit(SchedulableAstTransformation transformation, ASTNode[] nodes, SourceUnit sourceUnit) {
        getInstance().defer(transformation, nodes, sourceUnit);
    }

    public static void removeInstance() {
        INSTANCE.remove();
    }

    private final SortedSet<Invocation> invocations = new java.util.TreeSet<>();
    private final EnumSet<CompilePhase> alreadyScheduled = EnumSet.noneOf(CompilePhase.class);
    private boolean finalizerScheduled = false;

    void defer(SchedulableAstTransformation transformation, ASTNode[] nodes, SourceUnit sourceUnit) {
        invocations.add(new Invocation(transformation, nodes, sourceUnit));
        CompilePhase currentPhase = CompilePhase.fromPhaseNumber(transformation.getCompilationUnit().getPhase());
        scheduleDeferredTransformationsInPhase(transformation, currentPhase);
        if (!finalizerScheduled) {
            transformation.getCompilationUnit().addPhaseOperation(new CompilationUnit.SourceUnitOperation() {
                @Override
                public void call(SourceUnit source) throws CompilationFailedException {
                    removeInstance();
                }
            }, Phases.FINALIZATION);
            finalizerScheduled = true;
        }
    }

    private void scheduleDeferredTransformationsInPhase(SchedulableAstTransformation transformation, CompilePhase currentPhase) {
        if (alreadyScheduled.contains(currentPhase)) return;
        alreadyScheduled.add(currentPhase);
        CompilationUnit compilationUnit = transformation.getCompilationUnit();
        compilationUnit.addNewPhaseOperation(new CompilationUnit.SourceUnitOperation() {
            @Override
            public void call(SourceUnit source) throws CompilationFailedException {
                execute();
            }
        }, currentPhase.getPhaseNumber());
        if (currentPhase == CompilePhase.SEMANTIC_ANALYSIS)
            addStaticVerifier(compilationUnit);
        if (currentPhase == CompilePhase.CANONICALIZATION)
            addCanoncalizationPhaseOperations(compilationUnit);
    }

    private void addCanoncalizationPhaseOperations(CompilationUnit compilationUnit) {
        compilationUnit.addNewPhaseOperation(new CompilationUnit.SourceUnitOperation() {
            @Override
            public void call(SourceUnit source) throws CompilationFailedException {
                compilationUnit.applyToPrimaryClassNodes(new CompilationUnit.PrimaryClassNodeOperation() {
                    @Override
                    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                        InnerClassCompletionVisitor iv = new InnerClassCompletionVisitor(compilationUnit, source);
                        iv.visitClass(classNode);
                    }
                });
            }
        }, Phases.CANONICALIZATION);
        compilationUnit.addNewPhaseOperation(new CompilationUnit.SourceUnitOperation() {
            @Override
            public void call(SourceUnit source) throws CompilationFailedException {
                compilationUnit.applyToPrimaryClassNodes(new CompilationUnit.PrimaryClassNodeOperation() {
                    @Override
                    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                        EnumCompletionVisitor ecv = new EnumCompletionVisitor(compilationUnit, source);
                        ecv.visitClass(classNode);
                    }
                });
            }
        }, Phases.CANONICALIZATION);
    }

    private void addStaticVerifier(CompilationUnit compilationUnit) {
        compilationUnit.addNewPhaseOperation(new CompilationUnit.SourceUnitOperation() {
            @Override
            public void call(SourceUnit source) throws CompilationFailedException {
                compilationUnit.applyToPrimaryClassNodes(new CompilationUnit.PrimaryClassNodeOperation() {
                    @Override
                    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
                        StaticVerifier sv = new StaticVerifier();
                        sv.visitClass(classNode, source);
                    }
                });
            }
        }, Phases.SEMANTIC_ANALYSIS);
    }

    void execute() {
        for (Invocation invocation : invocations)
            invocation.transformation.deferredVisit(invocation.nodes, invocation.sourceUnit);
        invocations.clear();
    }

    private static class Invocation implements Comparable<Invocation> {
        private final SchedulableAstTransformation transformation;
        private final ASTNode[] nodes;
        private final SourceUnit sourceUnit;

        private Invocation(SchedulableAstTransformation transformation, ASTNode[] nodes, SourceUnit sourceUnit) {
            this.transformation = transformation;
            this.nodes = nodes;
            this.sourceUnit = sourceUnit;
        }

        @Override
        public int compareTo(@NotNull AstScheduler.Invocation o) {
            return transformation.compareTo(o.transformation);
        }
    }
}
