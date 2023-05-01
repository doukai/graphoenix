package io.graphoenix.core.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;

public class Document {

    private Collection<Object> definitions;

    public Collection<Object> getDefinitions() {
        return definitions;
    }

    public Document setDefinitions(Collection<Object> definitions) {
        this.definitions = definitions;
        return this;
    }

    public Document addDefinition(Object definition) {
        if (this.definitions == null) {
            this.definitions = new LinkedHashSet<>();
        }
        this.definitions.add(definition);
        return this;
    }

    public Document addDefinitions(Collection<Object> definitions) {
        if (this.definitions == null) {
            this.definitions = definitions;
        }
        this.definitions.addAll(definitions);
        return this;
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
