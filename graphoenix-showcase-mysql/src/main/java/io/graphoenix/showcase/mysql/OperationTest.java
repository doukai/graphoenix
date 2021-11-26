package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.annotation.*;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

@GraphQLOperation
public interface OperationTest {

//    @QueryOperation(value = "userList", layers = 1)
//    @UserExpressions({
//            @UserExpression(name = "name"),
//            @UserExpression(sex = "sex")
//    })
//    @UserExpression(opr = Operator.NEQ, $name = "name")
//    User queryUser(String name, Sex sex);
//
//    @QueryOperation("userList")
//    @UserExpressions(
//            value = {
//                    @UserExpression(name = {"dk1", "dk2"}),
//                    @UserExpression(opr = Operator.NEQ, $sex = {"sex", "sex2"})
//            },
//            roles = {
//                    @RoleExpression(name = "role1"),
//                    @RoleExpression($version = "version")
//            }
//    )
//    Mono<User> queryUserAsync(String name, Sex sex, Sex sex2, int version);

//    @MutationOperation("user")
//    @UserInput($name = {"dk1","dk3"}, $sex = "sex")
//    User mutationUser(Sex sex,String dk1,String dk3);

    @MutationOperation("user")
    @UserInputs(
            value = {
                    @UserInput($name = {"dk1", "dk3"}, $sex = "sex"),
                    @UserInput(name = "dk2", $sex = "sex2")
            },
            organization = @OrganizationInput(name = "org1", $version = "orgVersion"),
            roles = {
                    @RoleInput(name = "role1"),
                    @RoleInput($name = "name")
            })
    Mono<User> mutationUserAsync(String name, Sex sex, Sex sex2, int orgVersion, String dk1, String dk3);

}
