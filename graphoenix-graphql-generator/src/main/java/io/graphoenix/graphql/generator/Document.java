package io.graphoenix.graphql.generator;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Document {

    private final STGroup stGroupFile = new STGroupFile("stg/graphql/Document.stg");

    private List<String> definitions;

    public List<String> getDefinitions() {
        return definitions;
    }

    public Document setDefinitions(List<String> definitions) {
        this.definitions = definitions;
        return this;
    }

    public Document addDefinition(String definition) {
        if (this.definitions == null) {
            this.definitions = new ArrayList<>();
        }
        this.definitions.add(definition);
        return this;
    }

    public Document addDefinitions(List<String> definitions) {
        if (this.definitions == null) {
            this.definitions = definitions;
        } else {
            this.definitions.addAll(definitions);
        }
        return this;
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("schemaDefinition");
        st.add("schema", this);
        return st.render();
    }
}
