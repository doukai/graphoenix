package io.graphoenix.core.error;

import com.google.gson.GsonBuilder;
import io.graphoenix.spi.error.ElementError;
import io.graphoenix.spi.error.ElementErrorType;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Element;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Immutable
public class ElementProblem extends AbstractThrowableProblem {

    public static final String TYPE_VALUE = "https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Element.html";
    public static final URI TYPE = URI.create(TYPE_VALUE);

    private final List<ElementError> errors;

    public ElementProblem() {
        super(TYPE, "element errors", INTERNAL_SERVER_ERROR);
        this.errors = new ArrayList<>();
    }

    public ElementProblem(ElementErrorType elementErrorType) {
        super(TYPE, "element errors", INTERNAL_SERVER_ERROR);
        this.errors = new ArrayList<>();
        this.push(elementErrorType);
    }

    public ElementProblem push(ElementErrorType elementErrorType) {
        ElementError error = new ElementError();
        error.setMessage(elementErrorType.toString());
        this.errors.add(error);
        return this;
    }

    public ElementProblem push(ElementErrorType elementErrorType, Element element) {
        ElementError error = new ElementError();
        error.setMessage(elementErrorType.toString());
        error.setElements(Collections.singletonList(element));
        this.errors.add(error);
        return this;
    }

    public ElementProblem push(ElementErrorType elementErrorType, List<Element> elements) {
        ElementError error = new ElementError();
        error.setMessage(elementErrorType.toString());
        error.setElements(elements);
        this.errors.add(error);
        return this;
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
