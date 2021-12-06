package io.graphoenix.java.generator.implementer;

import com.pivovarit.function.ThrowingFunction;
import com.squareup.javapoet.*;
import io.graphoenix.common.pipeline.GraphQLDAO;
import io.graphoenix.common.pipeline.PipelineContext;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import javax.lang.model.element.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.squareup.javapoet.TypeName.VOID;

public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;
    private final ThrowingFunction<String, Class<?>, ClassNotFoundException> classForName = Class::forName;

    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public JavaFile buildImplementClass(PackageElement packageElement, TypeElement typeElement, List<String> executeHandlerNames, String suffix, boolean useInject) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl"))
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(GraphQLDAO.class))
                .addSuperinterface(typeElement.asType())
                .addFields(buildFileContentFields(typeElement))
                .addStaticBlock(buildFileContentFieldInitializeCodeBlock(packageElement, typeElement, suffix))
                .addMethods(typeElement.getEnclosedElements()
                        .stream().filter(element -> element.getKind().equals(ElementKind.METHOD))
                        .map(element -> executableElementToMethodSpec(typeElement, (ExecutableElement) element))
                        .collect(Collectors.toList())
                )
                .addMethod(addOperationHandlersMethodSpec(executeHandlerNames));
        if (useInject) {
            builder.addAnnotation(Singleton.class);
        }
        return JavaFile.builder(packageElement.getQualifiedName().toString(), builder.build()).build();
    }

    private List<FieldSpec> buildFileContentFields(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> buildFileContentField(typeElement, element))
                .collect(Collectors.toList());
    }

    private FieldSpec buildFileContentField(TypeElement typeElement, Element element) {
        return FieldSpec.builder(
                TypeName.get(String.class),
                element.getSimpleName().toString()
                        .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build();
    }


    private CodeBlock buildFileContentFieldInitializeCodeBlock(PackageElement packageElement, TypeElement typeElement, String suffix) {
        CodeBlock.Builder builder = CodeBlock.builder();
        typeElement.getEnclosedElements().forEach(element ->
                builder.addStatement(
                        "$L = fileToString($T.class,$S)",
                        element.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                        ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl"),
                        typeElement.getSimpleName().toString()
                                .concat("_")
                                .concat(element.getSimpleName().toString())
                                .concat("_" + typeElement.getEnclosedElements().indexOf(element))
                                .concat(".")
                                .concat(suffix)
                )
        );
        return builder.build();
    }

    private MethodSpec executableElementToMethodSpec(TypeElement typeElement, ExecutableElement executableElement) {

        return MethodSpec.methodBuilder(executableElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        executableElement.getParameters().stream()
                                .map(variableElement -> ParameterSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString()).build())
                                .collect(Collectors.toList())
                )
                .returns(TypeName.get(executableElement.getReturnType()))
                .addException(ClassName.get(Exception.class))
                .addStatement(
                        "return $L(new $T($L, $T.of($L)), $T.class)",
                        getMethodName(executableElement),
                        ClassName.get(PipelineContext.class),
                        executableElement.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                        ClassName.get(Map.class),
                        CodeBlock.join(
                                executableElement.getParameters().stream()
                                        .map(parameter ->
                                                CodeBlock.of(
                                                        "$S, $L",
                                                        parameter.getSimpleName().toString(),
                                                        parameter.getSimpleName().toString()
                                                )
                                        )
                                        .collect(Collectors.toList()),
                                ", "),
                        getGenericType(ClassName.get(executableElement.getReturnType()))
                )
                .build();
    }

    private TypeName getGenericType(TypeName typeName) {
        if (typeName instanceof ParameterizedTypeName) {
            return getGenericType(((ParameterizedTypeName) typeName).typeArguments.get(0));
        } else {
            return typeName;
        }
    }

    private String getMethodName(ExecutableElement executableElement) {
        TypeName typeName0 = ClassName.get(executableElement.getReturnType());
        if (typeName0 instanceof ParameterizedTypeName) {
            Class<?> clazz0 = classForName.uncheck().apply(((ParameterizedTypeName) typeName0).rawType.toString());
            if (clazz0.isAssignableFrom(Mono.class)) {
                TypeName typeName1 = ((ParameterizedTypeName) typeName0).typeArguments.get(0);
                if (typeName1 instanceof ParameterizedTypeName) {
                    Class<?> clazz1 = classForName.uncheck().apply(((ParameterizedTypeName) typeName1).rawType.toString());
                    if (clazz1.isAssignableFrom(List.class) || clazz1.isAssignableFrom(Set.class) || clazz1.isAssignableFrom(Collection.class)) {
                        if (executableElement.getAnnotation(QueryOperation.class) != null) {
                            return "findAllAsync";
                        }
                    }
                } else {
                    if (executableElement.getAnnotation(QueryOperation.class) != null) {
                        return "findOneAsync";
                    } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                        return "saveAsync";
                    }
                }
            } else if (clazz0.isAssignableFrom(List.class) || clazz0.isAssignableFrom(Set.class) || clazz0.isAssignableFrom(Collection.class)) {
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    return "findAll";
                }
            }
        } else {
            if (executableElement.getAnnotation(QueryOperation.class) != null) {
                return "findOne";
            } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                return "save";
            }
        }
        return null;
    }

    private MethodSpec addOperationHandlersMethodSpec(List<String> executeHandlerNames) {

        MethodSpec.Builder addOperationHandlers = MethodSpec.methodBuilder("addOperationHandlers")
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(Override.class)
                .returns(VOID);

        executeHandlerNames.forEach(handlerName ->
                addOperationHandlers
                        .addStatement(
                                "addOperationHandler(new $T())",
                                ClassName.get(classForName.uncheck().apply(handlerName))
                        )
        );
        return addOperationHandlers.build();
    }
}
