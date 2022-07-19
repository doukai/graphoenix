package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class BaseTask extends DefaultTask {

    private final IGraphQLDocumentManager manager;
    private final GraphQLConfigRegister configRegister;
    private final DocumentBuilder documentBuilder;
    private final IGraphQLFieldMapManager mapper;

    public BaseTask() {
        manager = BeanContext.get(IGraphQLDocumentManager.class);
        configRegister = BeanContext.get(GraphQLConfigRegister.class);
        documentBuilder = BeanContext.get(DocumentBuilder.class);
        mapper = BeanContext.get(IGraphQLFieldMapManager.class);
    }

    protected void init() throws IOException, URISyntaxException {
        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        assert graphQLConfig != null;
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcesPath = sourceSet.getResources().getSourceDirectories().getAsPath();

        configRegister.registerPreset(createClassLoader());
        configRegister.registerConfig(graphQLConfig, resourcesPath);
        if (graphQLConfig.getBuild()) {
            manager.registerGraphQL(documentBuilder.buildDocument().toString());
        }
        mapper.registerFieldMaps();
    }

    protected ClassLoader createClassLoader() throws TaskExecutionException {
        List<URL> urls = new ArrayList<>();
        SourceSetContainer sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
        try {
            for (SourceSet sourceSet : sourceSets) {
                for (File file : sourceSet.getCompileClasspath()) {
                    urls.add(file.toURI().toURL());
                }
                for (File classesDir : sourceSet.getOutput().getClassesDirs()) {
                    urls.add(classesDir.toURI().toURL());
                }
            }
        } catch (MalformedURLException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }
}
