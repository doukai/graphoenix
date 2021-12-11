package io.graphoenix.spi.config;

public class JavaGeneratorConfig {

    private String basePackageName;

    private String objectTypePackageName;

    private String interfaceTypePackageName;

    private String unionTypePackageName;

    private String enumTypePackageName;

    private String inputObjectTypePackageName;

    private String directivePackageName;

    private String annotationPackageName;

    private String graphQL;

    private String graphQLFileName;

    private String graphQLPath;

    public JavaGeneratorConfig() {
    }

    public JavaGeneratorConfig(String basePackageName,
                               String objectTypePackageName,
                               String interfaceTypePackageName,
                               String unionTypePackageName,
                               String enumTypePackageName,
                               String inputObjectTypePackageName,
                               String directivePackageName,
                               String annotationPackageName,
                               String graphQL,
                               String graphQLFileName,
                               String graphQLPath) {
        this.basePackageName = basePackageName;
        this.objectTypePackageName = objectTypePackageName;
        this.interfaceTypePackageName = interfaceTypePackageName;
        this.unionTypePackageName = unionTypePackageName;
        this.enumTypePackageName = enumTypePackageName;
        this.inputObjectTypePackageName = inputObjectTypePackageName;
        this.directivePackageName = directivePackageName;
        this.annotationPackageName = annotationPackageName;
        this.graphQL = graphQL;
        this.graphQLFileName = graphQLFileName;
        this.graphQLPath = graphQLPath;
    }

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
