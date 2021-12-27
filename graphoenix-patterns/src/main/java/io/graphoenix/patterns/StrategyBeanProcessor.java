package io.graphoenix.patterns;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.auto.service.AutoService;
import io.graphoenix.dagger.DaggerProxyProcessor;
import io.graphoenix.spi.patterns.StrategyBean;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@AutoService(DaggerProxyProcessor.class)
public class StrategyBeanProcessor implements DaggerProxyProcessor {

    private BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType;
    private BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource;

    @Override
    public void init(BiFunction<CompilationUnit, ClassOrInterfaceType, Optional<CompilationUnit>> getCompilationUnitByClassOrInterfaceType, BiConsumer<CompilationUnit, CompilationUnit> importAllTypesFromSource) {
        this.getCompilationUnitByClassOrInterfaceType = getCompilationUnitByClassOrInterfaceType;
        this.importAllTypesFromSource = importAllTypesFromSource;
    }

    @Override
    public void buildComponentProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, CompilationUnit componentCompilationUnit, ClassOrInterfaceDeclaration componentClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration) {

    }

    @Override
    public Optional<CompilationUnit> createComponentProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration) {

        if (moduleBodyDeclaration.isMethodDeclaration()) {
            MethodDeclaration originalMethodDeclaration = moduleBodyDeclaration.asMethodDeclaration();

            ClassOrInterfaceDeclaration strategyImplDeclaration = new ClassOrInterfaceDeclaration()
                    .addModifier(Modifier.Keyword.PUBLIC)
                    .removeModifier(Modifier.Keyword.ABSTRACT)
                    .setName(originalMethodDeclaration.getType().asClassOrInterfaceType().getNameAsString() + "Impl");

            CompilationUnit strategyDefineCompilationUnit = this.getCompilationUnitByClassOrInterfaceType.apply(moduleCompilationUnit, originalMethodDeclaration.getType().asClassOrInterfaceType()).orElseThrow();
            CompilationUnit strategyImplCompilationUnit = new CompilationUnit().addType(strategyImplDeclaration);
            strategyDefineCompilationUnit.getPackageDeclaration().ifPresent(strategyImplCompilationUnit::setPackageDeclaration);



        }

        return Optional.empty();
    }

    @Override
    public void prepareModuleProxy(BodyDeclaration<?> moduleBodyDeclaration, CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

    }

    @Override
    public void buildModuleProxy(CompilationUnit moduleCompilationUnit, ClassOrInterfaceDeclaration moduleClassDeclaration, List<CompilationUnit> componentProxyCompilationUnits, CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration) {

    }

    @Override
    public void buildComponentProxyComponent(CompilationUnit moduleProxyCompilationUnit, ClassOrInterfaceDeclaration moduleProxyClassDeclaration, CompilationUnit componentProxyCompilationUnit, ClassOrInterfaceDeclaration componentProxyClassDeclaration, CompilationUnit componentProxyComponentCompilationUnit, ClassOrInterfaceDeclaration componentProxyComponentInterfaceDeclaration) {

    }

    @Override
    public Class<? extends Annotation> support() {
        return StrategyBean.class;
    }
}
