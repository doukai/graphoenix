package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public interface ComponentProxyProcessor {

    void init(ProcessorManager processorManager);

    void inProcess();

    void processComponentProxy(CompilationUnit componentCompilationUnit,
                               ClassOrInterfaceDeclaration componentClassDeclaration,
                               CompilationUnit componentProxyCompilationUnit,
                               ClassOrInterfaceDeclaration componentProxyClassDeclaration);
}
