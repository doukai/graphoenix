package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {

        final IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        final GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        final IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        final DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        final JavaFileBuilder javaFileBuilder = BeanContext.get(JavaFileBuilder.class);

        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        JavaGeneratorConfig javaGeneratorConfig = getProject().getExtensions().findByType(JavaGeneratorConfig.class);
        assert graphQLConfig != null;
        assert javaGeneratorConfig != null;
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcesPath = sourceSet.getResources().getSourceDirectories().getAsPath();
        String javaPath = sourceSet.getJava().getSourceDirectories().getAsPath();

        try {
            configRegister.registerConfig(graphQLConfig, resourcesPath);
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
            mapper.registerFieldMaps();
            javaFileBuilder.writeToPath(new File(javaPath), javaGeneratorConfig);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
