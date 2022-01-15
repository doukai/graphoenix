package io.graphoenix.showcase.mysql.generated;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.showcase.mysql.ApiTest;
import io.graphoenix.showcase.mysql.generated.objectType.Organization;
import io.graphoenix.showcase.mysql.generated.objectType.Role;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.handler.BaseInvokeHandler;

import java.util.function.Function;

public class InvokeTest extends BaseInvokeHandler {

    private final ApiTest apiTest;

    public InvokeTest() {
        this.apiTest = BeanContext.get(ApiTest.class);
        Function<User, User> user = this::user;
        put(User.class, user);
        Function<Role, Role> role = this::role;
        put(Role.class, role);
        Function<Organization, Organization> organization = this::organization;
        put(Organization.class, organization);
    }

    public User user(User user) {
        if (user != null) {
            user.setLocation(apiTest.getUserLocation(user));

            if (user.getRoles() != null) {
                user.getRoles().forEach(this::role);
            }

            organization(user.getOrganization());
        }
        return user;
    }

    public Role role(Role role) {
        if (role != null) {
            role.setRoleMark(apiTest.getRoleMark(role));

            if (role.getUsers() != null) {
                role.getUsers().forEach(this::user);
            }
        }
        return role;
    }

    public Organization organization(Organization organization) {
        if (organization != null) {
            organization.setRoleMark(apiTest.getOrganizationMark(organization));

            if (organization.getUsers() != null) {
                organization.getUsers().forEach(this::user);
            }

            organization(organization.getAbove());
        }
        return organization;
    }
}
