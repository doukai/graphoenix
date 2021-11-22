package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.*;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.lang.model.element.*;
import java.util.stream.Collectors;

public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;

    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public JavaFile buildImplementClass(PackageElement packageElement, TypeElement typeElement) {
        TypeSpec implement = TypeSpec.classBuilder(ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl"))

                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(typeElement.asType())
                .addMethods(typeElement.getEnclosedElements()
                        .stream().filter(element -> element.getKind().equals(ElementKind.METHOD))
                        .map(element -> executableElementToMethodSpec((ExecutableElement) element))
                        .collect(Collectors.toList())
                )
                .build();
        return JavaFile.builder(packageElement.getQualifiedName().toString(), implement).build();
    }

    private MethodSpec executableElementToMethodSpec(ExecutableElement executableElement) {

        return MethodSpec.methodBuilder(executableElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        executableElement.getParameters().stream()
                                .map(variableElement -> ParameterSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString()).build())
                                .collect(Collectors.toList())
                )
                .returns(TypeName.get(executableElement.getReturnType()))
                .addStatement("return $L", "null")
                .build();
    }
}
