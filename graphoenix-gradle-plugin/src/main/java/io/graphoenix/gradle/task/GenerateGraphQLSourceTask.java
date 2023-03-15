package io.graphoenix.gradle.task;

import com.github.javaparser.ast.CompilationUnit;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
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
import java.util.List;

public class GenerateGraphQLSourceTask extends BaseTask {

    @TaskAction
    public void generateGraphQLSourceTask() {

        init();
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        JavaFileBuilder javaFileBuilder = BeanContext.get(JavaFileBuilder.class);
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String javaPath = sourceSet.getJava().getSourceDirectories().filter(file -> file.getPath().contains("src\\main\\java")).getAsPath();
        try {
            List<CompilationUnit> compilationUnits = buildCompilationUnits();
            if (graphQLConfig.getPackageName() == null) {
                getDefaultPackageName(compilationUnits).ifPresent(graphQLConfig::setPackageName);
            }
            registerInvoke(compilationUnits);
            configRegister.registerPreset(createClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
            javaFileBuilder.writeToPath(new File(javaPath));
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
