package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import javax.tools.FileObject;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProcessorTools {

    private BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource;
    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<String>> getTypeNameByClassOrInterfaceType;
    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;
    private BiFunction<CompilationUnit, String, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceTypeName;
    private Consumer<CompilationUnit> writeToFiler;
    private Function<String, Optional<FileObject>> getResource;

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

    public BiFunction<CompilationUnit, String, Optional<CompilationUnit>> getGetCompilationUnitByClassOrInterfaceTypeName() {
        return getCompilationUnitByClassOrInterfaceTypeName;
    }

    public ProcessorTools setGetCompilationUnitByClassOrInterfaceTypeName(BiFunction<CompilationUnit, String, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceTypeName) {
        this.getCompilationUnitByClassOrInterfaceTypeName = getCompilationUnitByClassOrInterfaceTypeName;
        return this;
    }

    public Consumer<CompilationUnit> getWriteToFiler() {
        return writeToFiler;
    }

    public ProcessorTools setWriteToFiler(Consumer<CompilationUnit> writeToFiler) {
        this.writeToFiler = writeToFiler;
        return this;
    }

    public Function<String, Optional<FileObject>> getGetResource() {
        return getResource;
    }

    public ProcessorTools setGetResource(Function<String, Optional<FileObject>> getResource) {
        this.getResource = getResource;
        return this;
    }
}
