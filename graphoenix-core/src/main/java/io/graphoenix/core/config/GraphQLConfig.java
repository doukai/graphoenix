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

    @Optional
    private String modulePackageName;

    @Optional
    private String handlerPackageName;

    @Optional
    private String conditionalInputName;

    @Optional
    private String operatorInputName;

    @Optional
    private int inputLayers = 1;

    @Optional
    private String outputPath;

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
        return objectTypePackageName != null ? objectTypePackageName : packageName + ".dto.objectType";
    }

    public void setObjectTypePackageName(String objectTypePackageName) {
        this.objectTypePackageName = objectTypePackageName;
    }

    public String getInterfaceTypePackageName() {
        return interfaceTypePackageName != null ? interfaceTypePackageName : packageName + ".dto.interfaceType";
    }

    public void setInterfaceTypePackageName(String interfaceTypePackageName) {
        this.interfaceTypePackageName = interfaceTypePackageName;
    }

    public String getUnionTypePackageName() {
        return unionTypePackageName != null ? unionTypePackageName : packageName + ".dto.unionType";
    }

    public void setUnionTypePackageName(String unionTypePackageName) {
        this.unionTypePackageName = unionTypePackageName;
    }

    public String getEnumTypePackageName() {
        return enumTypePackageName != null ? enumTypePackageName : packageName + ".dto.enumType";
    }

    public void setEnumTypePackageName(String enumTypePackageName) {
        this.enumTypePackageName = enumTypePackageName;
    }

    public String getInputObjectTypePackageName() {
        return inputObjectTypePackageName != null ? inputObjectTypePackageName : packageName + ".dto.inputObjectType";
    }

    public void setInputObjectTypePackageName(String inputObjectTypePackageName) {
        this.inputObjectTypePackageName = inputObjectTypePackageName;
    }

    public String getDirectivePackageName() {
        return directivePackageName != null ? directivePackageName : packageName + ".dto.directive";
    }

    public void setDirectivePackageName(String directivePackageName) {
        this.directivePackageName = directivePackageName;
    }

    public String getAnnotationPackageName() {
        return annotationPackageName != null ? annotationPackageName : packageName + ".dto.annotation";
    }

    public void setAnnotationPackageName(String annotationPackageName) {
        this.annotationPackageName = annotationPackageName;
    }

    public String getModulePackageName() {
        return modulePackageName != null ? modulePackageName : packageName + ".module";
    }

    public void setModulePackageName(String modulePackageName) {
        this.modulePackageName = modulePackageName;
    }

    public String getHandlerPackageName() {
        return handlerPackageName != null ? handlerPackageName : packageName + ".handler";
    }

    public void setHandlerPackageName(String handlerPackageName) {
        this.handlerPackageName = handlerPackageName;
    }

    public String getConditionalInputName() {
        return conditionalInputName != null ? conditionalInputName : getEnumTypePackageName() + ".Conditional";
    }

    public void setConditionalInputName(String conditionalInputName) {
        this.conditionalInputName = conditionalInputName;
    }

    public String getOperatorInputName() {
        return operatorInputName != null ? operatorInputName : getEnumTypePackageName() + ".Operator";
    }

    public void setOperatorInputName(String operatorInputName) {
        this.operatorInputName = operatorInputName;
    }

    public int getInputLayers() {
        return inputLayers;
    }

    public void setInputLayers(int inputLayers) {
        this.inputLayers = inputLayers;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
