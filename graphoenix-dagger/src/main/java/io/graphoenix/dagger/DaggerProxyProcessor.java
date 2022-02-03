package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.lang.annotation.Annotation;
import java.util.List;

public interface DaggerProxyProcessor {

    void init(ProcessorManager processorManager);

    void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                             ClassOrInterfaceDeclaration moduleClassDeclaration,
                             CompilationUnit componentCompilationUnit,
                             ClassOrInterfaceDeclaration componentClassDeclaration,
                             CompilationUnit componentProxyCompilationUnit,
                             ClassOrInterfaceDeclaration componentProxyClassDeclaration);

    void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                          ClassOrInterfaceDeclaration moduleClassDeclaration,
                          List<CompilationUnit> componentProxyCompilationUnits,
                          CompilationUnit moduleProxyCompilationUnit,
                          ClassOrInterfaceDeclaration moduleProxyClassDeclaration);

    void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit,
                                      ClassOrInterfaceDeclaration moduleProxyClassDeclaration,
                                      CompilationUnit componentProxyCompilationUnit,
                                      ClassOrInterfaceDeclaration componentProxyClassDeclaration,
                                      CompilationUnit componentProxyComponentCompilationUnit,
                                      ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration);

    Class<? extends Annotation> support();
}
