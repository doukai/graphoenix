package io.graphoenix.dagger;

import com.github.javaparser.ast.CompilationUnit;

import javax.annotation.processing.Filer;

public interface DaggerExpansionProcessor {

    void process(CompilationUnit compilationUnit, Filer filer);
}
