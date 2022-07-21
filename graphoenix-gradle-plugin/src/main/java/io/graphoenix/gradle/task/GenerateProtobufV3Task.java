package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.protobuf.builder.ProtobufFileBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenerateProtobufV3Task extends BaseTask {

    public static final String PROTO3_FILE_NAME = "schema.proto";

    private final IGraphQLDocumentManager manager;
    private final ProtobufFileBuilder protobufFileBuilder;

    public GenerateProtobufV3Task() {
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.protobufFileBuilder = BeanContext.get(ProtobufFileBuilder.class);
    }

    @TaskAction
    public void generateGraphQLSource() {
        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        if (graphQLConfig == null) {
            graphQLConfig = new GraphQLConfig();
        }
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcePath = sourceSet.getResources().getSourceDirectories().getAsPath();

        try {
            init();
            registerInvoke(manager);
            Files.writeString(
                    Path.of(resourcePath.concat(File.separator).concat(PROTO3_FILE_NAME)),
                    protobufFileBuilder.setGraphQLConfig(graphQLConfig).buildProto3()
            );
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
