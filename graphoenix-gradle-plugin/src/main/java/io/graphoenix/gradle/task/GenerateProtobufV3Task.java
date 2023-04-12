package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class GenerateProtobufV3Task extends BaseTask {

    @TaskAction
    public void generateProtobufV3Task() {
        init();
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        ProtobufFileBuilder protobufFileBuilder = BeanContext.get(ProtobufFileBuilder.class);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Path protoPath = Path.of(sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains(MAIN_JAVA_PATH)).getAsPath())
                .getParent()
                .resolve("proto")
                .resolve(graphQLConfig.getPackageName().replaceAll("\\.", Matcher.quoteReplacement(File.separator)));
        try {
            registerInvoke();
            configRegister.registerPackage(createClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
            if (Files.notExists(protoPath)) {
                Files.createDirectories(protoPath);
            }
            Set<Map.Entry<String, String>> entries = protobufFileBuilder.buildProto3().entrySet();
            for (Map.Entry<String, String> entry : entries) {
                Files.writeString(
                        protoPath.resolve(entry.getKey().concat(".proto")),
                        entry.getValue()
                );
            }
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
