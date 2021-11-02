package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;

public class EnumType {

    private final STGroup stGroupFile = new STGroupFile("stg/document/EnumType.stg");

    private String name;
    private List<String> directives;
    private List<EnumValue> enumValues;
    private String description;

    public String getName() {
        return name;
    }

    public EnumType setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public EnumType setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public List<EnumValue> getEnumValues() {
        return enumValues;
    }

    public EnumType setEnumValues(List<EnumValue> enumValues) {
        this.enumValues = enumValues;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EnumType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("enumTypeDefinition");
        st.add("enumType", this);
        return st.render();
    }
}
