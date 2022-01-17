package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.objectType.Organization;
import io.graphoenix.showcase.mysql.generated.objectType.Role;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.Set;

@GraphQLApi
public class ApiTest {

    @Query
    public String getUserLocation(@Source User user) {
        return user.getName();
    }

    @Query
    public String getRoleMark(@Source Role role) {
        return role.getName();
    }

    @Query
    public String getOrganizationMark(@Source Organization organization) {
        return organization.getName();
    }

    public Set<User> getOrganizationUsers(@Source Organization organization) {
        return organization.getUsers();
    }
}
