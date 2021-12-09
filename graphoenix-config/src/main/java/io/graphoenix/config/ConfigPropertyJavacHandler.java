package io.graphoenix.config;

import com.sun.tools.javac.tree.JCTree;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ConfigPropertyJavacHandler extends JavacAnnotationHandler<ConfigProperty> {
    @Override
    public void handle(AnnotationValues<ConfigProperty> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {

    }
}
