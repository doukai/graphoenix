package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeneratePackageGraphQLTask extends BaseTask {

    @TaskAction
    public void generatePackageGraphQLTask() {
        init();
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcePath = sourceSet.getResources().getSourceDirectories().filter(file -> file.getPath().contains(MAIN_RESOURCES_PATH)).getAsPath();
        try {
            registerInvoke();
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPackage(createClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            } else {
                mapper.registerFieldMaps();
            }
            Path filePath = Path.of(resourcePath).resolve("META-INF").resolve("graphql");
            if (Files.notExists(filePath)) {
                Files.createDirectories(filePath);
            }
            Files.writeString(
                    filePath.resolve("package.gql"),
                    documentBuilder.getPackageDocument().toString()
            );
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
