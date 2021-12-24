package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface DaggerProxyProcessor {

    void init(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType);

    void buildComponentProxy(CompilationUnit moduleCompilationUnit,
                             ClassOrInterfaceDeclaration moduleClassDeclaration,
                             CompilationUnit componentCompilationUnit,
                             ClassOrInterfaceDeclaration componentClassDeclaration,
                             CompilationUnit componentProxyCompilationUnit,
                             ClassOrInterfaceDeclaration componentProxyClassDeclaration);


    Optional<CompilationUnit> createComponentProxy(BodyDeclaration<?> moduleBodyDeclaration,
                                                   CompilationUnit moduleCompilationUnit,
                                                   ClassOrInterfaceDeclaration moduleClassDeclaration);

    void prepareModuleProxy(BodyDeclaration<?> moduleBodyDeclaration,
                            CompilationUnit moduleCompilationUnit,
                            ClassOrInterfaceDeclaration moduleClassDeclaration,
                            List<CompilationUnit> componentProxyCompilationUnits,
                            CompilationUnit moduleProxyCompilationUnit,
                            ClassOrInterfaceDeclaration moduleProxyClassDeclaration);

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
