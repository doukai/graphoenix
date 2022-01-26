package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

public class Schema {

    private String query;
    private String mutation;

    public String getQuery() {
        return query;
    }

    public Schema setQuery(String query) {
        this.query = query;
        return this;
    }

    public String getMutation() {
        return mutation;
    }

    public Schema setMutation(String mutation) {
        this.mutation = mutation;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/Schema.stg");
        ST st = stGroupFile.getInstanceOf("schemaDefinition");
        st.add("schema", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
