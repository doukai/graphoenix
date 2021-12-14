package graphoenix.annotation.processor;

import javax.annotation.processing.Filer;

public interface DaggerExpansionProcessor {

    void process(String source, Filer filer);
}
