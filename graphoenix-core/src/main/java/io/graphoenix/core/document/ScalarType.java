package io.graphoenix.core.document;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.operation.Directive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

public class ScalarType {

    private String name;
    private Collection<Directive> directives;
    private String description;

    public ScalarType() {
    }

    public ScalarType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        this.name = scalarTypeDefinitionContext.name().getText();
        if (scalarTypeDefinitionContext.description() != null) {
            this.description = DOCUMENT_UTIL.getStringValue(scalarTypeDefinitionContext.description().StringValue());
        }
        if (scalarTypeDefinitionContext.directives() != null) {
            this.directives = scalarTypeDefinitionContext.directives().directive().stream().map(Directive::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public String getName() {
        return name;
    }

    public ScalarType setName(String name) {
        this.name = name;
        return this;
    }

    public Collection<Directive> getDirectives() {
        return directives;
    }

    public ScalarType setDirectives(Collection<Directive> directives) {
        if (directives != null) {
            this.directives = new LinkedHashSet<>(directives);
        }
        return this;
    }

    public ScalarType addDirectives(Collection<Directive> directives) {
        if (this.directives == null) {
            this.directives = new LinkedHashSet<>();
        }
        this.directives.addAll(directives);
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ScalarType setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        STGroupFile stGroupFile = new STGroupFile("stg/document/ScalarType.stg");
        ST st = stGroupFile.getInstanceOf("scalarTypeDefinition");
        st.add("scalarType", this);
        String render = st.render();
        stGroupFile.unload();
        return render;
    }
}
