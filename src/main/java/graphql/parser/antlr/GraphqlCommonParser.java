// Generated from GraphqlCommon.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class GraphqlCommonParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, BooleanValue=12, NullValue=13, FRAGMENT=14, QUERY=15, 
		MUTATION=16, SUBSCRIPTION=17, SCHEMA=18, SCALAR=19, TYPE=20, INTERFACE=21, 
		IMPLEMENTS=22, ENUM=23, UNION=24, INPUT=25, EXTEND=26, DIRECTIVE=27, ON_KEYWORD=28, 
		NAME=29, IntValue=30, FloatValue=31, Sign=32, IntegerPart=33, NonZeroDigit=34, 
		ExponentPart=35, Digit=36, StringValue=37, TripleQuotedStringValue=38, 
		Comment=39, LF=40, CR=41, LineTerminator=42, Space=43, Tab=44, Comma=45, 
		UnicodeBOM=46;
	public static final int
		RULE_operationType = 0, RULE_description = 1, RULE_enumValue = 2, RULE_arrayValue = 3, 
		RULE_arrayValueWithVariable = 4, RULE_objectValue = 5, RULE_objectValueWithVariable = 6, 
		RULE_objectField = 7, RULE_objectFieldWithVariable = 8, RULE_directives = 9, 
		RULE_directive = 10, RULE_arguments = 11, RULE_argument = 12, RULE_baseName = 13, 
		RULE_fragmentName = 14, RULE_enumValueName = 15, RULE_name = 16, RULE_value = 17, 
		RULE_valueWithVariable = 18, RULE_variable = 19, RULE_defaultValue = 20, 
		RULE_stringValue = 21, RULE_type = 22, RULE_typeName = 23, RULE_listType = 24, 
		RULE_nonNullType = 25;
	private static String[] makeRuleNames() {
		return new String[] {
			"operationType", "description", "enumValue", "arrayValue", "arrayValueWithVariable", 
			"objectValue", "objectValueWithVariable", "objectField", "objectFieldWithVariable", 
			"directives", "directive", "arguments", "argument", "baseName", "fragmentName", 
			"enumValueName", "name", "value", "valueWithVariable", "variable", "defaultValue", 
			"stringValue", "type", "typeName", "listType", "nonNullType"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'['", "']'", "'{'", "'}'", "':'", "'@'", "'('", "')'", "'$'", 
			"'='", "'!'", null, "'null'", "'fragment'", "'query'", "'mutation'", 
			"'subscription'", "'schema'", "'scalar'", "'type'", "'interface'", "'implements'", 
			"'enum'", "'union'", "'input'", "'extend'", "'directive'", "'on'", null, 
			null, null, "'-'", null, null, null, null, null, null, null, null, null, 
			null, null, null, "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"BooleanValue", "NullValue", "FRAGMENT", "QUERY", "MUTATION", "SUBSCRIPTION", 
			"SCHEMA", "SCALAR", "TYPE", "INTERFACE", "IMPLEMENTS", "ENUM", "UNION", 
			"INPUT", "EXTEND", "DIRECTIVE", "ON_KEYWORD", "NAME", "IntValue", "FloatValue", 
			"Sign", "IntegerPart", "NonZeroDigit", "ExponentPart", "Digit", "StringValue", 
			"TripleQuotedStringValue", "Comment", "LF", "CR", "LineTerminator", "Space", 
			"Tab", "Comma", "UnicodeBOM"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "GraphqlCommon.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public GraphqlCommonParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class OperationTypeContext extends ParserRuleContext {
		public TerminalNode SUBSCRIPTION() { return getToken(GraphqlCommonParser.SUBSCRIPTION, 0); }
		public TerminalNode MUTATION() { return getToken(GraphqlCommonParser.MUTATION, 0); }
		public TerminalNode QUERY() { return getToken(GraphqlCommonParser.QUERY, 0); }
		public OperationTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterOperationType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitOperationType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitOperationType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperationTypeContext operationType() throws RecognitionException {
		OperationTypeContext _localctx = new OperationTypeContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_operationType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DescriptionContext extends ParserRuleContext {
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public DescriptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_description; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterDescription(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitDescription(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitDescription(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescriptionContext description() throws RecognitionException {
		DescriptionContext _localctx = new DescriptionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_description);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(54);
			stringValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumValueContext extends ParserRuleContext {
		public EnumValueNameContext enumValueName() {
			return getRuleContext(EnumValueNameContext.class,0);
		}
		public EnumValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterEnumValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitEnumValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitEnumValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumValueContext enumValue() throws RecognitionException {
		EnumValueContext _localctx = new EnumValueContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_enumValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(56);
			enumValueName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayValueContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public ArrayValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterArrayValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitArrayValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitArrayValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayValueContext arrayValue() throws RecognitionException {
		ArrayValueContext _localctx = new ArrayValueContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_arrayValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(58);
			match(T__0);
			setState(62);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << BooleanValue) | (1L << NullValue) | (1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << ON_KEYWORD) | (1L << NAME) | (1L << IntValue) | (1L << FloatValue) | (1L << StringValue) | (1L << TripleQuotedStringValue))) != 0)) {
				{
				{
				setState(59);
				value();
				}
				}
				setState(64);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(65);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayValueWithVariableContext extends ParserRuleContext {
		public List<ValueWithVariableContext> valueWithVariable() {
			return getRuleContexts(ValueWithVariableContext.class);
		}
		public ValueWithVariableContext valueWithVariable(int i) {
			return getRuleContext(ValueWithVariableContext.class,i);
		}
		public ArrayValueWithVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayValueWithVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterArrayValueWithVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitArrayValueWithVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitArrayValueWithVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayValueWithVariableContext arrayValueWithVariable() throws RecognitionException {
		ArrayValueWithVariableContext _localctx = new ArrayValueWithVariableContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_arrayValueWithVariable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(T__0);
			setState(71);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << T__8) | (1L << BooleanValue) | (1L << NullValue) | (1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << ON_KEYWORD) | (1L << NAME) | (1L << IntValue) | (1L << FloatValue) | (1L << StringValue) | (1L << TripleQuotedStringValue))) != 0)) {
				{
				{
				setState(68);
				valueWithVariable();
				}
				}
				setState(73);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(74);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectValueContext extends ParserRuleContext {
		public List<ObjectFieldContext> objectField() {
			return getRuleContexts(ObjectFieldContext.class);
		}
		public ObjectFieldContext objectField(int i) {
			return getRuleContext(ObjectFieldContext.class,i);
		}
		public ObjectValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterObjectValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitObjectValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitObjectValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectValueContext objectValue() throws RecognitionException {
		ObjectValueContext _localctx = new ObjectValueContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_objectValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			match(T__2);
			setState(80);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BooleanValue) | (1L << NullValue) | (1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << ON_KEYWORD) | (1L << NAME))) != 0)) {
				{
				{
				setState(77);
				objectField();
				}
				}
				setState(82);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(83);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectValueWithVariableContext extends ParserRuleContext {
		public List<ObjectFieldWithVariableContext> objectFieldWithVariable() {
			return getRuleContexts(ObjectFieldWithVariableContext.class);
		}
		public ObjectFieldWithVariableContext objectFieldWithVariable(int i) {
			return getRuleContext(ObjectFieldWithVariableContext.class,i);
		}
		public ObjectValueWithVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectValueWithVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterObjectValueWithVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitObjectValueWithVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitObjectValueWithVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectValueWithVariableContext objectValueWithVariable() throws RecognitionException {
		ObjectValueWithVariableContext _localctx = new ObjectValueWithVariableContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_objectValueWithVariable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(85);
			match(T__2);
			setState(89);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BooleanValue) | (1L << NullValue) | (1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << ON_KEYWORD) | (1L << NAME))) != 0)) {
				{
				{
				setState(86);
				objectFieldWithVariable();
				}
				}
				setState(91);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(92);
			match(T__3);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectFieldContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ObjectFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterObjectField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitObjectField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitObjectField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectFieldContext objectField() throws RecognitionException {
		ObjectFieldContext _localctx = new ObjectFieldContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_objectField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(94);
			name();
			setState(95);
			match(T__4);
			setState(96);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectFieldWithVariableContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueWithVariableContext valueWithVariable() {
			return getRuleContext(ValueWithVariableContext.class,0);
		}
		public ObjectFieldWithVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectFieldWithVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterObjectFieldWithVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitObjectFieldWithVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitObjectFieldWithVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectFieldWithVariableContext objectFieldWithVariable() throws RecognitionException {
		ObjectFieldWithVariableContext _localctx = new ObjectFieldWithVariableContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_objectFieldWithVariable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(98);
			name();
			setState(99);
			match(T__4);
			setState(100);
			valueWithVariable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectivesContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public DirectivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterDirectives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitDirectives(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitDirectives(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectivesContext directives() throws RecognitionException {
		DirectivesContext _localctx = new DirectivesContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_directives);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(102);
				directive();
				}
				}
				setState(105); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__5 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectiveContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitDirective(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_directive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(107);
			match(T__5);
			setState(108);
			name();
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(109);
				arguments();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentsContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(112);
			match(T__6);
			setState(114); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(113);
				argument();
				}
				}
				setState(116); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BooleanValue) | (1L << NullValue) | (1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << ON_KEYWORD) | (1L << NAME))) != 0) );
			setState(118);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueWithVariableContext valueWithVariable() {
			return getRuleContext(ValueWithVariableContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			name();
			setState(121);
			match(T__4);
			setState(122);
			valueWithVariable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BaseNameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphqlCommonParser.NAME, 0); }
		public TerminalNode FRAGMENT() { return getToken(GraphqlCommonParser.FRAGMENT, 0); }
		public TerminalNode QUERY() { return getToken(GraphqlCommonParser.QUERY, 0); }
		public TerminalNode MUTATION() { return getToken(GraphqlCommonParser.MUTATION, 0); }
		public TerminalNode SUBSCRIPTION() { return getToken(GraphqlCommonParser.SUBSCRIPTION, 0); }
		public TerminalNode SCHEMA() { return getToken(GraphqlCommonParser.SCHEMA, 0); }
		public TerminalNode SCALAR() { return getToken(GraphqlCommonParser.SCALAR, 0); }
		public TerminalNode TYPE() { return getToken(GraphqlCommonParser.TYPE, 0); }
		public TerminalNode INTERFACE() { return getToken(GraphqlCommonParser.INTERFACE, 0); }
		public TerminalNode IMPLEMENTS() { return getToken(GraphqlCommonParser.IMPLEMENTS, 0); }
		public TerminalNode ENUM() { return getToken(GraphqlCommonParser.ENUM, 0); }
		public TerminalNode UNION() { return getToken(GraphqlCommonParser.UNION, 0); }
		public TerminalNode INPUT() { return getToken(GraphqlCommonParser.INPUT, 0); }
		public TerminalNode EXTEND() { return getToken(GraphqlCommonParser.EXTEND, 0); }
		public TerminalNode DIRECTIVE() { return getToken(GraphqlCommonParser.DIRECTIVE, 0); }
		public BaseNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_baseName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterBaseName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitBaseName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitBaseName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BaseNameContext baseName() throws RecognitionException {
		BaseNameContext _localctx = new BaseNameContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_baseName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FRAGMENT) | (1L << QUERY) | (1L << MUTATION) | (1L << SUBSCRIPTION) | (1L << SCHEMA) | (1L << SCALAR) | (1L << TYPE) | (1L << INTERFACE) | (1L << IMPLEMENTS) | (1L << ENUM) | (1L << UNION) | (1L << INPUT) | (1L << EXTEND) | (1L << DIRECTIVE) | (1L << NAME))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FragmentNameContext extends ParserRuleContext {
		public BaseNameContext baseName() {
			return getRuleContext(BaseNameContext.class,0);
		}
		public TerminalNode BooleanValue() { return getToken(GraphqlCommonParser.BooleanValue, 0); }
		public TerminalNode NullValue() { return getToken(GraphqlCommonParser.NullValue, 0); }
		public FragmentNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterFragmentName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitFragmentName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitFragmentName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FragmentNameContext fragmentName() throws RecognitionException {
		FragmentNameContext _localctx = new FragmentNameContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_fragmentName);
		try {
			setState(129);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(126);
				baseName();
				}
				break;
			case BooleanValue:
				enterOuterAlt(_localctx, 2);
				{
				setState(127);
				match(BooleanValue);
				}
				break;
			case NullValue:
				enterOuterAlt(_localctx, 3);
				{
				setState(128);
				match(NullValue);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumValueNameContext extends ParserRuleContext {
		public BaseNameContext baseName() {
			return getRuleContext(BaseNameContext.class,0);
		}
		public TerminalNode ON_KEYWORD() { return getToken(GraphqlCommonParser.ON_KEYWORD, 0); }
		public EnumValueNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValueName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterEnumValueName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitEnumValueName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitEnumValueName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumValueNameContext enumValueName() throws RecognitionException {
		EnumValueNameContext _localctx = new EnumValueNameContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_enumValueName);
		try {
			setState(133);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(131);
				baseName();
				}
				break;
			case ON_KEYWORD:
				enterOuterAlt(_localctx, 2);
				{
				setState(132);
				match(ON_KEYWORD);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameContext extends ParserRuleContext {
		public BaseNameContext baseName() {
			return getRuleContext(BaseNameContext.class,0);
		}
		public TerminalNode BooleanValue() { return getToken(GraphqlCommonParser.BooleanValue, 0); }
		public TerminalNode NullValue() { return getToken(GraphqlCommonParser.NullValue, 0); }
		public TerminalNode ON_KEYWORD() { return getToken(GraphqlCommonParser.ON_KEYWORD, 0); }
		public NameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameContext name() throws RecognitionException {
		NameContext _localctx = new NameContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_name);
		try {
			setState(139);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(135);
				baseName();
				}
				break;
			case BooleanValue:
				enterOuterAlt(_localctx, 2);
				{
				setState(136);
				match(BooleanValue);
				}
				break;
			case NullValue:
				enterOuterAlt(_localctx, 3);
				{
				setState(137);
				match(NullValue);
				}
				break;
			case ON_KEYWORD:
				enterOuterAlt(_localctx, 4);
				{
				setState(138);
				match(ON_KEYWORD);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueContext extends ParserRuleContext {
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public TerminalNode IntValue() { return getToken(GraphqlCommonParser.IntValue, 0); }
		public TerminalNode FloatValue() { return getToken(GraphqlCommonParser.FloatValue, 0); }
		public TerminalNode BooleanValue() { return getToken(GraphqlCommonParser.BooleanValue, 0); }
		public TerminalNode NullValue() { return getToken(GraphqlCommonParser.NullValue, 0); }
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public ArrayValueContext arrayValue() {
			return getRuleContext(ArrayValueContext.class,0);
		}
		public ObjectValueContext objectValue() {
			return getRuleContext(ObjectValueContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_value);
		try {
			setState(149);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case StringValue:
			case TripleQuotedStringValue:
				enterOuterAlt(_localctx, 1);
				{
				setState(141);
				stringValue();
				}
				break;
			case IntValue:
				enterOuterAlt(_localctx, 2);
				{
				setState(142);
				match(IntValue);
				}
				break;
			case FloatValue:
				enterOuterAlt(_localctx, 3);
				{
				setState(143);
				match(FloatValue);
				}
				break;
			case BooleanValue:
				enterOuterAlt(_localctx, 4);
				{
				setState(144);
				match(BooleanValue);
				}
				break;
			case NullValue:
				enterOuterAlt(_localctx, 5);
				{
				setState(145);
				match(NullValue);
				}
				break;
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case ON_KEYWORD:
			case NAME:
				enterOuterAlt(_localctx, 6);
				{
				setState(146);
				enumValue();
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 7);
				{
				setState(147);
				arrayValue();
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 8);
				{
				setState(148);
				objectValue();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueWithVariableContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public TerminalNode IntValue() { return getToken(GraphqlCommonParser.IntValue, 0); }
		public TerminalNode FloatValue() { return getToken(GraphqlCommonParser.FloatValue, 0); }
		public TerminalNode BooleanValue() { return getToken(GraphqlCommonParser.BooleanValue, 0); }
		public TerminalNode NullValue() { return getToken(GraphqlCommonParser.NullValue, 0); }
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public ArrayValueWithVariableContext arrayValueWithVariable() {
			return getRuleContext(ArrayValueWithVariableContext.class,0);
		}
		public ObjectValueWithVariableContext objectValueWithVariable() {
			return getRuleContext(ObjectValueWithVariableContext.class,0);
		}
		public ValueWithVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueWithVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterValueWithVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitValueWithVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitValueWithVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueWithVariableContext valueWithVariable() throws RecognitionException {
		ValueWithVariableContext _localctx = new ValueWithVariableContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_valueWithVariable);
		try {
			setState(160);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__8:
				enterOuterAlt(_localctx, 1);
				{
				setState(151);
				variable();
				}
				break;
			case StringValue:
			case TripleQuotedStringValue:
				enterOuterAlt(_localctx, 2);
				{
				setState(152);
				stringValue();
				}
				break;
			case IntValue:
				enterOuterAlt(_localctx, 3);
				{
				setState(153);
				match(IntValue);
				}
				break;
			case FloatValue:
				enterOuterAlt(_localctx, 4);
				{
				setState(154);
				match(FloatValue);
				}
				break;
			case BooleanValue:
				enterOuterAlt(_localctx, 5);
				{
				setState(155);
				match(BooleanValue);
				}
				break;
			case NullValue:
				enterOuterAlt(_localctx, 6);
				{
				setState(156);
				match(NullValue);
				}
				break;
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case ON_KEYWORD:
			case NAME:
				enterOuterAlt(_localctx, 7);
				{
				setState(157);
				enumValue();
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 8);
				{
				setState(158);
				arrayValueWithVariable();
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 9);
				{
				setState(159);
				objectValueWithVariable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(T__8);
			setState(163);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefaultValueContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitDefaultValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitDefaultValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			match(T__9);
			setState(166);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringValueContext extends ParserRuleContext {
		public TerminalNode TripleQuotedStringValue() { return getToken(GraphqlCommonParser.TripleQuotedStringValue, 0); }
		public TerminalNode StringValue() { return getToken(GraphqlCommonParser.StringValue, 0); }
		public StringValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterStringValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitStringValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitStringValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringValueContext stringValue() throws RecognitionException {
		StringValueContext _localctx = new StringValueContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_stringValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(168);
			_la = _input.LA(1);
			if ( !(_la==StringValue || _la==TripleQuotedStringValue) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public NonNullTypeContext nonNullType() {
			return getRuleContext(NonNullTypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_type);
		try {
			setState(173);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(170);
				typeName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(171);
				listType();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(172);
				nonNullType();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeNameContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public TypeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterTypeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitTypeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitTypeName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeNameContext typeName() throws RecognitionException {
		TypeNameContext _localctx = new TypeNameContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_typeName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(175);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ListTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ListTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterListType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitListType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitListType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListTypeContext listType() throws RecognitionException {
		ListTypeContext _localctx = new ListTypeContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_listType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(177);
			match(T__0);
			setState(178);
			type();
			setState(179);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NonNullTypeContext extends ParserRuleContext {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public NonNullTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonNullType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).enterNonNullType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphqlCommonListener ) ((GraphqlCommonListener)listener).exitNonNullType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphqlCommonVisitor ) return ((GraphqlCommonVisitor<? extends T>)visitor).visitNonNullType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonNullTypeContext nonNullType() throws RecognitionException {
		NonNullTypeContext _localctx = new NonNullTypeContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_nonNullType);
		try {
			setState(187);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BooleanValue:
			case NullValue:
			case FRAGMENT:
			case QUERY:
			case MUTATION:
			case SUBSCRIPTION:
			case SCHEMA:
			case SCALAR:
			case TYPE:
			case INTERFACE:
			case IMPLEMENTS:
			case ENUM:
			case UNION:
			case INPUT:
			case EXTEND:
			case DIRECTIVE:
			case ON_KEYWORD:
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(181);
				typeName();
				setState(182);
				match(T__10);
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 2);
				{
				setState(184);
				listType();
				setState(185);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\60\u00c0\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\7\5?\n\5\f\5\16\5"+
		"B\13\5\3\5\3\5\3\6\3\6\7\6H\n\6\f\6\16\6K\13\6\3\6\3\6\3\7\3\7\7\7Q\n"+
		"\7\f\7\16\7T\13\7\3\7\3\7\3\b\3\b\7\bZ\n\b\f\b\16\b]\13\b\3\b\3\b\3\t"+
		"\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\6\13j\n\13\r\13\16\13k\3\f\3\f\3\f\5"+
		"\fq\n\f\3\r\3\r\6\ru\n\r\r\r\16\rv\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3"+
		"\17\3\20\3\20\3\20\5\20\u0084\n\20\3\21\3\21\5\21\u0088\n\21\3\22\3\22"+
		"\3\22\3\22\5\22\u008e\n\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23"+
		"\u0098\n\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u00a3\n"+
		"\24\3\25\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\30\5\30\u00b0"+
		"\n\30\3\31\3\31\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\5\33"+
		"\u00be\n\33\3\33\2\2\34\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*"+
		",.\60\62\64\2\5\3\2\21\23\4\2\20\35\37\37\3\2\'(\2\u00c4\2\66\3\2\2\2"+
		"\48\3\2\2\2\6:\3\2\2\2\b<\3\2\2\2\nE\3\2\2\2\fN\3\2\2\2\16W\3\2\2\2\20"+
		"`\3\2\2\2\22d\3\2\2\2\24i\3\2\2\2\26m\3\2\2\2\30r\3\2\2\2\32z\3\2\2\2"+
		"\34~\3\2\2\2\36\u0083\3\2\2\2 \u0087\3\2\2\2\"\u008d\3\2\2\2$\u0097\3"+
		"\2\2\2&\u00a2\3\2\2\2(\u00a4\3\2\2\2*\u00a7\3\2\2\2,\u00aa\3\2\2\2.\u00af"+
		"\3\2\2\2\60\u00b1\3\2\2\2\62\u00b3\3\2\2\2\64\u00bd\3\2\2\2\66\67\t\2"+
		"\2\2\67\3\3\2\2\289\5,\27\29\5\3\2\2\2:;\5 \21\2;\7\3\2\2\2<@\7\3\2\2"+
		"=?\5$\23\2>=\3\2\2\2?B\3\2\2\2@>\3\2\2\2@A\3\2\2\2AC\3\2\2\2B@\3\2\2\2"+
		"CD\7\4\2\2D\t\3\2\2\2EI\7\3\2\2FH\5&\24\2GF\3\2\2\2HK\3\2\2\2IG\3\2\2"+
		"\2IJ\3\2\2\2JL\3\2\2\2KI\3\2\2\2LM\7\4\2\2M\13\3\2\2\2NR\7\5\2\2OQ\5\20"+
		"\t\2PO\3\2\2\2QT\3\2\2\2RP\3\2\2\2RS\3\2\2\2SU\3\2\2\2TR\3\2\2\2UV\7\6"+
		"\2\2V\r\3\2\2\2W[\7\5\2\2XZ\5\22\n\2YX\3\2\2\2Z]\3\2\2\2[Y\3\2\2\2[\\"+
		"\3\2\2\2\\^\3\2\2\2][\3\2\2\2^_\7\6\2\2_\17\3\2\2\2`a\5\"\22\2ab\7\7\2"+
		"\2bc\5$\23\2c\21\3\2\2\2de\5\"\22\2ef\7\7\2\2fg\5&\24\2g\23\3\2\2\2hj"+
		"\5\26\f\2ih\3\2\2\2jk\3\2\2\2ki\3\2\2\2kl\3\2\2\2l\25\3\2\2\2mn\7\b\2"+
		"\2np\5\"\22\2oq\5\30\r\2po\3\2\2\2pq\3\2\2\2q\27\3\2\2\2rt\7\t\2\2su\5"+
		"\32\16\2ts\3\2\2\2uv\3\2\2\2vt\3\2\2\2vw\3\2\2\2wx\3\2\2\2xy\7\n\2\2y"+
		"\31\3\2\2\2z{\5\"\22\2{|\7\7\2\2|}\5&\24\2}\33\3\2\2\2~\177\t\3\2\2\177"+
		"\35\3\2\2\2\u0080\u0084\5\34\17\2\u0081\u0084\7\16\2\2\u0082\u0084\7\17"+
		"\2\2\u0083\u0080\3\2\2\2\u0083\u0081\3\2\2\2\u0083\u0082\3\2\2\2\u0084"+
		"\37\3\2\2\2\u0085\u0088\5\34\17\2\u0086\u0088\7\36\2\2\u0087\u0085\3\2"+
		"\2\2\u0087\u0086\3\2\2\2\u0088!\3\2\2\2\u0089\u008e\5\34\17\2\u008a\u008e"+
		"\7\16\2\2\u008b\u008e\7\17\2\2\u008c\u008e\7\36\2\2\u008d\u0089\3\2\2"+
		"\2\u008d\u008a\3\2\2\2\u008d\u008b\3\2\2\2\u008d\u008c\3\2\2\2\u008e#"+
		"\3\2\2\2\u008f\u0098\5,\27\2\u0090\u0098\7 \2\2\u0091\u0098\7!\2\2\u0092"+
		"\u0098\7\16\2\2\u0093\u0098\7\17\2\2\u0094\u0098\5\6\4\2\u0095\u0098\5"+
		"\b\5\2\u0096\u0098\5\f\7\2\u0097\u008f\3\2\2\2\u0097\u0090\3\2\2\2\u0097"+
		"\u0091\3\2\2\2\u0097\u0092\3\2\2\2\u0097\u0093\3\2\2\2\u0097\u0094\3\2"+
		"\2\2\u0097\u0095\3\2\2\2\u0097\u0096\3\2\2\2\u0098%\3\2\2\2\u0099\u00a3"+
		"\5(\25\2\u009a\u00a3\5,\27\2\u009b\u00a3\7 \2\2\u009c\u00a3\7!\2\2\u009d"+
		"\u00a3\7\16\2\2\u009e\u00a3\7\17\2\2\u009f\u00a3\5\6\4\2\u00a0\u00a3\5"+
		"\n\6\2\u00a1\u00a3\5\16\b\2\u00a2\u0099\3\2\2\2\u00a2\u009a\3\2\2\2\u00a2"+
		"\u009b\3\2\2\2\u00a2\u009c\3\2\2\2\u00a2\u009d\3\2\2\2\u00a2\u009e\3\2"+
		"\2\2\u00a2\u009f\3\2\2\2\u00a2\u00a0\3\2\2\2\u00a2\u00a1\3\2\2\2\u00a3"+
		"\'\3\2\2\2\u00a4\u00a5\7\13\2\2\u00a5\u00a6\5\"\22\2\u00a6)\3\2\2\2\u00a7"+
		"\u00a8\7\f\2\2\u00a8\u00a9\5$\23\2\u00a9+\3\2\2\2\u00aa\u00ab\t\4\2\2"+
		"\u00ab-\3\2\2\2\u00ac\u00b0\5\60\31\2\u00ad\u00b0\5\62\32\2\u00ae\u00b0"+
		"\5\64\33\2\u00af\u00ac\3\2\2\2\u00af\u00ad\3\2\2\2\u00af\u00ae\3\2\2\2"+
		"\u00b0/\3\2\2\2\u00b1\u00b2\5\"\22\2\u00b2\61\3\2\2\2\u00b3\u00b4\7\3"+
		"\2\2\u00b4\u00b5\5.\30\2\u00b5\u00b6\7\4\2\2\u00b6\63\3\2\2\2\u00b7\u00b8"+
		"\5\60\31\2\u00b8\u00b9\7\r\2\2\u00b9\u00be\3\2\2\2\u00ba\u00bb\5\62\32"+
		"\2\u00bb\u00bc\7\r\2\2\u00bc\u00be\3\2\2\2\u00bd\u00b7\3\2\2\2\u00bd\u00ba"+
		"\3\2\2\2\u00be\65\3\2\2\2\20@IR[kpv\u0083\u0087\u008d\u0097\u00a2\u00af"+
		"\u00bd";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}