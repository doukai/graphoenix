package io.graphoenix.java.generator.config;

public class JavaGeneratorConfiguration {

    private String basePackageName;

    private String objectTypePackageName;

    private String interfaceTypePackageName;

    private String unionTypePackageName;

    private String enumTypePackageName;

    private String inputObjectTypePackageName;

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public String getObjectTypePackageName() {
        return objectTypePackageName != null ? objectTypePackageName : basePackageName + ".object";
    }

    public void setObjectTypePackageName(String objectTypePackageName) {
        this.objectTypePackageName = objectTypePackageName;
    }

    public String getInterfaceTypePackageName() {
        return interfaceTypePackageName != null ? interfaceTypePackageName : basePackageName + ".interface";
    }

    public void setInterfaceTypePackageName(String interfaceTypePackageName) {
        this.interfaceTypePackageName = interfaceTypePackageName;
    }

    public String getUnionTypePackageName() {
        return unionTypePackageName != null ? unionTypePackageName : basePackageName + ".union";
    }

    public void setUnionTypePackageName(String unionTypePackageName) {
        this.unionTypePackageName = unionTypePackageName;
    }

    public String getEnumTypePackageName() {
        return enumTypePackageName != null ? enumTypePackageName : basePackageName + ".enum";
    }

    public void setEnumTypePackageName(String enumTypePackageName) {
        this.enumTypePackageName = enumTypePackageName;
    }

    public String getInputObjectTypePackageName() {
        return inputObjectTypePackageName != null ? inputObjectTypePackageName : basePackageName + ".inputObject";
    }

    public void setInputObjectTypePackageName(String inputObjectTypePackageName) {
        this.inputObjectTypePackageName = inputObjectTypePackageName;
    }
}
