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
        ST st = new STGroupFile("stg/document/Schema.stg").getInstanceOf("schemaDefinition");
        st.add("schema", this);
        return st.render();
    }
}
