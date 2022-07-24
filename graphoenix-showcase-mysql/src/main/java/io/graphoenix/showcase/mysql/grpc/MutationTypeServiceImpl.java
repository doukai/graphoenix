package io.graphoenix.showcase.mysql.grpc;

import io.graphoenix.showcase.mysql.*;
import io.grpc.stub.StreamObserver;

public class MutationTypeServiceImpl extends MutationTypeServiceGrpc.MutationTypeServiceImplBase {

    private static String queryTemplate = "query {%s %s %s}";

    @Override
    public void introSchema(MutationIntroSchemaRequest request, StreamObserver<MutationIntroSchemaResponse> responseObserver) {


        super.introSchema(request, responseObserver);
    }

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
