package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Set;

public class EnumType {

    private String name;
    private Set<String> directives;
    private Set<EnumValue> enumValues;
    private String description;

    public String getName() {
        return name;
    }

    public EnumType setName(String name) {
        this.name = name;
        return this;
    }

    public Set<String> getDirectives() {
        return directives;
    }

    public EnumType setDirectives(Set<String> directives) {
        this.directives = directives;
        return this;
    }

    public Set<EnumValue> getEnumValues() {
        return enumValues;
    }

    public EnumType setEnumValues(Set<EnumValue> enumValues) {
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
        STGroupFile stGroupFile = new STGroupFile("stg/document/EnumType.stg");
        ST st = stGroupFile.getInstanceOf("enumTypeDefinition");
        st.add("enumType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
