package io.graphoenix.common.error;

import io.graphoenix.spi.dto.GraphQLError;
import io.graphoenix.spi.dto.GraphQLLocation;
import io.graphoenix.spi.dto.GraphQLPath;
import io.graphoenix.spi.error.GraphQLErrorType;
import org.zalando.problem.AbstractThrowableProblem;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.zalando.problem.Status.BAD_REQUEST;

@Immutable
public class GraphQLProblem extends AbstractThrowableProblem {

    static final URI TYPE = URI.create("https://spec.graphql.org");

    private final List<GraphQLError> errors;

    public GraphQLProblem() {
        super(TYPE, "graphQL errors", BAD_REQUEST);
        this.errors = new ArrayList<>();
        super.getParameters().put("errors", this.errors);
    }

    public GraphQLProblem pushError(GraphQLError error) {
        this.errors.add(error);
        return this;
    }

    public GraphQLProblem pushError(GraphQLErrorType graphQLErrorType, List<GraphQLLocation> locations, GraphQLPath path) {
        GraphQLError error = new GraphQLError();
        error.setMessage(graphQLErrorType.toString());
        error.setLocations(locations);
        error.setPath(path);
        this.errors.add(error);
        return this;
    }

    public GraphQLProblem pushError(GraphQLErrorType graphQLErrorType, List<GraphQLLocation> locations) {
        return pushError(graphQLErrorType, locations);
    }

    public GraphQLProblem pushError(GraphQLErrorType graphQLErrorType) {
        return pushError(graphQLErrorType, null);
    }
}
