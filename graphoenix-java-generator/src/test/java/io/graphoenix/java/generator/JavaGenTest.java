package io.graphoenix.java.generator;

import com.squareup.javapoet.JavaFile;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.java.generator.config.JavaGeneratorConfiguration;
import io.graphoenix.java.generator.spec.TypeSpecBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class JavaGenTest {

    @Test
    void test() throws IOException {
        JavaGeneratorConfiguration javaGeneratorConfiguration = YAML_CONFIG_UTIL.loadAs(Hammurabi.CONFIG_FILE_NAME, JavaGeneratorConfiguration.class);
        IGraphQLDocumentManager manager = new GraphQLDocumentManager(
                new GraphQLOperationManager(),
                new GraphQLSchemaManager(),
                new GraphQLDirectiveManager(),
                new GraphQLObjectManager(),
                new GraphQLInterfaceManager(),
                new GraphQLUnionManager(),
                new GraphQLFieldManager(),
                new GraphQLInputObjectManager(),
                new GraphQLInputValueManager(),
                new GraphQLEnumManager(),
                new GraphQLScalarManager(),
                new GraphQLFragmentManager()
        );
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("preset.gql"));
        manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("auth.gql"));

        Document document = new DocumentBuilder(manager).buildDocument();
        manager.registerDocument(document.toString());

        TypeSpecBuilder typeSpecBuilder = new TypeSpecBuilder(manager, javaGeneratorConfiguration);
//        manager.getDirectives().forEach(directiveDefinitionContext -> {
//            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildAnnotation(directiveDefinitionContext)).build();
//            try {
//                javaFile.writeTo(System.out);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

//        manager.getObjects().forEach(objectTypeDefinitionContext -> {
//            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildClass(objectTypeDefinitionContext)).build();
//            try {
//                javaFile.writeTo(System.out);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

//        manager.getEnums().forEach(enumTypeDefinitionContext -> {
//            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildEnum(enumTypeDefinitionContext)).build();
//            try {
//                javaFile.writeTo(System.out);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

//        manager.getInputObjects().forEach(inputObjectTypeDefinitionContext -> {
//            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildClass(inputObjectTypeDefinitionContext)).build();
//            try {
//                javaFile.writeTo(System.out);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

//        manager.getInterfaces().forEach(interfaceTypeDefinitionContext -> {
//            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildInterface(interfaceTypeDefinitionContext)).build();
//            try {
//                javaFile.writeTo(System.out);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });

        typeSpecBuilder.buildScalarExpressions().forEach(
                typeSpec -> {

                    JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpec).build();
                    try {
                        javaFile.writeTo(System.out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );

        typeSpecBuilder.buildObjectExpressions().forEach(
                typeSpec -> {

                    JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpec).build();
                    try {
                        javaFile.writeTo(System.out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }
}
