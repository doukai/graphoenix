package io.graphoenix.inject.error;

import com.google.gson.GsonBuilder;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import javax.annotation.concurrent.Immutable;
import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Immutable
public class InjectionProblem extends AbstractThrowableProblem {

    public static final String TYPE_VALUE = "https://jakarta.ee/specifications/dependency-injection";
    public static final URI TYPE = URI.create(TYPE_VALUE);

    private final String message;

    public InjectionProblem(InjectionErrorType injectionErrorType) {
        super(TYPE, "injection errors", INTERNAL_SERVER_ERROR);
        this.message = injectionErrorType.toString();
    }

    @Override
    public String toString() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new ProblemAdapterFactory().registerSubtype(TYPE, this.getClass()))
                .setPrettyPrinting()
                .create()
                .toJson(this);
    }
}
