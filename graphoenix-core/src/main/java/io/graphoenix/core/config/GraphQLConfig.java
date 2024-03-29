package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.util.Set;

@ConfigProperties(prefix = "graphql")
public class GraphQLConfig {

    @Optional
    private String graphQL;

    @Optional
    private String graphQLFileName;

    @Optional
    private String graphQLPath = "graphql";

    @Optional
    private String defaultOperationHandlerName;

    @Optional
    private Boolean build = true;

    @Optional
    private Boolean englishPlural = false;

    @Optional
    private Boolean backup = false;

    @Optional
    private Boolean compensating = false;

    @Optional
    private String packageName;

    @Optional
    private Set<String> localPackageNames;

    @Optional
    private String packageRegister = "default";

    @Optional
    private String packageLoadBalance = "random";

    @Optional
    private String operationTypeFetchProtocol = "grpc";

    @Optional
    private Boolean fetchToMap = false;

    @Optional
    private Boolean mapToFetch = false;

    @Optional
    private String grpcPackageName;

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
    private String operationPackageName;

    @Optional
    private String grpcObjectTypePackageName;

    @Optional
    private String grpcInterfaceTypePackageName;

    @Optional
    private String grpcUnionTypePackageName;

    @Optional
    private String grpcEnumTypePackageName;

    @Optional
    private String grpcInputObjectTypePackageName;

    @Optional
    private String grpcDirectivePackageName;

    @Optional
    private String grpcHandlerPackageName;

    @Optional
    private String conditionalInputName;

    @Optional
    private String operatorInputName;

    @Optional
    private Integer inputLayers = 1;

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

    public String getDefaultOperationHandlerName() {
        return defaultOperationHandlerName;
    }

    public void setDefaultOperationHandlerName(String defaultOperationHandlerName) {
        this.defaultOperationHandlerName = defaultOperationHandlerName;
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

    public Boolean getBackup() {
        return backup;
    }

    public void setBackup(Boolean backup) {
        this.backup = backup;
    }

    public Boolean getCompensating() {
        return compensating;
    }

    public void setCompensating(Boolean compensating) {
        this.compensating = compensating;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Set<String> getLocalPackageNames() {
        return localPackageNames;
    }

    public void setLocalPackageNames(Set<String> localPackageNames) {
        this.localPackageNames = localPackageNames;
    }

    public String getPackageRegister() {
        return packageRegister;
    }

    public void setPackageRegister(String packageRegister) {
        this.packageRegister = packageRegister;
    }

    public String getPackageLoadBalance() {
        return packageLoadBalance;
    }

    public void setPackageLoadBalance(String packageLoadBalance) {
        this.packageLoadBalance = packageLoadBalance;
    }

    public String getOperationTypeFetchProtocol() {
        return operationTypeFetchProtocol;
    }

    public void setOperationTypeFetchProtocol(String operationTypeFetchProtocol) {
        this.operationTypeFetchProtocol = operationTypeFetchProtocol;
    }

    public Boolean getFetchToMap() {
        return fetchToMap;
    }

    public void setFetchToMap(Boolean fetchToMap) {
        this.fetchToMap = fetchToMap;
    }

    public Boolean getMapToFetch() {
        return mapToFetch;
    }

    public void setMapToFetch(Boolean mapToFetch) {
        this.mapToFetch = mapToFetch;
    }

    public String getGrpcPackageName() {
        return grpcPackageName != null ? grpcPackageName : packageName + ".grpc";
    }

    public void setGrpcPackageName(String grpcPackageName) {
        this.grpcPackageName = grpcPackageName;
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

    public String getOperationPackageName() {
        return operationPackageName != null ? operationPackageName : packageName + ".operation";
    }

    public void setOperationPackageName(String operationPackageName) {
        this.operationPackageName = operationPackageName;
    }

    public String getGrpcObjectTypePackageName() {
        return grpcObjectTypePackageName != null ? grpcObjectTypePackageName : getObjectTypePackageName() + ".grpc";
    }

    public void setGrpcObjectTypePackageName(String grpcObjectTypePackageName) {
        this.grpcObjectTypePackageName = grpcObjectTypePackageName;
    }

    public String getGrpcInterfaceTypePackageName() {
        return grpcInterfaceTypePackageName != null ? grpcInterfaceTypePackageName : getInterfaceTypePackageName() + ".grpc";
    }

    public void setGrpcInterfaceTypePackageName(String grpcInterfaceTypePackageName) {
        this.grpcInterfaceTypePackageName = grpcInterfaceTypePackageName;
    }

    public String getGrpcUnionTypePackageName() {
        return grpcUnionTypePackageName != null ? grpcUnionTypePackageName : getUnionTypePackageName() + ".grpc";
    }

    public void setGrpcUnionTypePackageName(String grpcUnionTypePackageName) {
        this.grpcUnionTypePackageName = grpcUnionTypePackageName;
    }

    public String getGrpcEnumTypePackageName() {
        return grpcEnumTypePackageName != null ? grpcEnumTypePackageName : getEnumTypePackageName() + ".grpc";
    }

    public void setGrpcEnumTypePackageName(String grpcEnumTypePackageName) {
        this.grpcEnumTypePackageName = grpcEnumTypePackageName;
    }

    public String getGrpcInputObjectTypePackageName() {
        return grpcInputObjectTypePackageName != null ? grpcInputObjectTypePackageName : getInputObjectTypePackageName() + ".grpc";
    }

    public void setGrpcInputObjectTypePackageName(String grpcInputObjectTypePackageName) {
        this.grpcInputObjectTypePackageName = grpcInputObjectTypePackageName;
    }

    public String getGrpcDirectivePackageName() {
        return grpcDirectivePackageName != null ? grpcDirectivePackageName : getDirectivePackageName() + ".grpc";
    }

    public void setGrpcDirectivePackageName(String grpcDirectivePackageName) {
        this.grpcDirectivePackageName = grpcDirectivePackageName;
    }

    public String getGrpcHandlerPackageName() {
        return grpcHandlerPackageName != null ? grpcHandlerPackageName : getHandlerPackageName() + ".grpc";
    }

    public void setGrpcHandlerPackageName(String grpcHandlerPackageName) {
        this.grpcHandlerPackageName = grpcHandlerPackageName;
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

    public Integer getInputLayers() {
        return inputLayers;
    }

    public void setInputLayers(Integer inputLayers) {
        this.inputLayers = inputLayers;
    }
}
