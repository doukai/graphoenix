package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.annotation.UserExpression;
import io.graphoenix.showcase.mysql.generated.annotation.UserExpressions;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import reactor.core.publisher.Mono;

@GraphQLApi
public interface OperationTest {

    @Query
//    @UserExpressions({
//            @UserExpression(name = "name"),
//            @UserExpression(sex = "sex")
//    })
    @UserExpression(opr = Operator.NEQ, sex = "sex", name = "name")
    User queryUser(String name, Sex sex);

    @Query
    @UserExpressions({
            @UserExpression(name = "name"),
            @UserExpression(sex = "sex")
    })
    Mono<User> queryUserAsync();

    @Mutation
    User mutationUser();

    @Mutation
    Mono<User> mutationUserAsync();

}
