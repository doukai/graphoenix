package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.annotation.RoleExpression;
import io.graphoenix.showcase.mysql.generated.annotation.UserExpression;
import io.graphoenix.showcase.mysql.generated.annotation.UserExpressions;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

@GraphQLOperation
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
//    @UserExpressions({
//            @UserExpression(name = "name"),
//            @UserExpression(sex = "sex")
//    })
    @UserExpression(opr = Operator.NEQ, name = "test2")
    User queryUser(String name, Sex sex);

    @QueryOperation("userList")
    @UserExpressions(
            value = {
                    @UserExpression(name = "dk1"),
//                    @UserExpression(opr = Operator.NEQ, $sex = "sex")
            },
            roles = {
                    @RoleExpression(name = "role1"),
//                    @RoleExpression($version = "version")
            }
    )
    Mono<User> queryUserAsync(String name, Sex sex, int version);

    @MutationOperation("userList")
    User mutationUser();

    @MutationOperation("userList")
    Mono<User> mutationUserAsync();

}
