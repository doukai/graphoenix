package io.graphoenix.dagger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import dagger.Component;
import dagger.Module;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.aop.InvocationContext;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.processing.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedAnnotationTypes("dagger.Module")
@AutoService(Processor.class)
public class DaggerModuleProcessor extends AbstractProcessor {

    private Trees trees;
    private JavaParser javaParser;
    private Filer filer;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.javaParser = new JavaParser();
        this.filer = this.processingEnv.getFiler();
        this.elements = this.processingEnv.getElementUtils();
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
                .ifSuccessful(compilationUnit -> {
                            List<CompilationUnit> proxyCompilationUnits = compilationUnit.getTypes().stream()
                                    .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                    .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                    .flatMap(classOrInterfaceDeclaration ->
                                            classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .filter(bodyDeclaration -> !bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                                                    .map(BodyDeclaration::toMethodDeclaration)
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .filter(methodDeclaration -> methodDeclaration.getType().isClassOrInterfaceType())
                                                    .map(methodDeclaration -> methodDeclaration.getType().asClassOrInterfaceType())
                                                    .flatMap(classOrInterfaceType ->
                                                            buildComponentProxy(
                                                                    compilationUnit,
                                                                    classOrInterfaceDeclaration,
                                                                    getSourceByType(compilationUnit, classOrInterfaceType)
                                                            )
                                                    )
                                    )
                                    .collect(Collectors.toList());

                            proxyCompilationUnits.forEach(this::writeToFiler);

                            CompilationUnit proxyModuleCompilationUnit = buildModuleProxy(proxyCompilationUnits, compilationUnit, compilationUnit.getType(0).asClassOrInterfaceDeclaration());
                            writeToFiler(proxyModuleCompilationUnit);

                            proxyCompilationUnits.forEach(
                                    proxyCompilationUnit -> writeToFiler(buildComponentProxyComponent(proxyCompilationUnit, proxyModuleCompilationUnit.getType(0).asClassOrInterfaceDeclaration()))
                            );
                        }
                );
    }

    protected List<FieldDeclaration> getConfigPropertyFieldDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isFieldDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(ConfigProperty.class))
                .map(bodyDeclaration -> (FieldDeclaration) bodyDeclaration)
                .collect(Collectors.toList());
    }

    protected Optional<MethodDeclaration> getInterceptorBeanMethodDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, AnnotationExpr annotationExpr) {
        return getInterceptorBeanMethodDeclarations(classOrInterfaceDeclaration).stream()
                .filter(methodDeclaration ->
                        methodDeclaration.getAnnotationByClass(InterceptorBean.class)
                                .flatMap(methodAnnotationExpr ->
                                        methodAnnotationExpr.asNormalAnnotationExpr().getPairs().stream()
                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("value"))
                                                .findFirst()
                                )
                                .filter(memberValuePair ->
                                        memberValuePair.getValue().asClassExpr().getType().asClassOrInterfaceType().getNameAsString().equals(annotationExpr.getNameAsString())
                                )
                                .isPresent()
                )
                .findFirst();
    }

    protected List<MethodDeclaration> getInterceptorBeanMethodDeclarations(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        return classOrInterfaceDeclaration.getMembers().stream()
                .filter(BodyDeclaration::isMethodDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.isAnnotationPresent(InterceptorBean.class))
                .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.getType().isClassOrInterfaceType())
                .collect(Collectors.toList());
    }

//    protected String getNameByType(CompilationUnit compilationUnit, Name name) {
//        return compilationUnit.getImports().stream()
//                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
//                .map(NodeWithName::getNameAsString)
//                .findFirst()
//                .orElseGet(() -> compilationUnit.getImports().stream()
//                        .filter(ImportDeclaration::isAsterisk)
//                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
//                        .map(importDeclaration -> importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
//                        .findFirst()
//                        .orElseGet(() -> compilationUnit.getPackageDeclaration()
//                                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name.getIdentifier()))
//                                .orElseGet(name::asString))
//                );
//    }
//
//    protected boolean isStaticImport(CompilationUnit compilationUnit, Name name) {
//        return compilationUnit.getImports().stream()
//                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
//                .map(ImportDeclaration::isStatic)
//                .findFirst()
//                .orElseGet(() -> compilationUnit.getImports().stream()
//                        .filter(ImportDeclaration::isAsterisk)
//                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
//                        .map(ImportDeclaration::isStatic)
//                        .findFirst()
//                        .orElse(false)
//                );
//    }
//

//
//    protected boolean isStaticImport(CompilationUnit compilationUnit, SimpleName name) {
//        return compilationUnit.getImports().stream()
//                .filter(importDeclaration -> importDeclaration.getName().getIdentifier().equals(name.getIdentifier()))
//                .map(ImportDeclaration::isStatic)
//                .findFirst()
//                .orElseGet(() -> compilationUnit.getImports().stream()
//                        .filter(ImportDeclaration::isAsterisk)
//                        .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name.getIdentifier())))
//                        .map(ImportDeclaration::isStatic)
//                        .findFirst()
//                        .orElse(false)
//                );
//    }

    //
//    protected boolean isStaticImport(CompilationUnit compilationUnit, ClassOrInterfaceType type) {
//        return isStaticImport(compilationUnit, type.getName());
//    }
//
//    protected String getNameByType(CompilationUnit compilationUnit, ClassOrInterfaceDeclaration declaration) {
//        return getNameByType(compilationUnit, declaration.getName());
//    }
//
//    protected boolean isStaticImport(CompilationUnit compilationUnit, ClassOrInterfaceDeclaration declaration) {
//        return isStaticImport(compilationUnit, declaration.getName());
//    }
//


    protected Stream<CompilationUnit> buildComponentProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, String moduleMethodTypeSource) {
        return javaParser.parse(moduleMethodTypeSource).getResult().stream()
                .flatMap(compilationUnit ->
                        compilationUnit.getTypes().stream()
                                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                                .map(classOrInterfaceDeclaration -> {

                                            ClassOrInterfaceDeclaration componentProxyClassDeclaration = new ClassOrInterfaceDeclaration()
                                                    .addExtendedType(classOrInterfaceDeclaration.getNameAsString())
                                                    .setName(classOrInterfaceDeclaration.getNameAsString().concat("Proxy"));


                                            List<MethodDeclaration> interceptorBeanMethodDeclarations = classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(bodyDeclaration -> !bodyDeclaration.isConstructorDeclaration())
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(bodyDeclaration -> (MethodDeclaration) bodyDeclaration)
                                                    .flatMap(moduleMethodDeclaration -> moduleMethodDeclaration.getAnnotations().stream()
                                                            .map(annotationExpr -> getInterceptorBeanMethodDeclaration(moduleClassDeclaration, annotationExpr))
                                                            .filter(Optional::isPresent)
                                                            .map(Optional::get)
                                                    )
                                                    .collect(Collectors.toList());

                                            interceptorBeanMethodDeclarations.forEach(interceptorBeanMethodDeclaration -> componentProxyClassDeclaration.addField(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString(), Modifier.Keyword.PRIVATE));

                                            classOrInterfaceDeclaration.getConstructors().forEach(
                                                    constructorDeclaration -> {
                                                        ConstructorDeclaration componentProxyClassConstructor = componentProxyClassDeclaration
                                                                .addConstructor(Modifier.Keyword.PUBLIC)
                                                                .addAnnotation(Inject.class)
                                                                .setParameters(constructorDeclaration.getParameters());

                                                        BlockStmt blockStmt = componentProxyClassConstructor.createBody()
                                                                .addStatement("super("
                                                                        .concat(constructorDeclaration.getParameters().stream()
                                                                                .map(NodeWithSimpleName::getNameAsString)
                                                                                .collect(Collectors.joining(",")))
                                                                        .concat(");"));

                                                        interceptorBeanMethodDeclarations
                                                                .forEach(interceptorBeanMethodDeclaration -> {
                                                                            componentProxyClassConstructor.addParameter(interceptorBeanMethodDeclaration.getType(), interceptorBeanMethodDeclaration.getNameAsString());
                                                                            blockStmt.addStatement("this.".concat(interceptorBeanMethodDeclaration.getNameAsString()).concat("=").concat(interceptorBeanMethodDeclaration.getNameAsString()).concat(";"));
                                                                        }
                                                                );
                                                    }
                                            );


                                            CompilationUnit componentProxyCompilationUnit = new CompilationUnit()
                                                    .addType(componentProxyClassDeclaration)
                                                    .addImport(Inject.class)
                                                    .addImport(InvocationContext.class);

                                            classOrInterfaceDeclaration.getMembers().stream()
                                                    .filter(BodyDeclaration::isMethodDeclaration)
                                                    .map(BodyDeclaration::asMethodDeclaration)
                                                    .forEach(superMethodDeclaration -> {

                                                        Map<AnnotationExpr, MethodDeclaration> interceptorBeanMethodMap = superMethodDeclaration.getAnnotations().stream()
                                                                .collect(Collectors.toMap(annotationExpr -> annotationExpr, annotationExpr -> getInterceptorBeanMethodDeclaration(moduleClassDeclaration, annotationExpr)))
                                                                .entrySet().stream()
                                                                .filter(entry -> entry.getValue().isPresent())
                                                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

                                                        if (interceptorBeanMethodMap.keySet().size() > 0) {
                                                            BlockStmt blockStmt = componentProxyClassDeclaration.addMethod(superMethodDeclaration.getNameAsString(), superMethodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                                                    .setParameters(superMethodDeclaration.getParameters())
                                                                    .setType(superMethodDeclaration.getType())
                                                                    .addAnnotation(Override.class)
                                                                    .createBody();

                                                            interceptorBeanMethodMap.forEach((key, value) -> {

                                                                addImport(value.getAnnotationByClass(InterceptorBean.class).orElseThrow(), componentProxyCompilationUnit, compilationUnit);

                                                                StringBuilder createInvocationContext = new StringBuilder(
                                                                        "InvocationContext " + value.getName().getIdentifier() + "Context = new InvocationContext()" +
                                                                                ".setName(\"" + superMethodDeclaration.getNameAsString() + "\")" +
                                                                                ".setTarget(" + "this" + ")" +
                                                                                ".setOwner(" + key.getName().getIdentifier() + ".class)"
                                                                );

                                                                for (Parameter parameter : superMethodDeclaration.getParameters()) {
                                                                    createInvocationContext.append(".addParameterValue(\"").append(parameter.getNameAsString()).append("\",").append(parameter.getNameAsString()).append(")");
                                                                }

                                                                for (MemberValuePair memberValuePair : key.asNormalAnnotationExpr().getPairs()) {
                                                                    createInvocationContext.append(".addOwnerValue(\"").append(memberValuePair.getNameAsString()).append("\",").append(memberValuePair.getValue()).append(")");
                                                                }

                                                                blockStmt.addStatement(createInvocationContext.append(";").toString());
                                                                blockStmt.addStatement("this.".concat(value.getName().getIdentifier()).concat(".before(").concat(value.getName().getIdentifier()).concat("Context);"));
                                                            });

                                                            StringBuilder processStatement = new StringBuilder();
                                                            if (!superMethodDeclaration.getType().isVoidType()) {
                                                                processStatement.append(superMethodDeclaration.getType().asString()).append(" result =");
                                                            }
                                                            processStatement.append(" super.")
                                                                    .append(superMethodDeclaration.getNameAsString())
                                                                    .append("(")
                                                                    .append(superMethodDeclaration.getParameters().stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.joining(",")))
                                                                    .append(");");

                                                            blockStmt.addStatement(processStatement.toString());

                                                            interceptorBeanMethodMap.forEach((key, value) -> {
                                                                if (!superMethodDeclaration.getType().isVoidType()) {
                                                                    blockStmt.addStatement(value.getName().getIdentifier().concat("Context.setReturnValue(result);"));
                                                                }

                                                                blockStmt.addStatement("this.".concat(value.getName().getIdentifier()).concat(".after(").concat(value.getName().getIdentifier()).concat("Context);"));

                                                                if (!superMethodDeclaration.getType().isVoidType()) {
                                                                    blockStmt.addStatement("return result;");
                                                                }
                                                            });
                                                        }
                                                    });

                                            moduleCompilationUnit.getPackageDeclaration().ifPresent(componentProxyCompilationUnit::setPackageDeclaration);

                                            importAllTypesFromSource(componentProxyClassDeclaration, componentProxyCompilationUnit, moduleCompilationUnit);

                                            return componentProxyCompilationUnit;
                                        }
                                ));
    }

//    protected void annotationValueImport(CompilationUnit compilationUnit, CompilationUnit sourceCompilationUnit, Expression expression) {
//        if (expression.isAnnotationExpr()) {
//            compilationUnit.addImport(
//                    getNameByType(sourceCompilationUnit, expression.asAnnotationExpr().getName()),
//                    isStaticImport(sourceCompilationUnit, expression.asAnnotationExpr().getName()),
//                    false
//            );
//        } else if (expression.isClassExpr()) {
//            compilationUnit.addImport(
//                    getNameByType(sourceCompilationUnit, expression.asClassExpr().getType().asClassOrInterfaceType()),
//                    isStaticImport(sourceCompilationUnit, expression.asClassExpr().getType().asClassOrInterfaceType()),
//                    false
//            );
//        } else if (expression.isNameExpr()) {
//            compilationUnit.addImport(
//                    getNameByType(sourceCompilationUnit, expression.asNameExpr().getName()),
//                    isStaticImport(sourceCompilationUnit, expression.asNameExpr().getName()),
//                    false
//            );
//        }
//    }

    protected CompilationUnit buildComponentProxyComponent(CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {
        TypeDeclaration<?> componentProxyClassDeclaration = componentProxyCompilationUnit.getType(0);
        ArrayInitializerExpr modules = new ArrayInitializerExpr();
        modules.getValues().add(new ClassExpr().setType(moduleProxyClassDeclaration.getNameAsString()));

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setInterface(true)
                .setName(componentProxyClassDeclaration.getNameAsString() + "Component")
                .addAnnotation(Singleton.class)
                .addAnnotation(new NormalAnnotationExpr().addPair("modules", modules).setName(Component.class.getSimpleName()));

        classOrInterfaceDeclaration.addMethod("get").setType(componentProxyClassDeclaration.getNameAsString()).removeBody();

        CompilationUnit compilationUnit = new CompilationUnit()
                .addType(classOrInterfaceDeclaration)
                .addImport(Singleton.class)
                .addImport(Component.class);
        componentProxyCompilationUnit.getPackageDeclaration().ifPresent(compilationUnit::setPackageDeclaration);
        importAllTypesFromSource(classOrInterfaceDeclaration, compilationUnit, componentProxyCompilationUnit);

        return compilationUnit;
    }


    protected CompilationUnit buildModuleProxy(List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleCLassDeclaration) {

        List<FieldDeclaration> configPropertyFieldDeclarations = getConfigPropertyFieldDeclarations(moduleCLassDeclaration);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = new ClassOrInterfaceDeclaration()
                .setPublic(true)
                .setName(moduleCLassDeclaration.getNameAsString() + "Proxy")
                .addAnnotation(moduleCLassDeclaration.getAnnotationByClass(Module.class).orElseThrow())
                .addAnnotation(new NormalAnnotationExpr().addPair("value", new StringLiteralExpr("io.graphoenix.dagger.DaggerModuleProcessor")).setName(Generated.class.getSimpleName()));

        classOrInterfaceDeclaration.addField(Config.class, "config", Modifier.Keyword.PRIVATE).getVariable(0).setInitializer("ConfigProvider.getConfig();");

        configPropertyFieldDeclarations
                .forEach(fieldDeclaration ->
                        classOrInterfaceDeclaration.addField(fieldDeclaration.getElementType(), fieldDeclaration.getVariable(0).getNameAsString(), fieldDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                .getVariable(0)
                                .setInitializer(
                                        "config.getValue("
                                                .concat(
                                                        fieldDeclaration.getAnnotationByClass(ConfigProperty.class)
                                                                .orElseThrow()
                                                                .asNormalAnnotationExpr().getPairs().stream()
                                                                .filter(memberValuePair -> memberValuePair.getNameAsString().equals("name"))
                                                                .findFirst()
                                                                .orElseThrow()
                                                                .getValue().toString()
                                                )
                                                .concat(",")
                                                .concat(fieldDeclaration.getElementType().asClassOrInterfaceType().getNameAsString())
                                                .concat(".class);"))
                );

        List<MethodDeclaration> interceptorBeanMethodDeclarations = getInterceptorBeanMethodDeclarations(moduleCLassDeclaration);
        interceptorBeanMethodDeclarations
                .forEach(methodDeclaration -> {
                            classOrInterfaceDeclaration
                                    .addMethod(methodDeclaration.getNameAsString(), methodDeclaration.getModifiers().stream().map(Modifier::getKeyword).toArray(Modifier.Keyword[]::new))
                                    .setParameters(methodDeclaration.getParameters())
                                    .setType(methodDeclaration.getType())
                                    .setAnnotations(methodDeclaration.getAnnotations())
                                    .setBody(methodDeclaration.getBody().orElseThrow());
                        }
                );

        componentProxyCompilationUnits.stream()
                .map(proxyCompilationUnit -> proxyCompilationUnit.getType(0).asClassOrInterfaceDeclaration())
                .forEach(
                        proxyClassOrInterfaceDeclaration -> {

                            MethodDeclaration superTypeMethodDeclaration = moduleCLassDeclaration.getMembers().stream()
                                    .filter(BodyDeclaration::isMethodDeclaration)
                                    .map(BodyDeclaration::asMethodDeclaration)
                                    .filter(declaration -> declaration.getType().isClassOrInterfaceType())
                                    .filter(declaration -> declaration.getType().asClassOrInterfaceType().getNameAsString().equals(proxyClassOrInterfaceDeclaration.getExtendedTypes(0).getNameAsString()))
                                    .findFirst()
                                    .orElseThrow();

                            ConstructorDeclaration injectConstructorDeclaration = proxyClassOrInterfaceDeclaration.getConstructors().stream()
                                    .filter(constructorDeclaration -> constructorDeclaration.isAnnotationPresent(Inject.class))
                                    .findFirst().orElseThrow();


                            NodeList<Parameter> injectConstructorDeclarationParameters = injectConstructorDeclaration.getParameters();


                            BlockStmt blockStmt = superTypeMethodDeclaration.getBody().orElseThrow().clone();
                            blockStmt.getStatements().forEach(
                                    statement -> {
                                        if (statement.isReturnStmt()) {
                                            Expression expression = statement.asReturnStmt().getExpression().orElseThrow();
                                            if (expression.isObjectCreationExpr()) {
                                                ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
                                                objectCreationExpr.setType(proxyClassOrInterfaceDeclaration.getNameAsString());

                                                injectConstructorDeclarationParameters.stream().skip(objectCreationExpr.getArguments().size())
                                                        .forEach(parameter -> objectCreationExpr.addArgument(parameter.getNameAsString()));

                                            }
                                        }
                                    }
                            );

                            classOrInterfaceDeclaration
                                    .addMethod(superTypeMethodDeclaration.getNameAsString(), Modifier.Keyword.PUBLIC)
                                    .setParameters(superTypeMethodDeclaration.getParameters())
                                    .setType(proxyClassOrInterfaceDeclaration.getNameAsString())
                                    .setAnnotations(superTypeMethodDeclaration.getAnnotations())
                                    .setBody(blockStmt);
                        }
                );
        CompilationUnit compilationUnit = new CompilationUnit().addType(classOrInterfaceDeclaration)
                .addImport(Module.class)
                .addImport(Generated.class)
                .addImport(Config.class)
                .addImport(ConfigProvider.class);

        moduleCompilationUnit.getPackageDeclaration().ifPresent(compilationUnit::setPackageDeclaration);

        importAllTypesFromSource(classOrInterfaceDeclaration, compilationUnit, moduleCompilationUnit);

        return compilationUnit;
    }

    protected void writeToFiler(CompilationUnit compilationUnit) {
        try {
            Writer writer = filer.createSourceFile(compilationUnit.getType(0).getFullyQualifiedName().orElseGet(compilationUnit.getType(0)::getNameAsString)).openWriter();
            writer.write(compilationUnit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } else if (expression.isTypeExpr()) {
            addImport(expression.asTypeExpr().getType(), target, source);
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

        Optional<ImportDeclaration> sourceImportInAsterisk = source.getImports().stream()
                .filter(ImportDeclaration::isAsterisk)
                .filter(importDeclaration -> classExist(importDeclaration.getNameAsString().concat(".").concat(name)))
                .findFirst();

        if (sourceImportInAsterisk.isPresent()) {
            target.addImport(sourceImportInAsterisk.get());
            return;
        }

        source.getPackageDeclaration()
                .map(packageDeclaration -> packageDeclaration.getNameAsString().concat(".").concat(name))
                .ifPresent(className -> {
                            if (classExist(className)) {
                                target.addImport(className);
                            }
                        }
                );
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
