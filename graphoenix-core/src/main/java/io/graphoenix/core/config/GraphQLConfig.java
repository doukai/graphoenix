package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "graphql")
public class GraphQLConfig {

    @Optional
    private String graphQL;

    @Optional
    private String graphQLFileName;

    @Optional
    private String graphQLPath;

    @Optional
    private Boolean build = true;

    @Optional
    private Boolean englishPlural = false;

    private String packageName;

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

    public Boolean getBuild() {
        return build;
    }

    public void setBuild(Boolean build) {
        this.build = build;
    }

    public Boolean getEnglishPlural() {
        return englishPlural;
    }

    public void setEnglishPlural(Boolean englishPlural) {
        this.englishPlural = englishPlural;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getObjectTypePackageName() {
        return objectTypePackageName != null ? objectTypePackageName : packageName + ".objectType";
    }

    public void setObjectTypePackageName(String objectTypePackageName) {
        this.objectTypePackageName = objectTypePackageName;
    }

    public String getInterfaceTypePackageName() {
        return interfaceTypePackageName != null ? interfaceTypePackageName : packageName + ".interfaceType";
    }

    public void setInterfaceTypePackageName(String interfaceTypePackageName) {
        this.interfaceTypePackageName = interfaceTypePackageName;
    }

    public String getUnionTypePackageName() {
        return unionTypePackageName != null ? unionTypePackageName : packageName + ".unionType";
    }

    public void setUnionTypePackageName(String unionTypePackageName) {
        this.unionTypePackageName = unionTypePackageName;
    }

    public String getEnumTypePackageName() {
        return enumTypePackageName != null ? enumTypePackageName : packageName + ".enumType";
    }

    public void setEnumTypePackageName(String enumTypePackageName) {
        this.enumTypePackageName = enumTypePackageName;
    }

    public String getInputObjectTypePackageName() {
        return inputObjectTypePackageName != null ? inputObjectTypePackageName : packageName + ".inputObjectType";
    }

    public void setInputObjectTypePackageName(String inputObjectTypePackageName) {
        this.inputObjectTypePackageName = inputObjectTypePackageName;
    }

    public String getDirectivePackageName() {
        return directivePackageName != null ? directivePackageName : packageName + ".directive";
    }

    public void setDirectivePackageName(String directivePackageName) {
        this.directivePackageName = directivePackageName;
    }

    public String getAnnotationPackageName() {
        return annotationPackageName != null ? annotationPackageName : packageName + ".annotation";
    }

    public void setAnnotationPackageName(String annotationPackageName) {
        this.annotationPackageName = annotationPackageName;
    }
}
