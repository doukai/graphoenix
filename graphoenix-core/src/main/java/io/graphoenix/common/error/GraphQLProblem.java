package io.graphoenix.common.error;

import com.google.gson.GsonBuilder;
import io.graphoenix.spi.dto.GraphQLError;
import io.graphoenix.spi.dto.GraphQLLocation;
import io.graphoenix.spi.dto.GraphQLPath;
import io.graphoenix.spi.error.GraphQLErrorType;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.zalando.problem.Status.BAD_REQUEST;

@Immutable
public class GraphQLProblem extends AbstractThrowableProblem {

    public static final String TYPE_VALUE = "https://spec.graphql.org";
    public static final URI TYPE = URI.create(TYPE_VALUE);

    private final List<GraphQLError> errors;

    public GraphQLProblem() {
        super(TYPE, "graphQL errors", BAD_REQUEST);
        this.errors = new ArrayList<>();
    }

    public GraphQLProblem(GraphQLErrorType graphQLErrorType) {
        super(TYPE, "graphQL errors", BAD_REQUEST);
        this.errors = new ArrayList<>();
        this.push(graphQLErrorType);
    }

    public GraphQLProblem push(GraphQLErrorType graphQLErrorType) {
        return this.push(graphQLErrorType, null, null);
    }

    public GraphQLProblem push(GraphQLErrorType graphQLErrorType, List<GraphQLLocation> locations, GraphQLPath path) {
        GraphQLError error = new GraphQLError();
        error.setMessage(graphQLErrorType.toString());
        error.setLocations(locations);
        error.setPath(path);
        this.errors.add(error);
        return this;
    }

    @Override
    public String toString() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new ProblemAdapterFactory().registerSubtype(TYPE, this.getClass()))
                .setPrettyPrinting().create().toJson(this);
    }
}
