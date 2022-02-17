package io.graphoenix.core.error;

import com.google.gson.GsonBuilder;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import javax.annotation.concurrent.Immutable;
import java.net.URI;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Immutable
public class ElementProblem extends AbstractThrowableProblem {

    public static final String TYPE_VALUE = "https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Element.html";
    public static final URI TYPE = URI.create(TYPE_VALUE);

    private final String message;

    public ElementProblem(ElementErrorType elementErrorType) {
        super(TYPE, "element errors", INTERNAL_SERVER_ERROR);
        this.message = elementErrorType.toString();
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
