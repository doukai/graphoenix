package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.error.ElementProblem;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import io.vavr.CheckedFunction0;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.ElementErrorType.UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE;

@ApplicationScoped
public class OperationInterfaceImplementer {

    public void writeToFiler(PackageElement packageElement, TypeElement typeElement, TypeMirror operationDAO, String suffix, Filer filer) throws IOException {
        this.buildImplementClass(packageElement, typeElement, operationDAO, suffix).writeTo(filer);
    }

    public JavaFile buildImplementClass(PackageElement packageElement, TypeElement typeElement, TypeMirror operationDAO, String suffix) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .superclass(operationDAO)
                .addSuperinterface(typeElement.asType())
                .addFields(buildFileContentFields(typeElement))
                .addStaticBlock(buildFileContentFieldInitializeCodeBlock(packageElement, typeElement, suffix))
                .addMethods(typeElement.getEnclosedElements()
                        .stream()
                        .filter(element -> element.getKind().equals(ElementKind.METHOD))
                        .map(element -> executableElementToMethodSpec(typeElement, (ExecutableElement) element))
                        .collect(Collectors.toList())
                );

        Logger.info("{} build success", typeElement.getSimpleName().toString() + "Impl");
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
                element.getSimpleName().toString().concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        ).build();
    }

    private CodeBlock buildFileContentFieldInitializeCodeBlock(PackageElement packageElement, TypeElement typeElement, String suffix) {
        ClassName typeClassName = ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl");
        CodeBlock.Builder builder = CodeBlock.builder();
        typeElement.getEnclosedElements()
                .forEach(element ->
                        builder.addStatement(
                                "$L = fileToString($T.class,$S)",
                                element.getSimpleName().toString()
                                        .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                                typeClassName,
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
                        "return $L($L, $T.of($L), $T.class)",
                        getMethodName(executableElement),
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
            Class<?> class0 = CheckedFunction0.of(() -> Class.forName(((ParameterizedTypeName) typeName0).rawType.toString())).unchecked().get();
            if (class0.isAssignableFrom(Mono.class)) {
                TypeName typeName1 = ((ParameterizedTypeName) typeName0).typeArguments.get(0);
                if (typeName1 instanceof ParameterizedTypeName) {
                    Class<?> class1 = CheckedFunction0.of(() -> Class.forName(((ParameterizedTypeName) typeName1).rawType.toString())).unchecked().get();
                    if (class1.isAssignableFrom(List.class) || class1.isAssignableFrom(Set.class) || class1.isAssignableFrom(Collection.class)) {
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
            } else if (class0.isAssignableFrom(List.class) || class0.isAssignableFrom(Set.class) || class0.isAssignableFrom(Collection.class)) {
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
        throw new ElementProblem(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(executableElement.getReturnType().toString()));
    }
}
