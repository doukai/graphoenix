package io.graphoenix.showcase.mysql;

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

    @QueryOperation("userList")
//    @UserExpressions({
//            @UserExpression(name = "name"),
//            @UserExpression(sex = "sex")
//    })
    @UserExpression(opr = Operator.NEQ, sex = "sex")
    User queryUser(String name, Sex sex);

    @QueryOperation("userList")
    @UserExpressions({
            @UserExpression(name = "name"),
            @UserExpression(sex = "sex")
    })
    Mono<User> queryUserAsync(String name, Sex sex);

    @MutationOperation("userList")
    User mutationUser();

    @MutationOperation("userList")
    Mono<User> mutationUserAsync();

}
