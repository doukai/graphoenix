package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.List;

public interface DaggerProxyProcessor {

    void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                             ClassOrInterfaceDeclaration moduleClassDeclaration,
                             CompilationUnit componentCompilationUnit,
                             ClassOrInterfaceDeclaration componentClassDeclaration,
                             CompilationUnit componentProxyCompilationUnit,
                             ClassOrInterfaceDeclaration componentProxyClassDeclaration);

    void buildModuleProxy(CompilationUnit moduleCompilationUnit,
                          ClassOrInterfaceDeclaration moduleCLassDeclaration,
                          List<CompilationUnit> componentProxyCompilationUnits,
                          CompilationUnit moduleProxyCompilationUnit,
                          ClassOrInterfaceDeclaration moduleProxyClassDeclaration);

    void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit,
                                      ClassOrInterfaceDeclaration moduleProxyClassDeclaration,
                                      CompilationUnit componentProxyCompilationUnit,
                                      ClassOrInterfaceDeclaration componentProxyClassDeclaration,
                                      CompilationUnit componentProxyComponentCompilationUnit,
                                      ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration);
}
