package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GeneratePackageGraphQLTask extends BaseTask {

    @TaskAction
    public void generatePackageGraphQLTask() {
        init();
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String resourcePath = sourceSet.getResources().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\resource")).getAsPath();
        try {
            List<CompilationUnit> compilationUnits = buildCompilationUnits();
            if (graphQLConfig.getPackageName() == null) {
                getDefaultPackageName(compilationUnits).ifPresent(graphQLConfig::setPackageName);
            }
            registerInvoke(compilationUnits);
            Path filePath = Path.of(resourcePath).resolve("META-INF").resolve("graphql");
            if (Files.notExists(filePath)) {
                Files.createDirectories(filePath);
            }
            Files.writeString(
                    filePath.resolve("package.gql"),
                    documentBuilder.getDocument().toString()
            );
        } catch (IOException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
