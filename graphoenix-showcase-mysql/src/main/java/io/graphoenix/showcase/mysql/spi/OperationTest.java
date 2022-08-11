package io.graphoenix.showcase.mysql.spi;

import io.graphoenix.r2dbc.connector.dao.R2DBCOperationDAO;
import io.graphoenix.showcase.mysql.dto.annotation.*;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.enumType.Sort;
import io.graphoenix.showcase.mysql.dto.inputObjectType.OrganizationInput;
import io.graphoenix.showcase.mysql.dto.inputObjectType.RoleInput;
import io.graphoenix.showcase.mysql.dto.inputObjectType.UserInput;
import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

import java.util.List;

@GraphQLOperation(operationDAO = R2DBCOperationDAO.class)
public interface OperationTest {

    @QueryOperation(value = "user", layers = 1)
    @UserExpression0(name = @StringExpression(val = "name"))
    User queryUser(String name) throws Exception;

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression0(roles = @RoleExpression1(name = @StringExpression(val = "role1111"), id = @IDExpression(val = "role1112")))
    List<User> queryUserList(Sex sex) throws Exception;

    @QueryOperation("user")
    @UserExpression0(
            name = @StringExpression($val = "name"),
            roles = @RoleExpression1(name = @StringExpression(val = "role1111"), id = @IDExpression($val = "name")),
            orderBy = @UserOrderBy0(age = Sort.DESC),
            $offset = "offset",
            first = 20,
            groupBy = {"name", "id"}
    )
    PublisherBuilder<User> queryUserAsync(String name, Sex sex, String roleName, int offset) throws Exception;

    @QueryOperation("userList")
    @UserExpression0(
            name = @StringExpression($val = "name"),
            sex = @SexExpression($val = "sex")
    )
    PublisherBuilder<List<User>> queryUserListAsync(String name, Sex sex, String roleName) throws Exception;

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

    @MutationOperation("userList")
    @UserInput0(
            $name = "name",
            $sex = "sex",
            login = "login1",
            password = "password1",
            $organization = "organization",
            $roles = "roles",
            $phones = "phones",
            $test1 = "test1",
            $test2 = "test2",
            $list = "list"
    )
    List<User> mutationUserList(List<UserInput> list, Sex sex, String name, String orgName, List<RoleInput> roles, OrganizationInput organization, List<String> phones, List<Integer> test1, List<Boolean> test2) throws Exception;
}
