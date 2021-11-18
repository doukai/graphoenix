package io.graphoenix.gradle.task;

import com.squareup.javapoet.JavaFile;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.Document;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.config.CodegenConfiguration;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.graphoenix.common.utils.DocumentUtil.DOCUMENT_UTIL;

public class GenerateGraphQLSourceTask extends DefaultTask {


    @TaskAction
    public void generateGraphQLSource() {
        CodegenConfiguration codegenConfiguration = getProject().getExtensions().findByType(CodegenConfiguration.class);
        assert codegenConfiguration != null;
        IGraphQLDocumentManager manager = new SimpleGraphQLDocumentManager();
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        try {
            if (codegenConfiguration.getGraphQL() != null) {
                manager.registerDocument(codegenConfiguration.getGraphQL());
            } else if (codegenConfiguration.getGraphQLFileName() != null) {
                sourceSet.getResources().getFiles().stream()
                        .filter(file -> file.getName().equals(codegenConfiguration.getGraphQLFileName()))
                        .findFirst()
                        .flatMap(file -> DOCUMENT_UTIL.graphqlFileTryToDocument(file.getPath()))
                        .ifPresent(manager::registerDocument);
            } else if (codegenConfiguration.getGraphQLPath() != null) {
                DOCUMENT_UTIL.graphqlPathTryToDocument(Path.of(sourceSet.getResources().getAsPath() + File.pathSeparator + codegenConfiguration.getGraphQLPath()))
                        .ifPresent(manager::registerDocument);
            }
            manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
            Document document = new DocumentBuilder(manager).buildDocument();
            manager.registerDocument(document.toString());
            JavaFileBuilder javaFileBuilder = new JavaFileBuilder(manager, codegenConfiguration);

            List<JavaFile> javaFileList = javaFileBuilder.buildJavaFileList();
            for (JavaFile javaFile : javaFileList) {
                javaFile.writeTo(Paths.get(getProject().getProjectDir().getPath() + "/src/main/java/"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
