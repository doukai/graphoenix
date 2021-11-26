package io.graphoenix.showcase.mysql;

import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.showcase.mysql.generated.annotation.*;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

@GraphQLOperation(
        bootstrapHandlers = {DocumentBuildHandler.class}
)
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
    @UserExpressions({
            @UserExpression(name = "name"),
            @UserExpression($sex = "sex")
    })
    @UserExpression(opr = Operator.NEQ, $name = "name")
    User queryUser(String name, Sex sex);

    @QueryOperation("userList")
    @UserExpressions(
            value = {
                    @UserExpression(name = {"dk1", "dk2"}),
                    @UserExpression(opr = Operator.NEQ, $sex = {"sex", "sex2"})
            },
            roles = {
                    @RoleExpression(name = "role1"),
                    @RoleExpression($version = "version")
            }
    )
    Mono<User> queryUserAsync(String name, Sex sex, Sex sex2, int version);

    @MutationOperation("user")
    @UserInput($name = "name", $sex = "sex", age = 11)
    User mutationUser(Sex sex, String name);

}
