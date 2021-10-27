package io.graphoenix.http.server;

import io.graphoenix.common.handler.GraphQLOperationPipeline;
import io.graphoenix.spi.handler.IGraphQLToSQLHandler;
import io.graphoenix.spi.handler.ISQLHandler;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;

import static io.graphoenix.common.handler.GraphQLOperationPipelineBootstrap.GRAPHQL_OPERATION_PIPELINE_BOOTSTRAP;

public class HttpServerTest {

    @Test
    void serverTest() throws Exception {
        GraphQLOperationPipeline graphQLOperationPipeline = GRAPHQL_OPERATION_PIPELINE_BOOTSTRAP
                .startup()
                .push(IGraphQLToSQLHandler.class)
                .push(ISQLHandler.class);
        GraphqlHttpServer graphqlHttpServer = new GraphqlHttpServer(graphQLOperationPipeline);
        graphqlHttpServer.run();
    }

    @Test
    void name() {
        String currentDir = System.getProperty("user.dir");
        System.out.println(currentDir);

        //获取Javac编译器对象
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        //获取文件管理器：负责管理类文件的输入输出
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        //获取要被编译的Java源文件
        File file = new File(currentDir + "/project/test/TestHello.java");
        //通过源文件获取到要编译的Java类源码迭代器，包括所有内部类，其中每个类都是一个JavaFileObject
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
        //生成编译任务
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnits);
        //执行编译任务
        task.call();
    }
}
