package io.graphoenix.gradle.task;

import com.squareup.javapoet.JavaFile;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.config.CodegenConfiguration;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {
        CodegenConfiguration codegenConfiguration = getProject().getExtensions().findByType(CodegenConfiguration.class);
        assert codegenConfiguration != null;

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
                new GraphQLFragmentManager());

        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);


        try {
            if (codegenConfiguration.getGraphQL() != null) {
                manager.registerDocument(codegenConfiguration.getGraphQL());
            } else if (codegenConfiguration.getGraphQLFileName() != null) {
                sourceSet.getResources().getFiles().forEach(file -> System.out.println(file.getName()));
                System.out.println(codegenConfiguration.getGraphQLFileName());

                Optional<File> optionalFile = sourceSet.getResources().getFiles().stream().filter(file -> file.getName().equals(codegenConfiguration.getGraphQLFileName())).findFirst();


                System.out.println(sourceSet.getResources().getName());
                if (optionalFile.isPresent()) {
                    manager.registerFile(optionalFile.get().getPath());
                }
            }
            System.out.println(manager.getObjects().collect(Collectors.toList()).size());


            manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
            Document document = new DocumentBuilder(manager).buildDocument();
            manager.registerDocument(document.toString());


            System.out.println(manager.getObjects().collect(Collectors.toList()).size());
            JavaFileBuilder javaFileBuilder = new JavaFileBuilder(manager, codegenConfiguration);

            List<JavaFile> javaFileList = javaFileBuilder.buildJavaFileList();
            System.out.println(getProject().getProjectDir().getPath());
            for (JavaFile javaFile : javaFileList) {

                javaFile.writeTo(Paths.get(getProject().getProjectDir().getPath() + "/src/main/java/"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
