package io.graphoenix.java.generator.builder;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.spi.context.BaseModuleContext;
import io.graphoenix.spi.context.ModuleContext;
import io.vavr.Tuple2;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;

public class ModuleContextBuilder {

    public void buildModuleContext(String packageName, String className, List<Tuple2<PackageElement, TypeElement>> elementList, Filer filer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(AutoService.class).addMember("value", "$T.class", ClassName.get(ModuleContext.class)).build())
                .superclass(BaseModuleContext.class)
                .addStaticBlock(buildStaticBlock(elementList))
                .build();

        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private CodeBlock buildStaticBlock(List<Tuple2<PackageElement, TypeElement>> elementList) {

        CodeBlock.Builder builder = CodeBlock.builder();
        elementList.forEach(element -> registerElement(builder, element._1(), element._2()));
        return builder.build();
    }

    private void registerElement(CodeBlock.Builder builder, PackageElement packageElement, TypeElement typeElement) {

        builder.addStatement(
                "put($T.class, $T::get)",
                ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString()),
                ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString().concat("Impl"))
        );
    }
}
