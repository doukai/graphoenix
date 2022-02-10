package io.graphoenix.java.generator.builder;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.vavr.Tuple3;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.annotation.processing.Filer;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ModuleBuilder {

    public void buildApiModule(String modulePackageName, String moduleClassName, Set<? extends Element> elementList, Types typeUtils, Filer filer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module.class)
                .addMethods(elementList.stream().map(element -> (TypeElement) element).map(typeElement -> buildApiProvides(typeElement, typeUtils)).collect(Collectors.toList()))
                .build();

        JavaFile.builder(modulePackageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec buildApiProvides(TypeElement typeElement, Types typeUtils) {
        Optional<CodeBlock> codeBlock = typeElement.getEnclosedElements().stream()
                .filter(enclosedElement -> enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR))
                .filter(enclosedElement -> enclosedElement.getAnnotation(Inject.class) != null)
                .findFirst()
                .map(enclosedElement -> (ExecutableElement) enclosedElement)
                .map(executableElement ->
                        CodeBlock.join(
                                executableElement.getParameters().stream()
                                        .map(variableElement -> {
                                                    if (variableElement.asType().getKind().equals(TypeKind.DECLARED) &&
                                                            ((TypeElement) ((DeclaredType) variableElement.asType()).asElement()).getQualifiedName().toString().equals(Provider.class.getName())) {
                                                        return CodeBlock.of("$T.getProvider($T.class)",
                                                                ClassName.get(BeanContext.class),
                                                                ClassName.get(((DeclaredType) variableElement.asType()).getTypeArguments().get(0))
                                                        );
                                                    } else {
                                                        return CodeBlock.of("$T.get($T.class)",
                                                                ClassName.get(BeanContext.class),
                                                                ClassName.get(variableElement.asType())
                                                        );
                                                    }
                                                }
                                        )
                                        .collect(Collectors.toList()),
                                ","
                        )
                );

        MethodSpec.Builder builder = MethodSpec.methodBuilder(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Provides.class)
                .returns(ClassName.get(typeElement));

        if (typeElement.getAnnotation(Singleton.class) != null) {
            builder.addAnnotation(Singleton.class);
        }

        if (codeBlock.isPresent()) {
            builder.addStatement("return new $T($L)", ClassName.get(typeElement), codeBlock.get());
        } else {
            builder.addStatement("return new $T()", ClassName.get(typeElement));
        }
        return builder.build();
    }

    public void buildOperationModule(String modulePackageName, String moduleClassName, Set<Tuple3<? extends Element, PackageElement, String>> elementList, Filer filer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module.class)
                .addMethods(elementList.stream().map(tuple3 -> buildOperationProvides((TypeElement) tuple3._1(), tuple3._2(), tuple3._3())).collect(Collectors.toList()))
                .build();

        JavaFile.builder(modulePackageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec buildOperationProvides(TypeElement typeElement, PackageElement packageElement, String implName) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Provides.class)
                .returns(ClassName.get(typeElement))
                .addStatement("return new $T()", ClassName.get(packageElement.getQualifiedName().toString(), implName));

        if (typeElement.getAnnotation(Singleton.class) != null) {
            builder.addAnnotation(Singleton.class);
        }
        return builder.build();
    }
}
