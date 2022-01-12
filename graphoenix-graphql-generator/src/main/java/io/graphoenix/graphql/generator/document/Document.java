package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

public class Document {

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
        ST st = new STGroupFile("stg/document/Document.stg").getInstanceOf("schemaDefinition");
        st.add("schema", this);
        return st.render();
    }
}
