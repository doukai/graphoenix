package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedAnnotationTypes("dagger.internal.DaggerGenerated")
@AutoService(Processor.class)
public class DaggerGeneratedAnnotationProcessor extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                ServiceLoader<DaggerExpansionProcessor> expansionProcessors = ServiceLoader.load(DaggerExpansionProcessor.class, DaggerGeneratedAnnotationProcessor.class.getClassLoader());
                expansionProcessors.forEach(daggerExpansionProcessor -> daggerExpansionProcessor.process(trees.getPath(bundleClassElement).getCompilationUnit().toString(), this.processingEnv.getFiler()));
            }
        }
        return false;
    }
}
