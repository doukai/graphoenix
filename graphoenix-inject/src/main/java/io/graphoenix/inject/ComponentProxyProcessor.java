package io.graphoenix.inject;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

public interface ComponentProxyProcessor {

    void init(ProcessorManager processorManager);

    default void inProcess() {

    }

    default void processComponentProxy(CompilationUnit componentCompilationUnit,
                                       ClassOrInterfaceDeclaration componentClassDeclaration,
                                       CompilationUnit componentProxyCompilationUnit,
                                       ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

    }

    default void processComponentModule(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

    }

    default void processModule(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

    }

    default void processModuleContext(CompilationUnit moduleCompilationUnit, BlockStmt moduleContextStaticInitializer) {

    }
}
