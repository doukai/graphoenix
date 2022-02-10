package io.graphoenix.inject;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessorManager {

    private final ProcessingEnvironment processingEnv;
    private final Trees trees;
    private final Filer filer;
    private final Elements elements;
    private final JavaParser javaParser;
    private final JavaSymbolSolver javaSymbolSolver;
    private RoundEnvironment roundEnv;

    public ProcessorManager(ProcessingEnvironment processingEnv, ClassLoader classLoader) {
        this.processingEnv = processingEnv;
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
        this.trees = Trees.instance(processingEnv);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(getSourcePath());
        JavaParserTypeSolver generatedJavaParserTypeSolver = new JavaParserTypeSolver(getGeneratedSourcePath());
        ClassLoaderTypeSolver classLoaderTypeSolver = new ClassLoaderTypeSolver(classLoader);
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(javaParserTypeSolver);
        combinedTypeSolver.add(generatedJavaParserTypeSolver);
        combinedTypeSolver.add(classLoaderTypeSolver);
        combinedTypeSolver.add(reflectionTypeSolver);
        this.javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        this.javaParser = new JavaParser();
        this.javaParser.getParserConfiguration().setSymbolResolver(javaSymbolSolver);
    }

    public void setRoundEnv(RoundEnvironment roundEnv) {
        this.roundEnv = roundEnv;
    }

    private Path getGeneratedSourcePath() {
        try {
            FileObject tmp = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "tmp");
            Writer writer = tmp.openWriter();
            writer.write("");
            writer.close();
            return Paths.get(tmp.toUri()).getParent();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to determine source file path.");
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Path getSourcePath() {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector diagnostics = new DiagnosticCollector();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            FileObject tmp = fileManager.getFileForOutput(StandardLocation.SOURCE_OUTPUT, "", "tmp", null);
            return Paths.get(tmp.toUri()).getParent().resolve("src/main/java");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to determine source file path.");
        }
        return null;
    }

    public Optional<CompilationUnit> parse(TypeElement typeElement) {
        return parse(trees.getPath(typeElement).getCompilationUnit().toString());
    }

    private Optional<CompilationUnit> parse(String sourceCode) {
        return javaParser.parse(sourceCode).getResult();
    }

    public void writeToFiler(CompilationUnit compilationUnit) {
        getPublicClassOrInterfaceDeclaration(compilationUnit)
                .ifPresent(classOrInterfaceDeclaration -> {
                            try {
                                Writer writer = filer.createSourceFile(classOrInterfaceDeclaration.getFullyQualifiedName().orElseGet(classOrInterfaceDeclaration::getNameAsString)).openWriter();
                                writer.write(compilationUnit.toString());
                                writer.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    public void createResource(String name, String content) {
        try {
            Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "", name).openWriter();
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<FileObject> getResource(String fileName) {
        try {
            FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "", fileName);
            if (resource != null) {
                return Optional.of(resource);
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public List<CompilationUnit> getCompilationUnitListWithAnnotationClass(Class<? extends Annotation> annotationClass) {
        return roundEnv.getElementsAnnotatedWith(annotationClass).stream()
                .map(element -> trees.getPath(element).getCompilationUnit().toString())
                .map(sourceCode -> getCompilationUnitBySourceCode(sourceCode).orElseThrow())
                .collect(Collectors.toList());
    }

    public Optional<CompilationUnit> getCompilationUnitBySourceCode(TypeElement typeElement) {
        return javaParser.parse(trees.getPath(typeElement).getCompilationUnit().toString()).getResult();
    }

    private Optional<CompilationUnit> getCompilationUnitBySourceCode(String sourceCode) {
        return javaParser.parse(sourceCode).getResult();
    }

    public Optional<CompilationUnit> getCompilationUnitByQualifiedName(String qualifiedName) {
        return getCompilationUnitByClassOrInterfaceType(elements.getTypeElement(qualifiedName));
    }

    public Optional<CompilationUnit> getCompilationUnitByResolvedReferenceType(ResolvedReferenceType resolvedReferenceType) {
        return getCompilationUnitByClassOrInterfaceType(elements.getTypeElement(resolvedReferenceType.getQualifiedName()));
    }

    public Optional<CompilationUnit> getCompilationUnitByClassOrInterfaceType(ClassOrInterfaceType type) {
        return getCompilationUnitByClassOrInterfaceType(getElementByType(type));
    }

    public Optional<CompilationUnit> getCompilationUnitByAnnotationExpr(AnnotationExpr annotationExpr) {
        return getCompilationUnitByClassOrInterfaceType(getElementByAnnotationExpr(annotationExpr));
    }

    private Optional<CompilationUnit> getCompilationUnitByClassOrInterfaceType(TypeElement elementByType) {
        if (elementByType != null) {
            TreePath treePath = trees.getPath(elementByType);
            if (treePath != null) {
                return javaParser.parse(treePath.getCompilationUnit().toString()).getResult();
            } else {
                ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
                DecompilerLoader decompilerLoader = new DecompilerLoader();
                DecompilerPrinter decompilerPrinter = new DecompilerPrinter();
                try {
                    decompiler.decompile(decompilerLoader, decompilerPrinter, elementByType.asType().toString());
                    String source = decompilerPrinter.toString();
                    return javaParser.parse(source).getResult();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }

    public String getQualifiedNameByAnnotationExpr(AnnotationExpr annotationExpr) {
        ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = javaSymbolSolver.resolveDeclaration(annotationExpr, ResolvedAnnotationDeclaration.class);
        return resolvedAnnotationDeclaration.getQualifiedName();
    }

    public String getNameByAnnotationExpr(AnnotationExpr annotationExpr) {
        ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = javaSymbolSolver.resolveDeclaration(annotationExpr, ResolvedAnnotationDeclaration.class);
        return resolvedAnnotationDeclaration.getName();
    }

    public String getQualifiedNameByType(ClassOrInterfaceType type) {
        ResolvedReferenceType resolvedReferenceType = javaSymbolSolver.toResolvedType(type, ResolvedReferenceType.class);
        return resolvedReferenceType.getQualifiedName();
    }

    private TypeElement getElementByType(ClassOrInterfaceType type) {
        return elements.getTypeElement(getQualifiedNameByType(type));
    }

    private TypeElement getElementByAnnotationExpr(AnnotationExpr annotationExpr) {
        return elements.getTypeElement(getQualifiedNameByAnnotationExpr(annotationExpr));
    }

    public String getQualifiedNameByDeclaration(ClassOrInterfaceDeclaration declaration) {
        ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = javaSymbolSolver.resolveDeclaration(declaration, ResolvedReferenceTypeDeclaration.class);
        return resolvedReferenceTypeDeclaration.getQualifiedName();
    }

    public String getNameByDeclaration(ClassOrInterfaceDeclaration declaration) {
        ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = javaSymbolSolver.resolveDeclaration(declaration, ResolvedReferenceTypeDeclaration.class);
        return resolvedReferenceTypeDeclaration.getName();
    }

    private TypeElement getElementByDeclaration(ClassOrInterfaceDeclaration declaration) {
        return elements.getTypeElement(getQualifiedNameByDeclaration(declaration));
    }

    public void importAllClassOrInterfaceType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ClassOrInterfaceDeclaration sourceClassOrInterfaceDeclaration) {
        classOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class)
                .forEach(classOrInterfaceType -> {
                            if (sourceClassOrInterfaceDeclaration.getNameAsString().equals(classOrInterfaceType.getNameAsString())) {
                                classOrInterfaceDeclaration.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(getQualifiedNameByDeclaration(sourceClassOrInterfaceDeclaration)));
                            }
                            sourceClassOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class).stream()
                                    .filter(sourceClassOrInterfaceType -> sourceClassOrInterfaceType.getNameAsString().equals(classOrInterfaceType.getNameAsString()))
                                    .findFirst()
                                    .ifPresent(sourceClassOrInterfaceType ->
                                            classOrInterfaceDeclaration.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(getQualifiedNameByType(sourceClassOrInterfaceType)))
                                    );
                        }
                );

        classOrInterfaceDeclaration.findAll(AnnotationExpr.class)
                .forEach(annotationExpr -> {
                            if (sourceClassOrInterfaceDeclaration.getNameAsString().equals(annotationExpr.getNameAsString())) {
                                classOrInterfaceDeclaration.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(getQualifiedNameByDeclaration(sourceClassOrInterfaceDeclaration)));
                            }
                            sourceClassOrInterfaceDeclaration.findAll(AnnotationExpr.class).stream()
                                    .filter(sourceAnnotationExpr -> sourceAnnotationExpr.getNameAsString().equals(annotationExpr.getNameAsString()))
                                    .findFirst()
                                    .ifPresent(sourceAnnotationExpr ->
                                            classOrInterfaceDeclaration.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(getQualifiedNameByAnnotationExpr(sourceAnnotationExpr)))
                                    );
                        }
                );
    }

    public Stream<ResolvedType> getMethodReturnType(MethodDeclaration methodDeclaration) {
        return methodDeclaration.findAll(ReturnStmt.class).stream()
                .map(ReturnStmt::getExpression)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(javaSymbolSolver::calculateType);
    }

    public Stream<ResolvedReferenceType> getMethodReturnReferenceType(MethodDeclaration methodDeclaration) {
        return getMethodReturnType(methodDeclaration)
                .filter(ResolvedType::isReferenceType)
                .map(ResolvedType::asReferenceType);
    }

    public Optional<ClassOrInterfaceDeclaration> getPublicClassOrInterfaceDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst();
    }

    public Optional<AnnotationDeclaration> getPublicAnnotationDeclaration(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isAnnotationDeclaration)
                .map(BodyDeclaration::asAnnotationDeclaration)
                .findFirst();
    }
}
