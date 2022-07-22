package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GenerateGraphQLSourceTask extends BaseTask {

    private final IGraphQLDocumentManager manager;
    private final JavaFileBuilder javaFileBuilder;

    public GenerateGraphQLSourceTask() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.javaFileBuilder = BeanContext.get(JavaFileBuilder.class);
    }

    @TaskAction
    public void generateGraphQLSource() {
        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        if (graphQLConfig == null) {
            graphQLConfig = new GraphQLConfig();
        }
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\java")).getAsPath();

        try {
            init();
            registerInvoke(manager);
            javaFileBuilder.writeToPath(new File(javaPath), graphQLConfig);

        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
