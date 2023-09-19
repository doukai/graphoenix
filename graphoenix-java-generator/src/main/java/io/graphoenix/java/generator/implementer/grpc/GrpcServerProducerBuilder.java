package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;

import static io.graphoenix.spi.constant.Hammurabi.MUTATION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;

@ApplicationScoped
public class GrpcServerProducerBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public GrpcServerProducerBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .stream()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(packageManager::isLocalPackage)
                .map(fieldDefinitionContext -> manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName))
                .distinct()
                .forEach(packageName -> {
                            try {
                                this.buildGrpcServerProducerClass(packageName).writeTo(filer);
                                Logger.info("{}.GrpcServerProducer build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );
    }

    private JavaFile buildGrpcServerProducerClass(String packageName) {
        TypeSpec typeSpec = buildGrpcServerBuilder(packageName);
        return JavaFile.builder(packageName + ".grpc", typeSpec).build();
    }

    private TypeSpec buildGrpcServerBuilder(String packageName) {
        String grpcPackageName = packageName + ".grpc";
        return TypeSpec.classBuilder("GrpcServerProducer")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addField(
                        FieldSpec.builder(
                                ClassName.get("io.graphoenix.grpc.server.config", "GrpcServerConfig"),
                                "grpcServerConfig",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Inject.class)
                                .addParameter(ClassName.get("io.graphoenix.grpc.server.config", "GrpcServerConfig"), "grpcServerConfig")
                                .addStatement("this.grpcServerConfig = grpcServerConfig")
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("server")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Produces.class)
                                .returns(Server.class)
                                .addStatement("return $T.forPort(grpcServerConfig.getPort()).addService(new $T()).addService(new $T()).addService(new $T()).addService(new $T()).addService(new $T()).addService(new $T()).build()",
                                        ClassName.get(ServerBuilder.class),
                                        ClassName.get(grpcPackageName, "Grpc" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImpl"),
                                        ClassName.get(grpcPackageName, "Grpc" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImpl"),
                                        ClassName.get(grpcPackageName, "GrpcGraphQLServiceImpl"),
                                        ClassName.get(grpcPackageName, "ReactorGrpc" + manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME) + "ServiceImpl"),
                                        ClassName.get(grpcPackageName, "ReactorGrpc" + manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME) + "ServiceImpl"),
                                        ClassName.get(grpcPackageName, "ReactorGrpcGraphQLServiceImpl")
                                )
                                .build()
                )
                .build();
    }
}
