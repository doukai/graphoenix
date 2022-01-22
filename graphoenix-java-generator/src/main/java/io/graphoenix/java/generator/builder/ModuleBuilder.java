package io.graphoenix.java.generator.builder;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.vavr.Tuple3;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.*;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleBuilder {

    public void buildModule(String modulePackageName, String moduleClassName, Set<? extends Element> elementList, Filer filer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module.class)
                .addMethods(elementList.stream().map(element -> (TypeElement) element).map(this::buildProvides).collect(Collectors.toList()))
                .build();

        JavaFile.builder(modulePackageName, typeSpec).build().writeTo(filer);
    }

    public void buildInterfaceModule(String modulePackageName, String moduleClassName, Set<Tuple3<? extends Element, PackageElement, String>> elementList, Filer filer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(moduleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module.class)
                .addMethods(elementList.stream().map(tuple3 -> buildProvides((TypeElement) tuple3._1(), tuple3._2(), tuple3._3())).collect(Collectors.toList()))
                .build();

        JavaFile.builder(modulePackageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec buildProvides(TypeElement typeElement) {
        Optional<CodeBlock> codeBlock = typeElement.getEnclosedElements().stream()
                .filter(enclosedElement -> enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR))
                .filter(enclosedElement -> enclosedElement.getAnnotation(Inject.class) != null)
                .findFirst()
                .map(enclosedElement -> (ExecutableElement) enclosedElement)
                .map(executableElement ->
                        CodeBlock.join(
                                executableElement.getParameters().stream()
                                        .map(variableElement ->
                                                CodeBlock.of("$T.get($T.class)",
                                                        ClassName.get(BeanContext.class),
                                                        ClassName.get(variableElement.asType())
                                                )
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

    private MethodSpec buildProvides(TypeElement typeElement, PackageElement packageElement, String implName) {
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
