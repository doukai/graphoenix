package graphoenix.annotation.processor;

import javax.annotation.processing.Filer;

public interface DaggerExpansionProcessor {

    String process(String source, Filer filer);
}
