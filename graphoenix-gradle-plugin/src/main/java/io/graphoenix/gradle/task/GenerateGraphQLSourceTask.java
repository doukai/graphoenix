package io.graphoenix.gradle.task;

import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {
        JavaGeneratorConfig javaGeneratorConfig = getProject().getExtensions().findByType(JavaGeneratorConfig.class);
        assert javaGeneratorConfig != null;
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        GraphQLSourceGenerator graphQLSourceGenerator = DaggerGraphQLSourceGeneratorFactory.builder().config(javaGeneratorConfig).build().get();
        graphQLSourceGenerator.generate(sourceSet);
    }
}
