package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ProcessorTools {

    private BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource;
    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<String>> getTypeNameByClassOrInterfaceType;
    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;

    public BiConsumer<CompilationUnit, CompilationUnit> getImportAllTypesFromSource() {
        return importAllTypesFromSource;
    }

    public ProcessorTools setImportAllTypesFromSource(BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource) {
        this.importAllTypesFromSource = importAllTypesFromSource;
        return this;
    }

    public BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<String>> getGetTypeNameByClassOrInterfaceType() {
        return getTypeNameByClassOrInterfaceType;
    }

    public ProcessorTools setGetTypeNameByClassOrInterfaceType(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<String>> getTypeNameByClassOrInterfaceType) {
        this.getTypeNameByClassOrInterfaceType = getTypeNameByClassOrInterfaceType;
        return this;
    }

    public BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getGetCompilationUnitByClassOrInterfaceType() {
        return getCompilationUnitByClassOrInterfaceType;
    }

    public ProcessorTools setGetCompilationUnitByClassOrInterfaceType(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType) {
        this.getCompilationUnitByClassOrInterfaceType = getCompilationUnitByClassOrInterfaceType;
        return this;
    }
}
