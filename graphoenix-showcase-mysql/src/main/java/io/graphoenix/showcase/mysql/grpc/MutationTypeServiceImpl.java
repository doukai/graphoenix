package io.graphoenix.showcase.mysql.grpc;

import io.graphoenix.showcase.mysql.MutationCreateGroupIdRequest;
import io.graphoenix.showcase.mysql.MutationCreateGroupIdResponse;
import io.graphoenix.showcase.mysql.MutationCreateTimeRequest;
import io.graphoenix.showcase.mysql.MutationCreateTimeResponse;
import io.graphoenix.showcase.mysql.MutationCreateUserIdRequest;
import io.graphoenix.showcase.mysql.MutationCreateUserIdResponse;
import io.graphoenix.showcase.mysql.MutationIntroDirectiveLocationsRequest;
import io.graphoenix.showcase.mysql.MutationIntroDirectiveLocationsResponse;
import io.graphoenix.showcase.mysql.MutationIntroDirectiveRequest;
import io.graphoenix.showcase.mysql.MutationIntroDirectiveResponse;
import io.graphoenix.showcase.mysql.MutationIntroEnumValueRequest;
import io.graphoenix.showcase.mysql.MutationIntroEnumValueResponse;
import io.graphoenix.showcase.mysql.MutationIntroFieldRequest;
import io.graphoenix.showcase.mysql.MutationIntroFieldResponse;
import io.graphoenix.showcase.mysql.MutationIntroInputValueRequest;
import io.graphoenix.showcase.mysql.MutationIntroInputValueResponse;
import io.graphoenix.showcase.mysql.MutationIntroSchemaRequest;
import io.graphoenix.showcase.mysql.MutationIntroSchemaResponse;
import io.graphoenix.showcase.mysql.MutationIntroTypeInterfacesRequest;
import io.graphoenix.showcase.mysql.MutationIntroTypeInterfacesResponse;
import io.graphoenix.showcase.mysql.MutationIntroTypePossibleTypesRequest;
import io.graphoenix.showcase.mysql.MutationIntroTypePossibleTypesResponse;
import io.graphoenix.showcase.mysql.MutationIntroTypeRequest;
import io.graphoenix.showcase.mysql.MutationIntroTypeResponse;
import io.graphoenix.showcase.mysql.MutationIsDeprecatedRequest;
import io.graphoenix.showcase.mysql.MutationIsDeprecatedResponse;
import io.graphoenix.showcase.mysql.MutationOrganizationRequest;
import io.graphoenix.showcase.mysql.MutationOrganizationResponse;
import io.graphoenix.showcase.mysql.MutationRealmIdRequest;
import io.graphoenix.showcase.mysql.MutationRealmIdResponse;
import io.graphoenix.showcase.mysql.MutationRoleRequest;
import io.graphoenix.showcase.mysql.MutationRoleResponse;
import io.graphoenix.showcase.mysql.MutationRoleRoleTypeRequest;
import io.graphoenix.showcase.mysql.MutationRoleRoleTypeResponse;
import io.graphoenix.showcase.mysql.MutationTypeServiceGrpc;
import io.graphoenix.showcase.mysql.MutationUpdateTimeRequest;
import io.graphoenix.showcase.mysql.MutationUpdateTimeResponse;
import io.graphoenix.showcase.mysql.MutationUpdateUserIdRequest;
import io.graphoenix.showcase.mysql.MutationUpdateUserIdResponse;
import io.graphoenix.showcase.mysql.MutationUserPhonesRequest;
import io.graphoenix.showcase.mysql.MutationUserPhonesResponse;
import io.graphoenix.showcase.mysql.MutationUserRequest;
import io.graphoenix.showcase.mysql.MutationUserResponse;
import io.graphoenix.showcase.mysql.MutationUserRoleRequest;
import io.graphoenix.showcase.mysql.MutationUserRoleResponse;
import io.graphoenix.showcase.mysql.MutationUserTest1Request;
import io.graphoenix.showcase.mysql.MutationUserTest1Response;
import io.graphoenix.showcase.mysql.MutationUserTest2Request;
import io.graphoenix.showcase.mysql.MutationUserTest2Response;
import io.graphoenix.showcase.mysql.MutationVersionRequest;
import io.graphoenix.showcase.mysql.MutationVersionResponse;
import io.graphoenix.showcase.mysql.QueryIntroSchemaListRequest;
import io.grpc.stub.StreamObserver;

public class MutationTypeServiceImpl extends MutationTypeServiceGrpc.MutationTypeServiceImplBase {

    private static String queryTemplate = "query {%s %s %s}";

    public void test(QueryIntroSchemaListRequest request){

    }

    @Override
    public void introSchema(MutationIntroSchemaRequest request, StreamObserver<MutationIntroSchemaResponse> responseObserver) {


        super.introSchema(request, responseObserver);
    }

//    private String userInput(UserInput userInput) {
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("{");
//        if (userInput.hasName()) {
//            stringBuilder.append("name: ").append(userInput.getName().getValue()).append(" ");
//        }
//        if (userInput.hasAge()) {
//            stringBuilder.append("age: ").append(userInput.getAge().getValue()).append(" ");
//        }
//        if (userInput.getRolesCount() > 0) {
//            stringBuilder.append("roles: [").append(userInput.getRolesList().stream().map(this::roleInput).collect(Collectors.joining(", "))).append("] ");
//        }
//        if(userInput.hasOrganization()){
//            stringBuilder.append("organization: ").append(organizationInput(userInput.getOrganization())).append(" ");
//        }
//        stringBuilder.append("}");
//        return stringBuilder.toString();
//    }
//
//    private String roleInput(RoleInput roleInput) {
//        roleInput.getTypeList().stream().m
//        roleInput.getType(0).getValueDescriptor().getFullName()
//
//    }
//
//    private String organizationInput(OrganizationInput organizationInput) {
//
//    }

    @Override
    public void introType(MutationIntroTypeRequest request, StreamObserver<MutationIntroTypeResponse> responseObserver) {
        super.introType(request, responseObserver);
    }

    @Override
    public void introTypeInterfaces(MutationIntroTypeInterfacesRequest request, StreamObserver<MutationIntroTypeInterfacesResponse> responseObserver) {
        super.introTypeInterfaces(request, responseObserver);
    }

    @Override
    public void introTypePossibleTypes(MutationIntroTypePossibleTypesRequest request, StreamObserver<MutationIntroTypePossibleTypesResponse> responseObserver) {
        super.introTypePossibleTypes(request, responseObserver);
    }

    @Override
    public void introField(MutationIntroFieldRequest request, StreamObserver<MutationIntroFieldResponse> responseObserver) {
        super.introField(request, responseObserver);
    }

    @Override
    public void introInputValue(MutationIntroInputValueRequest request, StreamObserver<MutationIntroInputValueResponse> responseObserver) {
        super.introInputValue(request, responseObserver);
    }

    @Override
    public void introEnumValue(MutationIntroEnumValueRequest request, StreamObserver<MutationIntroEnumValueResponse> responseObserver) {
        super.introEnumValue(request, responseObserver);
    }

    @Override
    public void introDirective(MutationIntroDirectiveRequest request, StreamObserver<MutationIntroDirectiveResponse> responseObserver) {
        super.introDirective(request, responseObserver);
    }

    @Override
    public void introDirectiveLocations(MutationIntroDirectiveLocationsRequest request, StreamObserver<MutationIntroDirectiveLocationsResponse> responseObserver) {
        super.introDirectiveLocations(request, responseObserver);
    }

    @Override
    public void user(MutationUserRequest request, StreamObserver<MutationUserResponse> responseObserver) {
        super.user(request, responseObserver);
    }

    @Override
    public void userPhones(MutationUserPhonesRequest request, StreamObserver<MutationUserPhonesResponse> responseObserver) {
        super.userPhones(request, responseObserver);
    }

    @Override
    public void userTest1(MutationUserTest1Request request, StreamObserver<MutationUserTest1Response> responseObserver) {
        super.userTest1(request, responseObserver);
    }

    @Override
    public void userTest2(MutationUserTest2Request request, StreamObserver<MutationUserTest2Response> responseObserver) {
        super.userTest2(request, responseObserver);
    }

    @Override
    public void userRole(MutationUserRoleRequest request, StreamObserver<MutationUserRoleResponse> responseObserver) {
        super.userRole(request, responseObserver);
    }

    @Override
    public void role(MutationRoleRequest request, StreamObserver<MutationRoleResponse> responseObserver) {
        super.role(request, responseObserver);
    }

    @Override
    public void roleRoleType(MutationRoleRoleTypeRequest request, StreamObserver<MutationRoleRoleTypeResponse> responseObserver) {
        super.roleRoleType(request, responseObserver);
    }

    @Override
    public void organization(MutationOrganizationRequest request, StreamObserver<MutationOrganizationResponse> responseObserver) {
        super.organization(request, responseObserver);
    }

    @Override
    public void isDeprecated(MutationIsDeprecatedRequest request, StreamObserver<MutationIsDeprecatedResponse> responseObserver) {
        super.isDeprecated(request, responseObserver);
    }

    @Override
    public void version(MutationVersionRequest request, StreamObserver<MutationVersionResponse> responseObserver) {
        super.version(request, responseObserver);
    }

    @Override
    public void realmId(MutationRealmIdRequest request, StreamObserver<MutationRealmIdResponse> responseObserver) {
        super.realmId(request, responseObserver);
    }

    @Override
    public void createUserId(MutationCreateUserIdRequest request, StreamObserver<MutationCreateUserIdResponse> responseObserver) {
        super.createUserId(request, responseObserver);
    }

    @Override
    public void createTime(MutationCreateTimeRequest request, StreamObserver<MutationCreateTimeResponse> responseObserver) {
        super.createTime(request, responseObserver);
    }

    @Override
    public void updateUserId(MutationUpdateUserIdRequest request, StreamObserver<MutationUpdateUserIdResponse> responseObserver) {
        super.updateUserId(request, responseObserver);
    }

    @Override
    public void updateTime(MutationUpdateTimeRequest request, StreamObserver<MutationUpdateTimeResponse> responseObserver) {
        super.updateTime(request, responseObserver);
    }

    @Override
    public void createGroupId(MutationCreateGroupIdRequest request, StreamObserver<MutationCreateGroupIdResponse> responseObserver) {
        super.createGroupId(request, responseObserver);
    }
}
