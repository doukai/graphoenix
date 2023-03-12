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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerateProtobufV3Task extends BaseTask {

    @TaskAction
    public void generateGraphQLSource() {
        init();
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        ProtobufFileBuilder protobufFileBuilder = BeanContext.get(ProtobufFileBuilder.class);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Path protoPath = Path.of(sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\java")).getAsPath()).getParent().resolve("proto");
        try {
            List<CompilationUnit> compilationUnits = buildCompilationUnits();
            if (graphQLConfig.getPackageName() == null) {
                getDefaultPackageName(compilationUnits).ifPresent(graphQLConfig::setPackageName);
            }
            registerInvoke(compilationUnits);
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
        } catch (IOException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
