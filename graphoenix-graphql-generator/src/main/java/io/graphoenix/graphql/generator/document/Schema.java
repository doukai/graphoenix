package io.graphoenix.graphql.generator.document;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class Schema {

    private final STGroup stGroupFile = new STGroupFile("stg/document/Schema.stg");

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
        ST st = stGroupFile.getInstanceOf("schemaDefinition");
        st.add("schema", this);
        return st.render();
    }
}
