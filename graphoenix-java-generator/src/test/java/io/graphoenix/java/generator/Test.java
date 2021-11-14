package io.graphoenix.java.generator;

import com.squareup.javapoet.JavaFile;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.common.manager.*;
import io.graphoenix.java.generator.config.JavaGeneratorConfiguration;
import io.graphoenix.java.generator.spec.TypeSpecBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import java.io.IOException;

import static io.graphoenix.common.utils.YamlConfigUtil.YAML_CONFIG_UTIL;

public class Test {

    @org.junit.jupiter.api.Test
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

        TypeSpecBuilder typeSpecBuilder = new TypeSpecBuilder(manager, javaGeneratorConfiguration);
        manager.getDirectives().forEach(directiveDefinitionContext -> {


            JavaFile javaFile = JavaFile.builder(javaGeneratorConfiguration.getBasePackageName(), typeSpecBuilder.buildAnnotation(directiveDefinitionContext)).build();
            try {
                javaFile.writeTo(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }
}
