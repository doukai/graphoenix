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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {
        CodegenConfiguration codegenConfiguration = getProject().getExtensions().findByType(CodegenConfiguration.class);
        assert codegenConfiguration != null;

        IGraphQLDocumentManager manager = new GraphQLDocumentManager(
                new GraphQLOperationManager(),
                new GraphQLSchemaManager(),
                new GraphQLDirectiveManager(),
                new GraphQLObjectManager(),
                new GraphQLInterfaceManager(),
                new GraphQLUnionManager(),
                new GraphQLFieldManager(),
                new GraphQLInputObjectManager(),
                new GraphQLInputValueManager(),
                new GraphQLEnumManager(),
                new GraphQLScalarManager(),
                new GraphQLFragmentManager());

        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);


        try {
            if (codegenConfiguration.getGraphQL() != null) {
                manager.registerDocument(codegenConfiguration.getGraphQL());
            } else if (codegenConfiguration.getGraphQLFileName() != null) {
                sourceSet.getResources().getFiles().forEach(file -> System.out.println(file.getName()));
                System.out.println(codegenConfiguration.getGraphQLFileName());

                Optional<File> optionalFile = sourceSet.getResources().getFiles().stream().filter(file -> file.getName().equals(codegenConfiguration.getGraphQLFileName())).findFirst();


                System.out.println(sourceSet.getResources().getName());
                if (optionalFile.isPresent()) {
                    String content = Files.readString(optionalFile.get().toPath(), StandardCharsets.UTF_8);

                    System.out.println(content);
//                    manager.registerFile(optionalFile.get().getPath());
                    manager.registerDocument(
                            "interface Meta {\n" +
                            "version: Int\n" +
                            "isDeprecated: Boolean\n" +
                            "}\n" +
                            "\n" +
                            "enum Operator {\n" +
                            "EQ\n" +
                            "NEQ\n" +
                            "LK\n" +
                            "NLK\n" +
                            "GT\n" +
                            "NLTE\n" +
                            "GTE\n" +
                            "NLT\n" +
                            "LT\n" +
                            "NGTE\n" +
                            "LTE\n" +
                            "NGT\n" +
                            "NIL\n" +
                            "NNIL\n" +
                            "}\n" +
                            "\n" +
                            "enum Conditional {\n" +
                            "AND\n" +
                            "OR\n" +
                            "}\n" +
                            "\n" +
                            "input IDExpression {\n" +
                            "opr: Operator = EQ\n" +
                            "val: ID\n" +
                            "in: [ID]\n" +
                            "}\n" +
                            "\n" +
                            "input StringExpression {\n" +
                            "opr: Operator = EQ\n" +
                            "val: String\n" +
                            "in: [String]\n" +
                            "}\n" +
                            "\n" +
                            "input IntExpression {\n" +
                            "opr: Operator = EQ\n" +
                            "val: Int\n" +
                            "in: [Int]\n" +
                            "}\n" +
                            "\n" +
                            "input FloatExpression {\n" +
                            "opr: Operator = EQ\n" +
                            "val: Float\n" +
                            "in: [Float]\n" +
                            "}\n" +
                            "\n" +
                            "input MapWith {\n" +
                            "type: String\n" +
                            "from: String\n" +
                            "to: String\n" +
                            "}\n" +
                            "\n" +
                            "directive @dataType(\n" +
                            "type: String\n" +
                            "length: Int\n" +
                            "decimals: Int\n" +
                            ") on FIELD_DEFINITION\n" +
                            "\n" +
                            "directive @map(\n" +
                            "from: String\n" +
                            "with: MapWith\n" +
                            "to: String\n" +
                            ") on FIELD_DEFINITION\n" +
                            "\n" +
                            "\n" +
                            "type User @table(engine:\"InnoDB\"){\n" +
                            "    id: ID @column(autoIncrement:true)\n" +
                            "    login  : String!\n" +
                            "    password: String!\n" +
                            "    name: String!\n" +
                            "    age: Int\n" +
                            "    disable: Boolean\n" +
                            "    sex: Sex\n" +
                            "    organizationId: Int\n" +
                            "    organization: Organization @map(from: \"organizationId\", to: \"id\")\n" +
                            "    roles: [Role!] @map(from: \"id\", with:{type: \"UserRole\" from: \"userId\", to: \"roleId\"}, to: \"id\")\n" +
                            "}\n" +
                            "\n" +
                            "enum Sex {\n" +
                            "    MAN\n" +
                            "    FEMALE\n" +
                            "}\n" +
                            "\n" +
                            "type UserRole @table(engine:\"InnoDB\"){\n" +
                            "    id: ID @column(autoIncrement:true)\n" +
                            "    userId  : Int\n" +
                            "    roleId  : Int\n" +
                            "}\n" +
                            "\n" +
                            "type Role @table(engine:\"InnoDB\") {\n" +
                            "    id: ID @column(autoIncrement:true)\n" +
                            "    name: String!\n" +
                            "    users: [User!] @map(from: \"id\", with:{type: \"UserRole\", from: \"roleId\", to: \"userId\"}, to: \"id\")\n" +
                            "}\n" +
                            "\n" +
                            "type Organization @table(engine:\"InnoDB\") {\n" +
                            "    id: ID @column(autoIncrement:true)\n" +
                            "    aboveId:Int\n" +
                            "    above: Organization @map(from: \"aboveId\", to: \"id\")\n" +
                            "    users: [User!] @map(from: \"id\", to: \"organizationId\")\n" +
                            "    name: String!\n" +
                            "}");

                }
            }
            System.out.println(manager.getObjects().collect(Collectors.toList()).size());

            manager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));
            Document document = new DocumentBuilder(manager).buildDocument();
            manager.registerDocument(document.toString());


            System.out.println(manager.getObjects().collect(Collectors.toList()).size());
            JavaFileBuilder javaFileBuilder = new JavaFileBuilder(manager, codegenConfiguration);

            List<JavaFile> javaFileList = javaFileBuilder.buildJavaFileList();
            System.out.println("~~~~~~~~~~~~~~" + javaFileList.size());
            for (JavaFile javaFile : javaFileList) {

                javaFile.writeTo(Paths.get(getProject().getBuildDir().getPath() + "/generated/sources/graphoenix/java/main"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
