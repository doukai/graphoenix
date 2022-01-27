package io.graphoenix.graphql.generator.document;

import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.LinkedHashSet;
import java.util.Set;

public class Document {

    private Schema schema;

    private Set<String> definitions;

    public Set<String> getDefinitions() {
        return definitions;
    }

    public Document setDefinitions(Set<String> definitions) {
        this.definitions = definitions;
        return this;
    }

    public Document addDefinition(String definition) {
        if (this.definitions == null) {
            this.definitions = new LinkedHashSet<>();
        }
        this.definitions.add(definition);
        return this;
    }

    public Document addDefinitions(Set<String> definitions) {
        if (this.definitions == null) {
            this.definitions = definitions;
        } else {
            this.definitions.addAll(definitions);
        }
        return this;
    }

    public String toString(IGraphQLDocumentManager manager) {

        return null;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/Document.stg");
        ST st = stGroupFile.getInstanceOf("documentDefinition");
        st.add("document", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
