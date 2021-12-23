package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import dagger.Module;
import io.graphoenix.spi.aop.InterceptorBean;
import jakarta.annotation.Generated;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.dagger.DaggerProcessorUtil.DAGGER_PROCESSOR_UTIL;

@SupportedAnnotationTypes("dagger.Module")
@AutoService(Processor.class)
public class DaggerModuleProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;
    private Filer filer;
    private Elements elements;
    private ServiceLoader<DaggerProxyProcessor> daggerProxyProcessors;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
        this.filer = this.processingEnv.getFiler();
        this.elements = this.processingEnv.getElementUtils();
        this.daggerProxyProcessors = ServiceLoader.load(DaggerProxyProcessor.class, DaggerModuleProcessor.class.getClassLoader());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getAnnotation(Generated.class) == null) {
                    buildComponents(trees.getPath(bundleClassElement).getCompilationUnit().toString());
                }
            }
        }
        return false;
    }

    protected void buildComponents(String sourceCode) {
        javaParser.parse(sourceCode)
                .ifSuccessful(moduleCompilationUnit -> {
                            List<CompilationUnit> componentProxyCompilationUnits = moduleCompilationUnit.getTypes().stream()
                                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                    .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                    .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                    .flatMap(moduleClassDeclaration ->
                                            moduleClassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .filter(bodyDeclaration -> !bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                                                    .map(BodyDeclaration::toMethodDeclaration)
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                                    .map(DAGGER_PROCESSOR_UTIL::getMethodReturnType)
                                                    .flatMap(componentType ->
                                                            buildComponentProxy(
                                                                    moduleCompilationUnit,
                                                                    moduleClassDeclaration,
                                                                    getSourceByType(moduleCompilationUnit, componentType)
                                                            )
                                                    )
                                    )
                                    .collect(Collectors.toList());

                            componentProxyCompilationUnits.forEach(this::writeToFiler);

                            moduleCompilationUnit.getTypes().stream()
                                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                    .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                    .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                    .findFirst()
                                    .ifPresent(moduleClassDeclaration -> {
                                                CompilationUnit moduleProxyCompilationUnit = buildModuleProxy(componentProxyCompilationUnits, moduleCompilationUnit, moduleClassDeclaration);
                                                writeToFiler(moduleProxyCompilationUnit);
                                                componentProxyCompilationUnits.forEach(
                                                        componentProxyCompilationUnit -> {
                                                            componentProxyCompilationUnit.getTypes().stream()
                                                                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                                                    .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                                                    .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                                                    .findFirst()
                                                                    .ifPresent(componentProxyClassDeclaration -> {
                                                                        moduleProxyCompilationUnit.getTypes().stream()
                                                                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                                                                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                                                                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                                                                .findFirst()
                                                                                .ifPresent(moduleProxyClassDeclaration -> {
                                                                                    writeToFiler(buildComponentProxyComponent(componentProxyCompilationUnit, componentProxyClassDeclaration, moduleProxyCompilationUnit, moduleProxyClassDeclaration));
                                                                                });
                                                                    });
                                                        }
                                                );
                                            }
                                    );
                        }
                );
    }

    protected Stream<CompilationUnit> buildComponentProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, String componentSource) {
        return javaParser.parse(componentSource).getResult().stream()
                .flatMap(componentCompilationUnit ->
                        componentCompilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                .map(componentClassDeclaration -> {

                                            ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                                                    .addModifier(Modifier.Keyword.PUBLIC)
                                                    .addExtendedType(componentClassDeclaration.getNameAsString())
                                                    .setName(componentClassDeclaration.getNameAsString().concat("Proxy"));

                                            componentClassDeclaration.getConstructors().forEach(
                                                    constructorDeclaration -> {
                                                        ConstructorDeclaration componentProxyClassConstructor = componentProxyClassDeclaration
                                                                .addConstructor(Modifier.Keyword.PUBLIC)
                                                                .addAnnotation(Inject.class)
                                                                .setParameters(constructorDeclaration.getParameters());

                                                        componentProxyClassConstructor
                                                                .createBody()
                                                                .addStatement(
                                                                        new MethodCallExpr()
                                                                                .setName(new SuperExpr().toString())
                                                                                .setArguments(
                                                                                        new NodeList<>(
                                                                                                constructorDeclaration.getParameters().stream()
                                                                                                        .map(NodeWithSimpleName::getNameAsExpression)
                                                                                                        .collect(Collectors.toList())
                                                                                        )
                                                                                )
                                                                );
                                                    }
                                            );

                                            CompilationUnit componentProxyCompilationUnit = new CompilationUnit()
                                                    .addType(componentProxyClassDeclaration)
                                                    .addImport(Inject.class);

                                            componentCompilationUnit.getPackageDeclaration().ifPresent(componentProxyCompilationUnit::setPackageDeclaration);

                                            daggerProxyProcessors.forEach(daggerProxyProcessor -> {
                                                daggerProxyProcessor.buildComponentProxy(moduleCompilationUnit, moduleClassDeclaration, componentCompilationUnit, componentClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration);
                                            });

                                            importAllTypesFromSource(componentProxyClassDeclaration, componentProxyCompilationUnit, moduleCompilationUnit);
                                            importAllTypesFromSource(componentProxyClassDeclaration, componentProxyCompilationUnit, componentCompilationUnit);

                                            return componentProxyCompilationUnit;
                                        }
                                ));
    }

    protected CompilationUnit buildModuleProxy(List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        ClassOrInterfaceDeclaration moduleProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleClassDeclaration.getNameAsString() + "Proxy")
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr("io.graphoenix.dagger.DaggerModuleProcessor")).setName(Generated.class.getSimpleName()));

        componentProxyCompilationUnits
                .forEach(componentProxyCompilationUnit ->
                        componentProxyCompilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                .findFirst()
                                .ifPresent(componentProxyClassDeclaration -> {
                                            MethodDeclaration componentMethodDeclaration = moduleClassDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .filter(declaration -> declaration.getType().isClassOrInterfaceType())
                                                    .filter(declaration -> DAGGER_PROCESSOR_UTIL.getMethodReturnType(declaration).getNameAsString().equals(componentProxyClassDeclaration.getExtendedTypes(0).getNameAsString()))
                                                    .findFirst()
                                                    .orElseThrow();

                                            BlockStmt blockStmt = componentMethodDeclaration.getBody().orElseThrow().clone();

                                            moduleProxyClassDeclaration
                                                    .addMethod(componentMethodDeclaration.getNameAsString(), Modifier.Keyword.PUBLIC)
                                                    .setParameters(componentMethodDeclaration.getParameters())
                                                    .setType(componentMethodDeclaration.getType())
                                                    .setAnnotations(componentMethodDeclaration.getAnnotations())
                                                    .setBody(blockStmt);

                                            blockStmt.getStatements()
                                                    .forEach(statement -> {
                                                                if (statement.isReturnStmt()) {
                                                                    Expression expression = statement.asReturnStmt().getExpression().orElseThrow();
                                                                    if (expression.isObjectCreationExpr()) {
                                                                        ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                                        objectCreationExpr.setType(componentProxyClassDeclaration.getNameAsString());
                                                                    }
                                                                }
                                                            }
                                                    );
                                        }
                                )
                );

        CompilationUnit moduleProxyCompilationUnit = new CompilationUnit().addType(moduleProxyClassDeclaration)
                .addImport(Module.class)
                .addImport(Generated.class);

        NormalAnnotationExpr moduleAnnotationExpr = new NormalAnnotationExpr();
        moduleAnnotationExpr.setName(Module.class.getSimpleName());
        moduleClassDeclaration.getAnnotationByClass(Module.class).orElseThrow()
                .clone()
                .asNormalAnnotationExpr().getPairs()
                .forEach(memberValuePair -> {
                    if (memberValuePair.getValue().isClassExpr()) {
                        moduleProxyCompilationUnit.addImport(getNameByType(moduleCompilationUnit, memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType().getName()) + "Proxy");
                        memberValuePair.getValue().asClassExpr().setType(memberValuePair.getValue().asClassExpr().getTypeAsString() + "Proxy");
                    }
                    moduleAnnotationExpr.addPair(memberValuePair.getNameAsString(), memberValuePair.getValue());
                });
        moduleProxyClassDeclaration.addAnnotation(moduleAnnotationExpr);

        moduleCompilationUnit.getPackageDeclaration().ifPresent(moduleProxyCompilationUnit::setPackageDeclaration);

        daggerProxyProcessors.forEach(daggerProxyProcessor -> {
            daggerProxyProcessor.buildModuleProxy(moduleCompilationUnit, moduleClassDeclaration, componentProxyCompilationUnits, moduleProxyCompilationUnit, moduleProxyClassDeclaration);
        });

        componentProxyCompilationUnits.forEach(
                componentProxyCompilationUnit -> {
                    componentProxyCompilationUnit.getTypes().stream()
                            .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                            .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                            .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                            .findFirst()
                            .ifPresent(componentProxyClassDeclaration -> {
                                moduleProxyCompilationUnit.addImport(componentProxyCompilationUnit.getPackageDeclaration()
                                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(componentProxyClassDeclaration.getNameAsString()))
                                        .orElseGet(componentProxyClassDeclaration::getNameAsString));
                            });
                }
        );

        importAllTypesFromSource(moduleProxyClassDeclaration, moduleProxyCompilationUnit, moduleCompilationUnit);

        return moduleProxyCompilationUnit;
    }

    protected CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {
        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleProxyClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentProxyClassDeclaration.getNameAsString() + "Component")
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        componentProxyComponentInterfaceDeclaration.addMethod("get").setType(
                moduleProxyClassDeclaration.getMembers()
                        .stream().filter(BodyDeclaration::isMethodDeclaration)
                        .map(BodyDeclaration::asMethodDeclaration)
                        .filter(methodDeclaration -> DAGGER_PROCESSOR_UTIL.getMethodReturnType(methodDeclaration).getNameAsString().equals(componentProxyClassDeclaration.getNameAsString()))
                        .findFirst()
                        .orElseThrow()
                        .getType()
        ).removeBody();

        CompilationUnit componentProxyComponentCompilationUnit = new CompilationUnit()
                .addType(componentProxyComponentInterfaceDeclaration)
                .addImport(Singleton.class)
                .addImport(Component.class);

        componentProxyCompilationUnit.getPackageDeclaration().ifPresent(componentProxyComponentCompilationUnit::setPackageDeclaration);
        importAllTypesFromSource(componentProxyComponentInterfaceDeclaration, componentProxyComponentCompilationUnit, componentProxyCompilationUnit);
        componentProxyComponentCompilationUnit.addImport(
                moduleProxyCompilationUnit.getPackageDeclaration()
                        .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(moduleProxyClassDeclaration.getNameAsString()))
                        .orElseGet(moduleProxyClassDeclaration::getNameAsString)
        );

        daggerProxyProcessors.forEach(daggerProxyProcessor -> {
            daggerProxyProcessor.buildComponentProxyComponent(moduleProxyCompilationUnit, moduleProxyClassDeclaration, componentProxyCompilationUnit, componentProxyClassDeclaration, componentProxyComponentCompilationUnit, componentProxyComponentInterfaceDeclaration);
        });

        importAllTypesFromSource(componentProxyComponentInterfaceDeclaration, componentProxyComponentCompilationUnit, moduleProxyCompilationUnit);

        return componentProxyComponentCompilationUnit;
    }

    protected void writeToFiler(CompilationUnit compilationUnit) {
        compilationUnit.getTypes().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .filter(typeDeclaration -> typeDeclaration.hasModifier(Modifier.Keyword.PUBLIC))
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst()
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

    protected void importAllTypesFromSource(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, CompilationUnit target, CompilationUnit source) {
        NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

        addImport(classOrInterfaceDeclaration.getAnnotations(), target, source);

        classOrInterfaceDeclaration.getImplementedTypes()
                .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));

        classOrInterfaceDeclaration.getExtendedTypes()
                .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));

        members.stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .map(BodyDeclaration::asFieldDeclaration)
                .forEach(fieldDeclaration -> {
                            addImport(fieldDeclaration.getElementType(), target, source);
                            addImport(fieldDeclaration.getAnnotations(), target, source);
                        }
                );

        members.stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .map(BodyDeclaration::asMethodDeclaration)
                .forEach(
                        methodDeclaration -> {
                            addImport(methodDeclaration.getType(), target, source);
                            addImport(methodDeclaration.getAnnotations(), target, source);
                            methodDeclaration.getParameters()
                                    .forEach(
                                            parameter -> {
                                                addImport(parameter.getType(), target, source);
                                                addImport(parameter.getAnnotations(), target, source);
                                            }
                                    );
                            methodDeclaration.getBody().ifPresent(blockStmt -> addImport(blockStmt, target, source));
                        }
                );

        members.stream()
                .filter(BodyDeclaration::isConstructorDeclaration)
                .map(BodyDeclaration::asConstructorDeclaration)
                .forEach(
                        constructorDeclaration -> {
                            addImport(constructorDeclaration.getAnnotations(), target, source);
                            constructorDeclaration.getParameters()
                                    .forEach(
                                            parameter -> {
                                                addImport(parameter.getType(), target, source);
                                                addImport(parameter.getAnnotations(), target, source);
                                            }
                                    );
                            addImport(constructorDeclaration.getBody(), target, source);
                        }
                );
    }

    protected void addImport(BlockStmt blockStmt, CompilationUnit target, CompilationUnit source) {
        blockStmt.getStatements()
                .forEach(statement -> addImport(statement, target, source));
    }

    protected void addImport(Statement statement, CompilationUnit target, CompilationUnit source) {
        if (statement.isExpressionStmt()) {
            addImport(statement.asExpressionStmt().getExpression(), target, source);
        } else if (statement.isReturnStmt()) {
            statement.asReturnStmt().getExpression().ifPresent(expression -> addImport(expression, target, source));
        }
    }

    protected void addImport(List<AnnotationExpr> annotations, CompilationUnit target, CompilationUnit source) {
        for (AnnotationExpr annotation : annotations) {
            addImport(annotation, target, source);
        }
    }

    protected void addImport(AnnotationExpr annotationExpr, CompilationUnit target, CompilationUnit source) {
        addImport(annotationExpr.getName(), target, source);
        if (annotationExpr.isNormalAnnotationExpr()) {
            annotationExpr.asNormalAnnotationExpr().getPairs()
                    .forEach(memberValuePair -> addImport(memberValuePair.getValue(), target, source));
        } else if (annotationExpr.isSingleMemberAnnotationExpr()) {
            addImport(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue(), target, source);
        }
    }

    protected void addImport(Expression expression, CompilationUnit target, CompilationUnit source) {
        if (expression.isClassExpr()) {
            addImport(expression.asClassExpr().getType(), target, source);
        } else if (expression.isAnnotationExpr()) {
            addImport(expression.asAnnotationExpr(), target, source);
        } else if (expression.isNameExpr()) {
            addImport(expression.asNameExpr().getName(), target, source);
        } else if (expression.isObjectCreationExpr()) {
            addImport(expression.asObjectCreationExpr().getType(), target, source);
            expression.asObjectCreationExpr().getArguments().forEach(argExpr -> addImport(argExpr, target, source));
            expression.asObjectCreationExpr().getTypeArguments().ifPresent(args -> args.forEach(arg -> addImport(arg.getElementType(), target, source)));
            expression.asObjectCreationExpr().getScope().ifPresent(scopeExpr -> addImport(scopeExpr, target, source));
        } else if (expression.isTypeExpr()) {
            addImport(expression.asTypeExpr().getType(), target, source);
        } else if (expression.isArrayInitializerExpr()) {
            expression.asArrayInitializerExpr().getValues().forEach(itemExpr -> addImport(itemExpr, target, source));
        } else if (expression.isVariableDeclarationExpr()) {
            addImport(expression.asVariableDeclarationExpr().getAnnotations(), target, source);
            expression.asVariableDeclarationExpr().getVariables()
                    .forEach(variableDeclarator -> {
                        addImport(variableDeclarator.getType(), target, source);
                        variableDeclarator.getInitializer().ifPresent(initExpression -> addImport(initExpression, target, source));
                    });
        } else if (expression.isMethodCallExpr()) {
            expression.asMethodCallExpr().getArguments().forEach(argExpr -> addImport(argExpr, target, source));
            expression.asMethodCallExpr().getTypeArguments().ifPresent(args -> args.forEach(arg -> addImport(arg.getElementType(), target, source)));
            expression.asMethodCallExpr().getScope().ifPresent(scopeExpr -> addImport(scopeExpr, target, source));
        }
    }

    protected void addImport(Type type, CompilationUnit target, CompilationUnit source) {
        if (type.isClassOrInterfaceType()) {
            addImport(type.asClassOrInterfaceType().getName(), target, source);
        } else if (type.isTypeParameter()) {
            addImport(type.asTypeParameter().getElementType(), target, source);
            type.asTypeParameter().getTypeBound()
                    .forEach(classOrInterfaceType -> addImport(classOrInterfaceType, target, source));
        } else if (type.isArrayType()) {
            addImport(type.asArrayType().getElementType(), target, source);
            addImport(type.asArrayType().getComponentType(), target, source);
        }
    }

    protected void addImport(Name name, CompilationUnit target, CompilationUnit source) {
        addImport(name.getIdentifier(), target, source);
    }

    protected void addImport(SimpleName name, CompilationUnit target, CompilationUnit source) {
        addImport(name.getIdentifier(), target, source);
    }

    protected void addImport(String name, CompilationUnit target, CompilationUnit source) {
        Optional<ImportDeclaration> sourceImport = source.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name))
                .findFirst();

        if (sourceImport.isPresent()) {
            target.addImport(sourceImport.get());
            return;
        }

        Optional<ImportDeclaration> classImportInAsterisk = source.getImports().stream()
                .filter(ImportDeclaration::isAsterisk)
                .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name)))
                .findFirst();

        if (classImportInAsterisk.isPresent()) {
            target.addImport(classImportInAsterisk.get());
            return;
        }

        source.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name))
                .filter(this::classExist)
                .ifPresent(target::addImport);
    }

    protected String getNameByType(CompilationUnit compilationUnit, SimpleName name) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
                .map(NodeWithName::getNameAsString)
                .findFirst()
                .orElseGet(() -> compilationUnit.getImports().stream()
                        .filter(ImportDeclaration::isAsterisk)
                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
                        .map(importDeclaration -> importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                        .findFirst()
                        .orElseGet(() -> compilationUnit.getPackageDeclaration()
                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
                                .orElseGet(name::asString))
                );
    }

    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return getNameByType(compilationUnit, type.getName());
    }

    protected TypeElement getElementByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return elements.getTypeElement(getNameByType(compilationUnit, type));
    }

    protected String getSourceByType(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
        return trees.getPath(getElementByType(compilationUnit, type)).getCompilationUnit().toString();
    }

    protected boolean classExist(String className) {
        return elements.getTypeElement(className) != null;
    }
}
