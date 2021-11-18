package io.graphoenix.java.generator.builder;

import com.squareup.javapoet.JavaFile;
import io.graphoenix.java.generator.config.CodegenConfiguration;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import java.util.List;
import java.util.stream.Collectors;

public class JavaFileBuilder {

    private final IGraphQLDocumentManager manager;
    private final CodegenConfiguration configuration;

    public JavaFileBuilder(IGraphQLDocumentManager manager, CodegenConfiguration configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public List<JavaFile> buildJavaFileList() {

        TypeSpecBuilder typeSpecBuilder = new TypeSpecBuilder(manager, configuration);
        List<JavaFile> javaFileList = manager.getDirectives().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()).collect(Collectors.toList());
        javaFileList.addAll(manager.getInputObjects().map(typeSpecBuilder::buildAnnotation).map(typeSpec -> JavaFile.builder(configuration.getDirectivePackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(manager.getEnums().map(typeSpecBuilder::buildEnum).map(typeSpec -> JavaFile.builder(configuration.getEnumTypePackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(manager.getInterfaces().map(typeSpecBuilder::buildInterface).map(typeSpec -> JavaFile.builder(configuration.getInterfaceTypePackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(manager.getInputObjects().map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getInputObjectTypePackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(manager.getObjects().map(typeSpecBuilder::buildClass).map(typeSpec -> JavaFile.builder(configuration.getObjectTypePackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(typeSpecBuilder.buildObjectTypeExpressionAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(typeSpecBuilder.buildObjectTypeExpressionsAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(typeSpecBuilder.buildObjectTypeInputAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()).collect(Collectors.toList()));
        javaFileList.addAll(typeSpecBuilder.buildObjectTypeInputsAnnotations().map(typeSpec -> JavaFile.builder(configuration.getAnnotationPackageName(), typeSpec).build()).collect(Collectors.toList()));

        return javaFileList;
    }


}
