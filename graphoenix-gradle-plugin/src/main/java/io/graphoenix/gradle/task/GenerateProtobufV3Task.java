package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.protobuf.builder.ProtobufFileBuilder;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerateProtobufV3Task extends BaseTask {

    private final ProtobufFileBuilder protobufFileBuilder;

    public GenerateProtobufV3Task() {
        this.protobufFileBuilder = BeanContext.get(ProtobufFileBuilder.class);
    }

    @TaskAction
    public void generateGraphQLSource() {
        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        if (graphQLConfig == null) {
            graphQLConfig = new GraphQLConfig();
        }
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Path protoPath = Path.of(sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\java")).getAsPath()).getParent().resolve("proto");
        try {
            init();
            List<CompilationUnit> compilationUnits = getCompilationUnits();
            registerInvoke(compilationUnits);
            if (graphQLConfig.getPackageName() == null) {
                getDefaultPackageName(compilationUnits).ifPresent(graphQLConfig::setPackageName);
            }
            if (Files.notExists(protoPath)) {
                Files.createDirectories(protoPath);
            }
            Set<Map.Entry<String, String>> entries = protobufFileBuilder.setGraphQLConfig(graphQLConfig).buildProto3().entrySet();
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
