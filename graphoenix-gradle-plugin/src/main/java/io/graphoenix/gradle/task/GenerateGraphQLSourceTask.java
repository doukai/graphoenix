package io.graphoenix.gradle.task;

import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.Path;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {
        JavaGeneratorConfig javaGeneratorConfig = getProject().getExtensions().findByType(JavaGeneratorConfig.class);
        assert javaGeneratorConfig != null;
        IGraphQLDocumentManager manager = new SimpleGraphQLDocumentManager();
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        try {
            if (javaGeneratorConfig.getGraphQL() != null) {
                manager.registerDocument(javaGeneratorConfig.getGraphQL());
            } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                sourceSet.getResources().getFiles().stream()
                        .filter(file -> file.getName().equals(javaGeneratorConfig.getGraphQLFileName()))
                        .findFirst()
                        .flatMap(file -> DOCUMENT_UTIL.graphqlFileTryToDocument(file.getPath()))
                        .ifPresent(manager::registerDocument);
            } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                sourceSet.getResources().getSrcDirs().stream().findFirst()
                        .flatMap(file -> DOCUMENT_UTIL.graphqlPathTryToDocument(Path.of(file.getPath() + File.pathSeparator + javaGeneratorConfig.getGraphQLPath())))
                        .ifPresent(manager::registerDocument);
            }
            manager.registerDocument(new DocumentBuilder(manager).buildDocument().toString());
            JavaFileBuilder javaFileBuilder = new JavaFileBuilder(manager, javaGeneratorConfig);
            sourceSet.getJava().getSrcDirs().stream()
                    .findFirst()
                    .ifPresent(javaFileBuilder::writeToPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
