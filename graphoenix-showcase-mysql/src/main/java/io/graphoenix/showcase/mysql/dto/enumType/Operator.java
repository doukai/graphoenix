package io.graphoenix.showcase.mysql.dto.enumType;

import io.graphoenix.spi.annotation.SchemaBean;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Enum;

@Enum
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@SchemaBean
public enum Operator {
  EQ,

  NEQ,

  LK,

  NLK,

  GT,

  NLTE,

  GTE,

  NLT,

  LT,

  NGTE,

  LTE,

  NGT,

  NIL,

  NNIL,

  IN,

  NIN
}
