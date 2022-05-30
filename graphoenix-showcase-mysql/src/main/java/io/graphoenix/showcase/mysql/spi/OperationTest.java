package io.graphoenix.showcase.mysql.spi;

import io.graphoenix.r2dbc.connector.dao.R2DBCOperationDAO;
import io.graphoenix.showcase.mysql.dto.annotation.RoleExpression1;
import io.graphoenix.showcase.mysql.dto.annotation.RoleExpressions1;
import io.graphoenix.showcase.mysql.dto.annotation.UserExpression0;
import io.graphoenix.showcase.mysql.dto.annotation.UserExpressions0;
import io.graphoenix.showcase.mysql.dto.annotation.UserInput0;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.inputObjectType.OrganizationInput;
import io.graphoenix.showcase.mysql.dto.inputObjectType.RoleInput;
import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

import java.util.List;

@GraphQLOperation(operationDAO = R2DBCOperationDAO.class)
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression0($name = "name")
    User queryUser(String name) throws Exception;

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression0(roles = {
            @RoleExpressions1({
                    @RoleExpression1(name = "role1111"),
                    @RoleExpression1(id = "role1112")
            }),
    })
    List<User> queryUserList(Sex sex) throws Exception;

    @QueryOperation("userList")
    @UserExpressions0({
            @UserExpression0($name = "name"),
            @UserExpression0(roles = {
                    @RoleExpressions1({
                            @RoleExpression1(name = "role1111"),
                            @RoleExpression1(id = "role1112")
                    }),
            })
    })
    Mono<User> queryUserAsync(String name, Sex sex, String roleName) throws Exception;

    @QueryOperation("userList")
    @UserExpressions0({
            @UserExpression0($name = "name"),
            @UserExpression0($sex = "sex")
    })
    Mono<List<User>> queryUserListAsync(String name, Sex sex, String roleName) throws Exception;

    @MutationOperation("user")
    @UserInput0(
            $name = "name",
            $sex = "sex",
            login = "login1",
            password = "password1",
            $organization = "organization",
            $roles = "roles",
            $phones = "phones",
            $test1 = "test1",
            $test2 = "test2"
    )
    User mutationUser(Sex sex, String name, String orgName, List<RoleInput> roles, OrganizationInput organization, List<String> phones, List<Integer> test1, List<Boolean> test2) throws Exception;
}
