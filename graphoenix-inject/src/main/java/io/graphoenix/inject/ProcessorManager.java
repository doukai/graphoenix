package io.graphoenix.inject;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
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
import io.graphoenix.inject.error.InjectionErrorType;
import io.graphoenix.inject.error.InjectionProblem;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.inject.error.InjectionErrorType.CANNOT_GET_COMPILATION_UNIT;
import static io.graphoenix.inject.error.InjectionErrorType.CANNOT_PARSER_SOURCE_CODE;
import static io.graphoenix.inject.error.InjectionErrorType.PUBLIC_ANNOTATION_NOT_EXIST;
import static io.graphoenix.inject.error.InjectionErrorType.PUBLIC_CLASS_NOT_EXIST;

public class ProcessorManager {

    private final ProcessingEnvironment processingEnv;
    private final ClassLoader classLoader;
    private final Trees trees;
    private final Filer filer;
    private final Elements elements;
    private final JavaParser javaParser;
    private final JavaSymbolSolver javaSymbolSolver;
    private RoundEnvironment roundEnv;
    private final ClassFileToJavaSourceDecompiler decompiler;
    private final DecompilerLoader decompilerLoader;
    private final DecompilerPrinter decompilerPrinter;

    public ProcessorManager(ProcessingEnvironment processingEnv, ClassLoader classLoader) {
        this.processingEnv = processingEnv;
        this.classLoader = classLoader;
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
        this.trees = Trees.instance(processingEnv);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        Path generatedSourcePath = getGeneratedSourcePath();
        assert generatedSourcePath != null;
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(getSourcePath(generatedSourcePath));
        JavaParserTypeSolver generatedJavaParserTypeSolver = new JavaParserTypeSolver(generatedSourcePath);
        ClassLoaderTypeSolver classLoaderTypeSolver = new ClassLoaderTypeSolver(classLoader);
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(javaParserTypeSolver);
        combinedTypeSolver.add(generatedJavaParserTypeSolver);
        combinedTypeSolver.add(classLoaderTypeSolver);
        combinedTypeSolver.add(reflectionTypeSolver);
        this.javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        this.javaParser = new JavaParser();
        this.javaParser.getParserConfiguration().setSymbolResolver(javaSymbolSolver);
        this.decompiler = new ClassFileToJavaSourceDecompiler();
        this.decompilerLoader = new DecompilerLoader(classLoader);
        this.decompilerPrinter = new DecompilerPrinter();
    }

    public void setRoundEnv(RoundEnvironment roundEnv) {
        this.roundEnv = roundEnv;
    }

    private Path getGeneratedSourcePath() {
        try {
            FileObject tmp = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", UUID.randomUUID().toString());
            Writer writer = tmp.openWriter();
            writer.write("");
            writer.close();
            Path path = Paths.get(tmp.toUri());
            Files.deleteIfExists(path);
            Path generatedSourcePath = path.getParent();
            Logger.info("generated source path: {}", generatedSourcePath.toString());
            return generatedSourcePath;
        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "unable to determine generated source path.");
            return null;
        }
    }

    private Path getSourcePath(Path generatedSourcePath) {
        Path sourcePath = generatedSourcePath.getParent().getParent().getParent().getParent().getParent().getParent().resolve("src/main/java");
        Logger.info("source path: {}", sourcePath.toString());
        return sourcePath;
    }

    public String getRootPackageName() {
        String rootPackageName = roundEnv.getRootElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.PACKAGE))
                .map(element -> (PackageElement) element)
                .reduce((left, right) -> left.getQualifiedName().toString().length() < right.getQualifiedName().length() ? left : right)
                .map(packageElement -> packageElement.getQualifiedName().toString())
                .orElseGet(() ->
                        roundEnv.getRootElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.ENUM) ||
                                        element.getKind().equals(ElementKind.CLASS) ||
                                        element.getKind().equals(ElementKind.INTERFACE) ||
                                        element.getKind().equals(ElementKind.ANNOTATION_TYPE))
                                .map(elements::getPackageOf)
                                .reduce((left, right) -> left.getQualifiedName().toString().length() < right.getQualifiedName().length() ? left : right)
                                .map(packageElement -> packageElement.getQualifiedName().toString())
                                .orElseThrow(() -> new InjectionProblem(InjectionErrorType.ROOT_PACKAGE_NOT_EXIST))
                );
        Logger.info("root package: {}", rootPackageName);
        return rootPackageName;
    }

    public Optional<CompilationUnit> parse(TypeElement typeElement) {
        return parse(trees.getPath(typeElement).getCompilationUnit().toString());
    }

    private Optional<CompilationUnit> parse(String sourceCode) {
        return javaParser.parse(sourceCode).getResult();
    }

    public boolean classExists(String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void writeToFiler(CompilationUnit compilationUnit) {
        getPublicClassOrInterfaceDeclarationOptional(compilationUnit)
                .ifPresent(classOrInterfaceDeclaration -> {
                            try {
                                Writer writer = filer.createSourceFile(classOrInterfaceDeclaration.getFullyQualifiedName().orElseGet(classOrInterfaceDeclaration::getNameAsString)).openWriter();
                                writer.write(compilationUnit.toString());
                                writer.close();
                                Logger.info("{} build success", getQualifiedNameByDeclaration(classOrInterfaceDeclaration));
                            } catch (IOException e) {
                                Logger.error(e);
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "java file create failed");
                            }
                        }
                );
    }

    public void createResource(String name, String content) {
        try {
            Writer writer = filer.createResource(StandardLocation.CLASS_OUTPUT, "", name).openWriter();
            writer.write(content);
            writer.close();
            Logger.info("{} build success", name);
        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "resource file create failed");
        }
    }

    public Optional<FileObject> getResource(String fileName) {
        try {
            FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "", fileName);
            if (resource != null) {
                return Optional.of(resource);
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    public List<CompilationUnit> getCompilationUnitListWithAnnotationClass(Class<? extends Annotation> annotationClass) {
        return roundEnv.getElementsAnnotatedWith(annotationClass).stream()
                .map(element -> trees.getPath(element).getCompilationUnit().toString())
                .map(sourceCode -> getCompilationUnitBySourceCode(sourceCode).orElseThrow(() -> new InjectionProblem(CANNOT_PARSER_SOURCE_CODE.bind(sourceCode))))
                .collect(Collectors.toList());
    }

    public CompilationUnit getCompilationUnitBySourceCode(TypeElement typeElement) {
        return getCompilationUnitBySourceCodeOptional(typeElement).orElseThrow(() -> new InjectionProblem(InjectionErrorType.CANNOT_GET_COMPILATION_UNIT.bind(typeElement.getQualifiedName().toString())));
    }

    public Optional<CompilationUnit> getCompilationUnitBySourceCodeOptional(TypeElement typeElement) {
        return javaParser.parse(trees.getPath(typeElement).getCompilationUnit().toString()).getResult();
    }

    private Optional<CompilationUnit> getCompilationUnitBySourceCode(String sourceCode) {
        return javaParser.parse(sourceCode).getResult();
    }

    public CompilationUnit getCompilationUnitByQualifiedName(String qualifiedName) {
        return getCompilationUnitByQualifiedNameOptional(qualifiedName).orElseThrow(() -> new InjectionProblem(CANNOT_GET_COMPILATION_UNIT.bind(qualifiedName)));
    }

    public CompilationUnit getCompilationUnitByResolvedReferenceType(ResolvedReferenceType resolvedReferenceType) {
        return getCompilationUnitByResolvedReferenceTypeOptional(resolvedReferenceType).orElseThrow(() -> new InjectionProblem(CANNOT_GET_COMPILATION_UNIT.bind(resolvedReferenceType.getQualifiedName())));
    }

    public CompilationUnit getCompilationUnitByType(Type type) {
        return getCompilationUnitByTypeOptional(type).orElseThrow(() -> new InjectionProblem(CANNOT_GET_COMPILATION_UNIT.bind(getQualifiedNameByType(type))));
    }

    public CompilationUnit getCompilationUnitByClassOrInterfaceType(ClassOrInterfaceType type) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(type).orElseThrow(() -> new InjectionProblem(CANNOT_GET_COMPILATION_UNIT.bind(getQualifiedNameByType(type))));
    }

    public CompilationUnit getCompilationUnitByAnnotationExpr(AnnotationExpr annotationExpr) {
        return getCompilationUnitByAnnotationExprOptional(annotationExpr).orElseThrow(() -> new InjectionProblem(CANNOT_GET_COMPILATION_UNIT.bind(getQualifiedNameByAnnotationExpr(annotationExpr))));
    }

    public Optional<CompilationUnit> getCompilationUnitByQualifiedNameOptional(String qualifiedName) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(elements.getTypeElement(qualifiedName));
    }

    public Optional<CompilationUnit> getCompilationUnitByResolvedReferenceTypeOptional(ResolvedReferenceType resolvedReferenceType) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(elements.getTypeElement(resolvedReferenceType.getQualifiedName()));
    }

    public Optional<CompilationUnit> getCompilationUnitByTypeOptional(Type type) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(getElementByType(type));
    }

    public Optional<CompilationUnit> getCompilationUnitByClassOrInterfaceTypeOptional(ClassOrInterfaceType type) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(getElementByType(type));
    }

    public Optional<CompilationUnit> getCompilationUnitByAnnotationExprOptional(AnnotationExpr annotationExpr) {
        return getCompilationUnitByClassOrInterfaceTypeOptional(getElementByAnnotationExpr(annotationExpr));
    }

    private Optional<CompilationUnit> getCompilationUnitByClassOrInterfaceTypeOptional(TypeElement elementByType) {
        if (elementByType != null) {
            TreePath treePath = trees.getPath(elementByType);
            if (treePath != null) {
                return javaParser.parse(treePath.getCompilationUnit().toString()).getResult();
            } else {
                try {
                    decompiler.decompile(decompilerLoader, decompilerPrinter, elementByType.asType().toString());
                    String source = decompilerPrinter.toString();
                    return javaParser.parse(source).getResult();
                } catch (Exception e) {
                    Logger.warn(e);
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

    public String getQualifiedNameByType(Type type) {
        ResolvedReferenceType resolvedReferenceType = javaSymbolSolver.toResolvedType(type, ResolvedReferenceType.class);
        return resolvedReferenceType.getQualifiedName();
    }

    public String getQualifiedNameByType(ClassOrInterfaceType type) {
        ResolvedReferenceType resolvedReferenceType = javaSymbolSolver.toResolvedType(type, ResolvedReferenceType.class);
        return resolvedReferenceType.getQualifiedName();
    }

    private TypeElement getElementByType(Type type) {
        return elements.getTypeElement(getQualifiedNameByType(type));
    }

    private TypeElement getElementByType(ClassOrInterfaceType type) {
        return elements.getTypeElement(getQualifiedNameByType(type));
    }

    private TypeElement getElementByAnnotationExpr(AnnotationExpr annotationExpr) {
        return elements.getTypeElement(getQualifiedNameByAnnotationExpr(annotationExpr));
    }

    public String getTypeNameByExpression(Expression expression) {
        ResolvedType resolvedType = javaSymbolSolver.calculateType(expression);
        return resolvedType.toString();
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

        classOrInterfaceDeclaration.getMembers().stream()
                .flatMap(bodyDeclaration -> bodyDeclaration.findAll(ClassOrInterfaceType.class).stream())
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

        classOrInterfaceDeclaration.findAll(AnnotationExpr.class).stream()
                .filter(annotationExpr -> !tryResolve(annotationExpr))
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

    private boolean tryResolve(Node node) {
        try {
            javaSymbolSolver.resolveDeclaration(node, ResolvedAnnotationDeclaration.class);
            return true;
        } catch (Exception e) {
            return false;
        }
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

    public ClassOrInterfaceDeclaration getPublicClassOrInterfaceDeclaration(CompilationUnit compilationUnit) {
        return getPublicClassOrInterfaceDeclarationOptional(compilationUnit)
                .orElseThrow(() -> new InjectionProblem(PUBLIC_CLASS_NOT_EXIST.bind(compilationUnit.toString())));
    }

    public Optional<ClassOrInterfaceDeclaration> getPublicClassOrInterfaceDeclarationOptional(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst();
    }

    public AnnotationDeclaration getPublicAnnotationDeclaration(CompilationUnit compilationUnit) {
        return getPublicAnnotationDeclarationOptional(compilationUnit)
                .orElseThrow(() -> new InjectionProblem(PUBLIC_ANNOTATION_NOT_EXIST.bind(compilationUnit.toString())));
    }

    private Optional<AnnotationDeclaration> getPublicAnnotationDeclarationOptional(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .filter(BodyDeclaration::isAnnotationDeclaration)
                .map(BodyDeclaration::asAnnotationDeclaration)
                .findFirst();
    }
}
