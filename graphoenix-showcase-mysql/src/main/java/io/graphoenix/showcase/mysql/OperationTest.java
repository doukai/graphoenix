package io.graphoenix.showcase.mysql;

import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.SQLToFileConvertHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.showcase.mysql.generated.annotation.*;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

@GraphQLOperation(
        bootstrapHandlers = DocumentBuildHandler.class,
        pretreatmentHandlers = {OperationToSQLConvertHandler.class, SQLToFileConvertHandler.class},
        executeHandlers = OperationSQLExecuteHandler.class
)
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression(opr = Operator.NEQ, name = "name")
    User queryUser(String name, Sex sex);

    @QueryOperation("userList")
    @UserExpressions(
            value = {
                    @UserExpression(opr = Operator.NEQ, name = {"dk1", "dk2"})
            },
            roles = {
                    @RoleExpression(opr = Operator.NEQ, name = "role1"),
            }
    )
    Mono<User> queryUserAsync(String name, Sex sex, Sex sex2, int version);

    @MutationOperation("user")
    @UserInput(name = "name1",  age = 11, login = "login1", password = "password1")
    User mutationUser(Sex sex, String name);

}
