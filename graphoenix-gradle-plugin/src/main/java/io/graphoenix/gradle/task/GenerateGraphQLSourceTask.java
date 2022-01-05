package io.graphoenix.gradle.task;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.control.Try;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {

        final IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        final IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        final DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        final JavaFileBuilder javaFileBuilder = BeanContext.get(JavaFileBuilder.class);

        JavaGeneratorConfig javaGeneratorConfig = getProject().getExtensions().findByType(JavaGeneratorConfig.class);
        assert javaGeneratorConfig != null;
        javaFileBuilder.setConfiguration(javaGeneratorConfig);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        if (javaGeneratorConfig.getGraphQL() != null) {
            manager.registerGraphQL(javaGeneratorConfig.getGraphQL());
        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
            sourceSet.getResources().getFiles().stream()
                    .filter(file -> file.getName().equals(javaGeneratorConfig.getGraphQLFileName()))
                    .forEach(file -> Try.run(() -> manager.registerFile(file)));
        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
            sourceSet.getResources().getSrcDirs().stream()
                    .map(file -> Path.of(file.getPath() + File.pathSeparator + javaGeneratorConfig.getGraphQLPath()))
                    .forEach(path -> Try.run(() -> manager.registerPath(path)));
        }

        try {
            manager.registerGraphQL(documentBuilder.buildDocument().toString());
            mapper.registerFieldMaps();
            sourceSet.getJava().getSrcDirs().stream().findFirst().ifPresent(javaFileBuilder::writeToPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
