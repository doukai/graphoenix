package io.graphoenix.java.generator.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "codegen.java")
public class JavaGeneratorConfig {

    private String basePackageName;

    @Optional
    private String objectTypePackageName;

    @Optional
    private String interfaceTypePackageName;

    @Optional
    private String unionTypePackageName;

    @Optional
    private String enumTypePackageName;

    @Optional
    private String inputObjectTypePackageName;

    @Optional
    private String directivePackageName;

    @Optional
    private String annotationPackageName;

    @Optional
    private String graphQL;

    @Optional
    private String graphQLFileName;

    @Optional
    private String graphQLPath;

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public String getObjectTypePackageName() {
        return objectTypePackageName != null ? objectTypePackageName : basePackageName + ".objectType";
    }

    public void setObjectTypePackageName(String objectTypePackageName) {
        this.objectTypePackageName = objectTypePackageName;
    }

    public String getInterfaceTypePackageName() {
        return interfaceTypePackageName != null ? interfaceTypePackageName : basePackageName + ".interfaceType";
    }

    public void setInterfaceTypePackageName(String interfaceTypePackageName) {
        this.interfaceTypePackageName = interfaceTypePackageName;
    }

    public String getUnionTypePackageName() {
        return unionTypePackageName != null ? unionTypePackageName : basePackageName + ".unionType";
    }

    public void setUnionTypePackageName(String unionTypePackageName) {
        this.unionTypePackageName = unionTypePackageName;
    }

    public String getEnumTypePackageName() {
        return enumTypePackageName != null ? enumTypePackageName : basePackageName + ".enumType";
    }

    public void setEnumTypePackageName(String enumTypePackageName) {
        this.enumTypePackageName = enumTypePackageName;
    }

    public String getInputObjectTypePackageName() {
        return inputObjectTypePackageName != null ? inputObjectTypePackageName : basePackageName + ".inputObjectType";
    }

    public void setInputObjectTypePackageName(String inputObjectTypePackageName) {
        this.inputObjectTypePackageName = inputObjectTypePackageName;
    }

    public String getDirectivePackageName() {
        return directivePackageName != null ? directivePackageName : basePackageName + ".directive";
    }

    public void setDirectivePackageName(String directivePackageName) {
        this.directivePackageName = directivePackageName;
    }

    public String getAnnotationPackageName() {
        return annotationPackageName != null ? annotationPackageName : basePackageName + ".annotation";
    }

    public void setAnnotationPackageName(String annotationPackageName) {
        this.annotationPackageName = annotationPackageName;
    }

    public String getGraphQL() {
        return graphQL;
    }

    public void setGraphQL(String graphQL) {
        this.graphQL = graphQL;
    }

    public String getGraphQLFileName() {
        return graphQLFileName;
    }

    public void setGraphQLFileName(String graphQLFileName) {
        this.graphQLFileName = graphQLFileName;
    }

    public String getGraphQLPath() {
        return graphQLPath;
    }

    public void setGraphQLPath(String graphQLPath) {
        this.graphQLPath = graphQLPath;
    }
}
