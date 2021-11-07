package io.graphoenix.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.graphoenix.common.error.GraphQLProblem;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import java.net.URI;

import static io.graphoenix.spi.error.GraphQLErrorType.OPERATION_NOT_EXIST;
import static org.zalando.problem.Status.BAD_REQUEST;

public class Test {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new ProblemAdapterFactory()
                    .registerSubtype(GraphQLProblem.TYPE, GraphQLProblem.class))
            .create();

    @org.junit.jupiter.api.Test
    void test1() {

//        Gson gson = new Gson();

        System.out.println(gson.toJson(new GraphQLProblem(OPERATION_NOT_EXIST)));


//        System.out.println(new GraphQLProblem(OPERATION_NOT_EXIST));
    }
}
