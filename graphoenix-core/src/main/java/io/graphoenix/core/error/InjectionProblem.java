package io.graphoenix.core.error;

import com.google.gson.GsonBuilder;
import io.graphoenix.spi.error.InjectionError;
import io.graphoenix.spi.error.InjectionErrorType;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.gson.ProblemAdapterFactory;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Immutable
public class InjectionProblem extends AbstractThrowableProblem {

    public static final String TYPE_VALUE = "https://jakarta.ee/specifications/dependency-injection";
    public static final URI TYPE = URI.create(TYPE_VALUE);

    private final List<InjectionError> errors;

    public InjectionProblem() {
        super(TYPE, "injection errors", INTERNAL_SERVER_ERROR);
        this.errors = new ArrayList<>();
    }

    public InjectionProblem(InjectionErrorType injectionErrorType) {
        super(TYPE, "injection errors", INTERNAL_SERVER_ERROR);
        this.errors = new ArrayList<>();
        this.push(injectionErrorType);
    }

    public InjectionProblem push(InjectionErrorType injectionErrorType) {
        return this.push(injectionErrorType, null, null);
    }

    public InjectionProblem push(InjectionErrorType injectionErrorType, Class<?> beanClass, String beanName) {
        InjectionError error = new InjectionError();
        error.setMessage(injectionErrorType.toString());
        error.setBeanClass(beanClass);
        error.setBeanName(beanName);
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
