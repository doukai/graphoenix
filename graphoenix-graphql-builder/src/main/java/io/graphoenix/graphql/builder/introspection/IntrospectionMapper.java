package io.graphoenix.graphql.builder.introspection;

import io.graphoenix.graphql.builder.introspection.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IntrospectionMapper {

    IntrospectionMapper INSTANCE = Mappers.getMapper(IntrospectionMapper.class);

    @Mapping(target = "hasMutationType", expression = "java( dto.getMutationType() != null )")
    @Mapping(target = "hasSubscriptionType", expression = "java( dto.getSubscriptionType() != null )")
    @Mapping(target = "hasDirectives", expression = "java( dto.getDirectives() != null && dto.getDirectives().size() > 0 )")
    __Schema schemaDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Schema dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "description", expression = "java( dto.getDescription() == null ? null : ('\"' + dto.getDescription() + '\"') )")
    @Mapping(target = "hasName", expression = "java( dto.getName() != null )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasFields", expression = "java( dto.getFields() != null && dto.getFields().size() > 0 )")
    @Mapping(target = "hasInterfaces", expression = "java( dto.getInterfaces() != null && dto.getInterfaces().size() > 0 )")
    @Mapping(target = "hasPossibleTypes", expression = "java( dto.getPossibleTypes() != null && dto.getPossibleTypes().size() > 0 )")
    @Mapping(target = "hasEnumValues", expression = "java( dto.getEnumValues() != null && dto.getEnumValues().size() > 0 )")
    @Mapping(target = "hasInputFields", expression = "java( dto.getInputFields() != null && dto.getInputFields().size() > 0 )")
    @Mapping(target = "hasOfType", expression = "java( dto.getOfType() != null )")
    __Type typeDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Type dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "description", expression = "java( dto.getDescription() == null ? null : ('\"' + dto.getDescription() + '\"') )")
    @Mapping(target = "deprecationReason", expression = "java( dto.getDeprecationReason() == null ? null : ('\"' + dto.getDeprecationReason() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasArgs", expression = "java( dto.getArgs() != null && dto.getArgs().size() > 0 )")
    @Mapping(target = "hasDeprecationReason", expression = "java( dto.getDeprecationReason() != null )")
    __Field fieldDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Field dto);


    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "description", expression = "java( dto.getDescription() == null ? null : ('\"' + dto.getDescription() + '\"') )")
    @Mapping(target = "deprecationReason", expression = "java( dto.getDeprecationReason() == null ? null : ('\"' + dto.getDeprecationReason() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasDeprecationReason", expression = "java( dto.getDeprecationReason() != null )")
    __EnumValue enumValueDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__EnumValue dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "description", expression = "java( dto.getDescription() == null ? null : ('\"' + dto.getDescription() + '\"') )")
    @Mapping(target = "defaultValue", expression = "java( dto.getDefaultValue() == null ? null : ('\"' + dto.getDefaultValue() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasDefaultValue", expression = "java( dto.getDefaultValue() != null )")
    __InputValue inputValueDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__InputValue dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "description", expression = "java( dto.getDescription() == null ? null : ('\"' + dto.getDescription() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasArgs", expression = "java( dto.getArgs() != null && dto.getArgs().size() > 0 )")
    __Directive directiveDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Directive dto);
}
