// Generated from GraphqlCommon.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GraphqlCommonParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GraphqlCommonVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#operationType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationType(GraphqlCommonParser.OperationTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescription(GraphqlCommonParser.DescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(GraphqlCommonParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#arrayValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValue(GraphqlCommonParser.ArrayValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#arrayValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayValueWithVariable(GraphqlCommonParser.ArrayValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#objectValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValue(GraphqlCommonParser.ObjectValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#objectValueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectValueWithVariable(GraphqlCommonParser.ObjectValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#objectField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectField(GraphqlCommonParser.ObjectFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#objectFieldWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectFieldWithVariable(GraphqlCommonParser.ObjectFieldWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectives(GraphqlCommonParser.DirectivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirective(GraphqlCommonParser.DirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(GraphqlCommonParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(GraphqlCommonParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#baseName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseName(GraphqlCommonParser.BaseNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#fragmentName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFragmentName(GraphqlCommonParser.FragmentNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#enumValueName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValueName(GraphqlCommonParser.EnumValueNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(GraphqlCommonParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(GraphqlCommonParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#valueWithVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueWithVariable(GraphqlCommonParser.ValueWithVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(GraphqlCommonParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(GraphqlCommonParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#stringValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringValue(GraphqlCommonParser.StringValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(GraphqlCommonParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(GraphqlCommonParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#listType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListType(GraphqlCommonParser.ListTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link GraphqlCommonParser#nonNullType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonNullType(GraphqlCommonParser.NonNullTypeContext ctx);
}