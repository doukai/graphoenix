package io.graphoenix.spi.error;

import javax.lang.model.element.Element;
import java.util.List;

public class ElementError {

    private String message;

    private List<Element> elements;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }
}
