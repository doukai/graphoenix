package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.graphoenix.dagger.JavaParserUtil.JAVA_PARSER_UTIL;

public class ProcessorManager {

    private final ProcessingEnvironment processingEnv;
    private final Trees trees;
    private final Filer filer;
    private final Elements elements;
    private final JavaParser javaParser;
    private final CombinedTypeSolver combinedTypeSolver;
    private RoundEnvironment roundEnv;

    public ProcessorManager(ProcessingEnvironment processingEnv, ClassLoader classLoader) {
        this.processingEnv = processingEnv;
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
        this.trees = Trees.instance(processingEnv);
        combinedTypeSolver = new CombinedTypeSolver();
        JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(getSourcePath());
        ClassLoaderTypeSolver classLoaderTypeSolver = new ClassLoaderTypeSolver(classLoader);
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver.add(javaParserTypeSolver);
        combinedTypeSolver.add(classLoaderTypeSolver);
        JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        this.javaParser = new JavaParser();
        this.javaParser.getParserConfiguration().setSymbolResolver(javaSymbolSolver);
    }

    public void setRoundEnv(RoundEnvironment roundEnv) {
        this.roundEnv = roundEnv;
    }

    private Path getSourcePath() {
        try {
            Filer filer = processingEnv.getFiler();
            FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "tmp", (Element[]) null);
            Path projectPath = Paths.get(resource.toUri()).getParent().getParent();
            resource.delete();
            return projectPath.getParent().getParent().getParent().resolve("src/main/java");
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
        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(compilationUnit)
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
        return getCompilationUnitByClassOrInterfaceType(getElementByType(annotationExpr));
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

//    public Optional<CompilationUnit> getCompilationUnitByName(String name) {
//        TreePath treePath = trees.getPath(elements.getTypeElement(name));
//        if (treePath != null) {
//            return javaParser.parse(treePath.getCompilationUnit().toString()).getResult();
//        } else {
//            ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
//            DecompilerLoader decompilerLoader = new DecompilerLoader();
//            DecompilerPrinter decompilerPrinter = new DecompilerPrinter();
//            try {
//                decompiler.decompile(decompilerLoader, decompilerPrinter, name);
//                String source = decompilerPrinter.toString();
//                return javaParser.parse(source).getResult();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return Optional.empty();
//    }

//    public String getNameByType(CompilationUnit compilationUnit, String name) {
//        return compilationUnit.getImports().stream()
//                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name))
//                .map(NodeWithName::getNameAsString)
//                .findFirst()
//                .orElseGet(() -> compilationUnit.getImports().stream()
//                        .filter(ImportDeclaration::isAsterisk)
//                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name)))
//                        .map(importDeclaration -> importDeclaration.getNameAsString().concat(".").concat(name))
//                        .findFirst()
//                        .orElseGet(() -> classExist("java.lang.".concat(name)) ?
//                                "java.lang.".concat(name) :
//                                compilationUnit.getPackageDeclaration()
//                                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name))
//                                        .orElse(name)
//                        )
//                );
//    }

    public String getNameByType(AnnotationExpr annotationExpr) {
        return annotationExpr.resolve().getQualifiedName();
    }

    public String getNameByType(ClassOrInterfaceType type) {
        return type.resolve().getQualifiedName();
    }

    private TypeElement getElementByType(ClassOrInterfaceType type) {
        return elements.getTypeElement(type.resolve().getQualifiedName());
    }

    private TypeElement getElementByType(AnnotationExpr annotationExpr) {
        return elements.getTypeElement(annotationExpr.resolve().getQualifiedName());
    }

    public String getNameByDeclaration(ClassOrInterfaceDeclaration declaration) {
        return declaration.resolve().getQualifiedName();
    }

    private TypeElement getElementByDeclaration(ClassOrInterfaceDeclaration declaration) {
        return elements.getTypeElement(declaration.resolve().getQualifiedName());
    }

    public boolean classExist(String className) {
        return elements.getTypeElement(className) != null;
    }

//    public void importAllTypesFromSource(CompilationUnit target, CompilationUnit source) {
//        JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(target).ifPresent(classOrInterfaceDeclaration -> importAllTypesFromSource(classOrInterfaceDeclaration, target, source));
//    }


    public void importAllClassOrInterfaceType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ClassOrInterfaceDeclaration sourceClassOrInterfaceDeclaration) {

        classOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class)
                .forEach(classOrInterfaceType -> {
//                            ResolvedType resolvedType = JavaParserFacade.get(combinedTypeSolver).convertToUsage(classOrInterfaceType);
                            sourceClassOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class).stream()
                                    .filter(sourceClassOrInterfaceType -> sourceClassOrInterfaceType.getNameAsString().equals(classOrInterfaceType.getNameAsString()))
                                    .findFirst()
                                    .ifPresent(sourceClassOrInterfaceType ->
                                            classOrInterfaceDeclaration.findCompilationUnit()
                                                    .ifPresent(compilationUnit -> compilationUnit.addImport(sourceClassOrInterfaceType.resolve().getQualifiedName()))
                                    );
                        }
                );
    }

//    public void importAllTypesFromSource(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, CompilationUnit target, CompilationUnit source) {
//
//        classOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class)
//                .forEach(classOrInterfaceType ->
//                        classOrInterfaceType.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(classOrInterfaceType.resolve().getQualifiedName()))
//                );
//
//        classOrInterfaceDeclaration.findAll(ClassOrInterfaceType.class)
//                .forEach(classOrInterfaceType ->
//                        classOrInterfaceType.findCompilationUnit().ifPresent(compilationUnit -> compilationUnit.addImport(classOrInterfaceType.resolve().getQualifiedName()))
//                );
//
//        NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();
//
//        addImport(classOrInterfaceDeclaration.getAnnotations(), target, source);
//
//        classOrInterfaceDeclaration.getImplementedTypes()
//                .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));
//
//        classOrInterfaceDeclaration.getExtendedTypes()
//                .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));
//
//        members.stream()
//                .filter(BodyDeclaration::isFieldDeclaration)
//                .map(BodyDeclaration::asFieldDeclaration)
//                .forEach(fieldDeclaration -> {
//                            addImport(fieldDeclaration.getElementType(), target, source);
//                            addImport(fieldDeclaration.getAnnotations(), target, source);
//                        }
//                );
//
//        members.stream()
//                .filter(BodyDeclaration::isMethodDeclaration)
//                .map(BodyDeclaration::asMethodDeclaration)
//                .forEach(methodDeclaration -> {
//                            addImport(methodDeclaration.getType(), target, source);
//                            addImport(methodDeclaration.getAnnotations(), target, source);
//                            methodDeclaration.getParameters()
//                                    .forEach(parameter -> {
//                                                addImport(parameter.getType(), target, source);
//                                                addImport(parameter.getAnnotations(), target, source);
//                                            }
//                                    );
//                            methodDeclaration.getBody().ifPresent(blockStmt -> addImport(blockStmt, target, source));
//                        }
//                );
//
//        members.stream()
//                .filter(BodyDeclaration::isConstructorDeclaration)
//                .map(BodyDeclaration::asConstructorDeclaration)
//                .forEach(
//                        constructorDeclaration -> {
//                            addImport(constructorDeclaration.getAnnotations(), target, source);
//                            constructorDeclaration.getParameters()
//                                    .forEach(
//                                            parameter -> {
//                                                addImport(parameter.getType(), target, source);
//                                                addImport(parameter.getAnnotations(), target, source);
//                                            }
//                                    );
//                            addImport(constructorDeclaration.getBody(), target, source);
//                        }
//                );
//    }
//
//    private void addImport(BlockStmt blockStmt, CompilationUnit target, CompilationUnit source) {
//        blockStmt.getStatements().forEach(statement -> addImport(statement, target, source));
//    }
//
//    private void addImport(Statement statement, CompilationUnit target, CompilationUnit source) {
//        if (statement.isExpressionStmt()) {
//            addImport(statement.asExpressionStmt().getExpression(), target, source);
//        } else if (statement.isReturnStmt()) {
//            statement.asReturnStmt().getExpression().ifPresent(expression -> addImport(expression, target, source));
//        }
//    }
//
//    private void addImport(List<AnnotationExpr> annotations, CompilationUnit target, CompilationUnit source) {
//        for (AnnotationExpr annotation : annotations) {
//            addImport(annotation, target, source);
//        }
//    }
//
//    private void addImport(AnnotationExpr annotationExpr, CompilationUnit target, CompilationUnit source) {
//        addImport(annotationExpr.getName(), target, source);
//        if (annotationExpr.isNormalAnnotationExpr()) {
//            annotationExpr.asNormalAnnotationExpr().getPairs()
//                    .forEach(memberValuePair -> addImport(memberValuePair.getValue(), target, source));
//        } else if (annotationExpr.isSingleMemberAnnotationExpr()) {
//            addImport(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue(), target, source);
//        }
//    }
//
//    private void addImport(Expression expression, CompilationUnit target, CompilationUnit source) {
//        if (expression.isClassExpr()) {
//            addImport(expression.asClassExpr().getType(), target, source);
//        } else if (expression.isAnnotationExpr()) {
//            addImport(expression.asAnnotationExpr(), target, source);
//        } else if (expression.isNameExpr()) {
//            addImport(expression.asNameExpr().getName(), target, source);
//        } else if (expression.isObjectCreationExpr()) {
//            addImport(expression.asObjectCreationExpr().getType(), target, source);
//            expression.asObjectCreationExpr().getArguments().forEach(argExpr -> addImport(argExpr, target, source));
//            expression.asObjectCreationExpr().getTypeArguments().ifPresent(args -> args.forEach(arg -> addImport(arg.getElementType(), target, source)));
//            expression.asObjectCreationExpr().getScope().ifPresent(scopeExpr -> addImport(scopeExpr, target, source));
//        } else if (expression.isTypeExpr()) {
//            addImport(expression.asTypeExpr().getType(), target, source);
//        } else if (expression.isArrayInitializerExpr()) {
//            expression.asArrayInitializerExpr().getValues().forEach(itemExpr -> addImport(itemExpr, target, source));
//        } else if (expression.isVariableDeclarationExpr()) {
//            addImport(expression.asVariableDeclarationExpr().getAnnotations(), target, source);
//            expression.asVariableDeclarationExpr().getVariables()
//                    .forEach(variableDeclarator -> {
//                        addImport(variableDeclarator.getType(), target, source);
//                        variableDeclarator.getInitializer().ifPresent(initExpression -> addImport(initExpression, target, source));
//                    });
//        } else if (expression.isMethodCallExpr()) {
//            expression.asMethodCallExpr().getArguments().forEach(argExpr -> addImport(argExpr, target, source));
//            expression.asMethodCallExpr().getTypeArguments().ifPresent(args -> args.forEach(arg -> addImport(arg.getElementType(), target, source)));
//            expression.asMethodCallExpr().getScope().ifPresent(scopeExpr -> addImport(scopeExpr, target, source));
//        } else if (expression.isFieldAccessExpr()) {
//            addImport(expression.asFieldAccessExpr().getScope(), target, source);
//            expression.asFieldAccessExpr().getTypeArguments().ifPresent(args -> args.forEach(arg -> addImport(arg.getElementType(), target, source)));
//        }
//    }
//
//    private void addImport(Type type, CompilationUnit target, CompilationUnit source) {
//        if (type.isClassOrInterfaceType()) {
//            addImport(type.asClassOrInterfaceType().getName(), target, source);
//            type.asClassOrInterfaceType().getTypeArguments().ifPresent(types -> types.forEach(subType -> addImport(subType, target, source)));
//        } else if (type.isTypeParameter()) {
//            addImport(type.asTypeParameter().getElementType(), target, source);
//            type.asTypeParameter().getTypeBound()
//                    .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));
//        } else if (type.isArrayType()) {
//            addImport(type.asArrayType().getElementType(), target, source);
//            addImport(type.asArrayType().getComponentType(), target, source);
//        }
//    }
//
//    private void addImport(Name name, CompilationUnit target, CompilationUnit source) {
//        addImport(name.getIdentifier(), target, source);
//    }
//
//    private void addImport(SimpleName name, CompilationUnit target, CompilationUnit source) {
//        addImport(name.getIdentifier(), target, source);
//    }
//
//    private void addImport(String name, CompilationUnit target, CompilationUnit source) {
//
//
//        Optional<ImportDeclaration> sourceImport = source.getImports().stream()
//                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name))
//                .findFirst();
//
//        if (sourceImport.isPresent()) {
//            target.addImport(sourceImport.get());
//            return;
//        }
//
//        Optional<ImportDeclaration> classImportInAsterisk = source.getImports().stream()
//                .filter(ImportDeclaration::isAsterisk)
//                .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name)))
//                .findFirst();
//
//        if (classImportInAsterisk.isPresent()) {
//            target.addImport(classImportInAsterisk.get());
//            return;
//        }
//
//        Optional<String> sourceSamePackageClassName = source.getPackageDeclaration()
//                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name))
//                .filter(this::classExist);
//
//        if (sourceSamePackageClassName.isPresent()) {
//            target.addImport(sourceSamePackageClassName.get());
//            return;
//        }
//
//        Optional<String> sourceClassName = JAVA_PARSER_UTIL.getPublicClassOrInterfaceDeclaration(source)
//                .filter(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getNameAsString().equals(name))
//                .map(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getFullyQualifiedName().orElse(classOrInterfaceDeclaration.getNameAsString()));
//
//        sourceClassName.ifPresent(target::addImport);
//    }
}
