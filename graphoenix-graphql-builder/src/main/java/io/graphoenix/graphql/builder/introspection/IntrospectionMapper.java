package io.graphoenix.graphql.builder.introspection;

import io.graphoenix.graphql.builder.introspection.vo.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface IntrospectionMapper {

    IntrospectionMapper INSTANCE = Mappers.getMapper(IntrospectionMapper.class);

    @Mapping(target = "hasMutationType", expression = "java( dto.getMutationType() != null )")
    @Mapping(target = "hasSubscriptionType", expression = "java( dto.getSubscriptionType() != null )")
    @Mapping(target = "hasDirectives", expression = "java( dto.getDirectives() != null && dto.getDirectives().size() > 0 )")
    __Schema schemaDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Schema dto);

    @AfterMapping
    default void after(@MappingTarget __Schema vo) {
        if (vo.getTypes() != null && vo.getTypes().size() > 0) {
            vo.getTypes().get(vo.getTypes().size() - 1).setLast(true);
        }
    }

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "hasName", expression = "java( dto.getName() != null )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasFields", expression = "java( dto.getFields() != null && dto.getFields().size() > 0 )")
    @Mapping(target = "hasInterfaces", expression = "java( dto.getInterfaces() != null && dto.getInterfaces().size() > 0 )")
    @Mapping(target = "hasPossibleTypes", expression = "java( dto.getPossibleTypes() != null && dto.getPossibleTypes().size() > 0 )")
    @Mapping(target = "hasEnumValues", expression = "java( dto.getEnumValues() != null && dto.getEnumValues().size() > 0 )")
    @Mapping(target = "hasInputFields", expression = "java( dto.getInputFields() != null && dto.getInputFields().size() > 0 )")
    @Mapping(target = "hasOfType", expression = "java( dto.getOfType() != null )")
    @Mapping(target = "last", ignore = true)
    __Type typeDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Type dto);

    @AfterMapping
    default void after(@MappingTarget __Type vo) {
        if (vo.getFields() != null && vo.getFields().size() > 0) {
            vo.getFields().get(vo.getFields().size() - 1).setLast(true);
        }
        if (vo.getEnumValues() != null && vo.getEnumValues().size() > 0) {
            vo.getEnumValues().get(vo.getEnumValues().size() - 1).setLast(true);
        }
        if (vo.getInputFields() != null && vo.getInputFields().size() > 0) {
            vo.getInputFields().get(vo.getInputFields().size() - 1).setLast(true);
        }
        if (vo.getInterfaces() != null && vo.getInterfaces().size() > 0) {
            vo.getInterfaces().get(vo.getInterfaces().size() - 1).setLast(true);
        }
        if (vo.getPossibleTypes() != null && vo.getPossibleTypes().size() > 0) {
            vo.getPossibleTypes().get(vo.getPossibleTypes().size() - 1).setLast(true);
        }
    }

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasArgs", expression = "java( dto.getArgs() != null && dto.getArgs().size() > 0 )")
    @Mapping(target = "hasDeprecationReason", expression = "java( dto.getDeprecationReason() != null )")
    @Mapping(target = "last", ignore = true)
    __Field fieldDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Field dto);

    @AfterMapping
    default void after(@MappingTarget __Field vo) {
        if (vo.getArgs() != null && vo.getArgs().size() > 0) {
            vo.getArgs().get(vo.getArgs().size() - 1).setLast(true);
        }
    }


    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasDeprecationReason", expression = "java( dto.getDeprecationReason() != null )")
    @Mapping(target = "last", ignore = true)
    __EnumValue enumValueDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__EnumValue dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasDefaultValue", expression = "java( dto.getDefaultValue() != null )")
    @Mapping(target = "last", ignore = true)
    __InputValue inputValueDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__InputValue dto);

    @Mapping(target = "name", expression = "java( dto.getName() == null ? null : ('\"' + dto.getName() + '\"') )")
    @Mapping(target = "hasDescription", expression = "java( dto.getDescription() != null )")
    @Mapping(target = "hasArgs", expression = "java( dto.getArgs() != null && dto.getArgs().size() > 0 )")
    @Mapping(target = "last", ignore = true)
    __Directive directiveDTOToVO(io.graphoenix.graphql.builder.introspection.dto.__Directive dto);

    @AfterMapping
    default void after(@MappingTarget __Directive vo) {
        if (vo.getArgs() != null && vo.getArgs().size() > 0) {
            vo.getArgs().get(vo.getArgs().size() - 1).setLast(true);
        }
    }
}
