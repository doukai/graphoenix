package io.graphoenix.gradle.task;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateIntrospectionSQLTask extends BaseTask {

    private final IntrospectionMutationBuilder introspectionMutationBuilder;
    private final GraphQLMutationToStatements mutationToStatements;

    public GenerateIntrospectionSQLTask() {
        this.introspectionMutationBuilder = BeanContext.get(IntrospectionMutationBuilder.class);
        this.mutationToStatements = BeanContext.get(GraphQLMutationToStatements.class);
    }

    @TaskAction
    public void GenerateIntrospectionSQL() {

        GraphQLConfig graphQLConfig = getProject().getExtensions().findByType(GraphQLConfig.class);
        if (graphQLConfig == null) {
            graphQLConfig = new GraphQLConfig();
        }

        try {
            init();
            Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
            Stream<String> introspectionMutationSQLStream = mutationToStatements.createStatementsSQL(operation.toString());
            String introspectionMutationSQL = introspectionMutationSQLStream.collect(Collectors.joining(";\r\n"));
            Files.writeString(Path.of(graphQLConfig.getOutputPath().concat(File.separator).concat("introspection.sql")), introspectionMutationSQL);
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            throw new TaskExecutionException(this, e);
        }
    }
}
