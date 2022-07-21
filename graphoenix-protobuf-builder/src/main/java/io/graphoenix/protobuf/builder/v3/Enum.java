package io.graphoenix.protobuf.builder.v3;

import graphql.parser.antlr.GraphqlParser;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Enum {

    private String name;

    private List<EnumField> fields;

    public Enum() {
    }

    public Enum(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        this.name = enumTypeDefinitionContext.name().getText();
        this.fields = IntStream.range(0, enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().size() - 1)
                .mapToObj(index ->
                        new EnumField()
                                .setName(enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().get(index).enumValue().enumValueName().getText())
                                .setNumber(index)
                )
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public Enum setName(String name) {
        this.name = name;
        return this;
    }

    public List<EnumField> getFields() {
        return fields;
    }

    public Enum setFields(List<EnumField> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/v3/Enum.stg");
        ST st = stGroupFile.getInstanceOf("enumDefinition");
        st.add("enum", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
