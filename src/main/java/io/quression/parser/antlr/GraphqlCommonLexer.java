// Generated from GraphqlCommon.g4 by ANTLR 4.9
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class GraphqlCommonLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9", RuntimeMetaData.VERSION); }

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
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "BooleanValue", "NullValue", "FRAGMENT", "QUERY", "MUTATION", 
			"SUBSCRIPTION", "SCHEMA", "SCALAR", "TYPE", "INTERFACE", "IMPLEMENTS", 
			"ENUM", "UNION", "INPUT", "EXTEND", "DIRECTIVE", "ON_KEYWORD", "NAME", 
			"IntValue", "FloatValue", "Sign", "IntegerPart", "NonZeroDigit", "ExponentPart", 
			"Digit", "StringValue", "TripleQuotedStringValue", "TripleQuotedStringPart", 
			"EscapedTripleQuote", "ExtendedSourceCharacter", "ExtendedSourceCharacterWithoutLineFeed", 
			"Comment", "EscapedChar", "Unicode", "Hex", "LF", "CR", "LineTerminator", 
			"Space", "Tab", "Comma", "UnicodeBOM"
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


	public GraphqlCommonLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "GraphqlCommon.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\60\u018f\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7"+
		"\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\5\r\u008d\n\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\34"+
		"\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\36\3\36"+
		"\7\36\u0108\n\36\f\36\16\36\u010b\13\36\3\37\5\37\u010e\n\37\3\37\3\37"+
		"\3 \5 \u0113\n \3 \3 \3 \6 \u0118\n \r \16 \u0119\5 \u011c\n \3 \5 \u011f"+
		"\n \3!\3!\3\"\3\"\3\"\3\"\6\"\u0127\n\"\r\"\16\"\u0128\5\"\u012b\n\"\3"+
		"#\3#\3$\3$\5$\u0131\n$\3$\6$\u0134\n$\r$\16$\u0135\3%\3%\3&\3&\3&\7&\u013d"+
		"\n&\f&\16&\u0140\13&\3&\3&\3\'\3\'\3\'\3\'\3\'\5\'\u0149\n\'\3\'\3\'\3"+
		"\'\3\'\3(\3(\6(\u0151\n(\r(\16(\u0152\3)\3)\3)\3)\3)\3*\3*\3+\3+\3,\3"+
		",\7,\u0160\n,\f,\16,\u0163\13,\3,\3,\3-\3-\3-\5-\u016a\n-\3.\3.\3.\3."+
		"\3.\3.\3/\3/\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3"+
		"\62\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\66\3"+
		"\66\3\66\3\66\3\u0152\2\67\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32"+
		"\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O\2Q\2S\2U\2W)Y\2[\2]\2"+
		"_*a+c,e-g.i/k\60\3\2\17\5\2C\\aac|\6\2\62;C\\aac|\4\2GGgg\4\2--//\7\2"+
		"\f\f\17\17$$^^\u202a\u202b\n\2$$\61\61^^ddhhppttvv\5\2\62;CHch\3\2\f\f"+
		"\3\2\17\17\3\2\u202a\u202b\3\2\"\"\3\2\13\13\3\2\uff01\uff01\4\5\2\13"+
		"\2\f\2\17\2\17\2\"\2\1\22\4\2\13\2\13\2\"\2\1\22\u019a\2\3\3\2\2\2\2\5"+
		"\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2"+
		"\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33"+
		"\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2"+
		"\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2"+
		"\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2"+
		"\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K"+
		"\3\2\2\2\2M\3\2\2\2\2W\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2"+
		"\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\3m\3\2\2\2\5o\3\2\2\2\7q\3\2\2\2"+
		"\ts\3\2\2\2\13u\3\2\2\2\rw\3\2\2\2\17y\3\2\2\2\21{\3\2\2\2\23}\3\2\2\2"+
		"\25\177\3\2\2\2\27\u0081\3\2\2\2\31\u008c\3\2\2\2\33\u008e\3\2\2\2\35"+
		"\u0093\3\2\2\2\37\u009c\3\2\2\2!\u00a2\3\2\2\2#\u00ab\3\2\2\2%\u00b8\3"+
		"\2\2\2\'\u00bf\3\2\2\2)\u00c6\3\2\2\2+\u00cb\3\2\2\2-\u00d5\3\2\2\2/\u00e0"+
		"\3\2\2\2\61\u00e5\3\2\2\2\63\u00eb\3\2\2\2\65\u00f1\3\2\2\2\67\u00f8\3"+
		"\2\2\29\u0102\3\2\2\2;\u0105\3\2\2\2=\u010d\3\2\2\2?\u0112\3\2\2\2A\u0120"+
		"\3\2\2\2C\u012a\3\2\2\2E\u012c\3\2\2\2G\u012e\3\2\2\2I\u0137\3\2\2\2K"+
		"\u0139\3\2\2\2M\u0143\3\2\2\2O\u0150\3\2\2\2Q\u0154\3\2\2\2S\u0159\3\2"+
		"\2\2U\u015b\3\2\2\2W\u015d\3\2\2\2Y\u0166\3\2\2\2[\u016b\3\2\2\2]\u0171"+
		"\3\2\2\2_\u0173\3\2\2\2a\u0177\3\2\2\2c\u017b\3\2\2\2e\u017f\3\2\2\2g"+
		"\u0183\3\2\2\2i\u0187\3\2\2\2k\u018b\3\2\2\2mn\7]\2\2n\4\3\2\2\2op\7_"+
		"\2\2p\6\3\2\2\2qr\7}\2\2r\b\3\2\2\2st\7\177\2\2t\n\3\2\2\2uv\7<\2\2v\f"+
		"\3\2\2\2wx\7B\2\2x\16\3\2\2\2yz\7*\2\2z\20\3\2\2\2{|\7+\2\2|\22\3\2\2"+
		"\2}~\7&\2\2~\24\3\2\2\2\177\u0080\7?\2\2\u0080\26\3\2\2\2\u0081\u0082"+
		"\7#\2\2\u0082\30\3\2\2\2\u0083\u0084\7v\2\2\u0084\u0085\7t\2\2\u0085\u0086"+
		"\7w\2\2\u0086\u008d\7g\2\2\u0087\u0088\7h\2\2\u0088\u0089\7c\2\2\u0089"+
		"\u008a\7n\2\2\u008a\u008b\7u\2\2\u008b\u008d\7g\2\2\u008c\u0083\3\2\2"+
		"\2\u008c\u0087\3\2\2\2\u008d\32\3\2\2\2\u008e\u008f\7p\2\2\u008f\u0090"+
		"\7w\2\2\u0090\u0091\7n\2\2\u0091\u0092\7n\2\2\u0092\34\3\2\2\2\u0093\u0094"+
		"\7h\2\2\u0094\u0095\7t\2\2\u0095\u0096\7c\2\2\u0096\u0097\7i\2\2\u0097"+
		"\u0098\7o\2\2\u0098\u0099\7g\2\2\u0099\u009a\7p\2\2\u009a\u009b\7v\2\2"+
		"\u009b\36\3\2\2\2\u009c\u009d\7s\2\2\u009d\u009e\7w\2\2\u009e\u009f\7"+
		"g\2\2\u009f\u00a0\7t\2\2\u00a0\u00a1\7{\2\2\u00a1 \3\2\2\2\u00a2\u00a3"+
		"\7o\2\2\u00a3\u00a4\7w\2\2\u00a4\u00a5\7v\2\2\u00a5\u00a6\7c\2\2\u00a6"+
		"\u00a7\7v\2\2\u00a7\u00a8\7k\2\2\u00a8\u00a9\7q\2\2\u00a9\u00aa\7p\2\2"+
		"\u00aa\"\3\2\2\2\u00ab\u00ac\7u\2\2\u00ac\u00ad\7w\2\2\u00ad\u00ae\7d"+
		"\2\2\u00ae\u00af\7u\2\2\u00af\u00b0\7e\2\2\u00b0\u00b1\7t\2\2\u00b1\u00b2"+
		"\7k\2\2\u00b2\u00b3\7r\2\2\u00b3\u00b4\7v\2\2\u00b4\u00b5\7k\2\2\u00b5"+
		"\u00b6\7q\2\2\u00b6\u00b7\7p\2\2\u00b7$\3\2\2\2\u00b8\u00b9\7u\2\2\u00b9"+
		"\u00ba\7e\2\2\u00ba\u00bb\7j\2\2\u00bb\u00bc\7g\2\2\u00bc\u00bd\7o\2\2"+
		"\u00bd\u00be\7c\2\2\u00be&\3\2\2\2\u00bf\u00c0\7u\2\2\u00c0\u00c1\7e\2"+
		"\2\u00c1\u00c2\7c\2\2\u00c2\u00c3\7n\2\2\u00c3\u00c4\7c\2\2\u00c4\u00c5"+
		"\7t\2\2\u00c5(\3\2\2\2\u00c6\u00c7\7v\2\2\u00c7\u00c8\7{\2\2\u00c8\u00c9"+
		"\7r\2\2\u00c9\u00ca\7g\2\2\u00ca*\3\2\2\2\u00cb\u00cc\7k\2\2\u00cc\u00cd"+
		"\7p\2\2\u00cd\u00ce\7v\2\2\u00ce\u00cf\7g\2\2\u00cf\u00d0\7t\2\2\u00d0"+
		"\u00d1\7h\2\2\u00d1\u00d2\7c\2\2\u00d2\u00d3\7e\2\2\u00d3\u00d4\7g\2\2"+
		"\u00d4,\3\2\2\2\u00d5\u00d6\7k\2\2\u00d6\u00d7\7o\2\2\u00d7\u00d8\7r\2"+
		"\2\u00d8\u00d9\7n\2\2\u00d9\u00da\7g\2\2\u00da\u00db\7o\2\2\u00db\u00dc"+
		"\7g\2\2\u00dc\u00dd\7p\2\2\u00dd\u00de\7v\2\2\u00de\u00df\7u\2\2\u00df"+
		".\3\2\2\2\u00e0\u00e1\7g\2\2\u00e1\u00e2\7p\2\2\u00e2\u00e3\7w\2\2\u00e3"+
		"\u00e4\7o\2\2\u00e4\60\3\2\2\2\u00e5\u00e6\7w\2\2\u00e6\u00e7\7p\2\2\u00e7"+
		"\u00e8\7k\2\2\u00e8\u00e9\7q\2\2\u00e9\u00ea\7p\2\2\u00ea\62\3\2\2\2\u00eb"+
		"\u00ec\7k\2\2\u00ec\u00ed\7p\2\2\u00ed\u00ee\7r\2\2\u00ee\u00ef\7w\2\2"+
		"\u00ef\u00f0\7v\2\2\u00f0\64\3\2\2\2\u00f1\u00f2\7g\2\2\u00f2\u00f3\7"+
		"z\2\2\u00f3\u00f4\7v\2\2\u00f4\u00f5\7g\2\2\u00f5\u00f6\7p\2\2\u00f6\u00f7"+
		"\7f\2\2\u00f7\66\3\2\2\2\u00f8\u00f9\7f\2\2\u00f9\u00fa\7k\2\2\u00fa\u00fb"+
		"\7t\2\2\u00fb\u00fc\7g\2\2\u00fc\u00fd\7e\2\2\u00fd\u00fe\7v\2\2\u00fe"+
		"\u00ff\7k\2\2\u00ff\u0100\7x\2\2\u0100\u0101\7g\2\2\u01018\3\2\2\2\u0102"+
		"\u0103\7q\2\2\u0103\u0104\7p\2\2\u0104:\3\2\2\2\u0105\u0109\t\2\2\2\u0106"+
		"\u0108\t\3\2\2\u0107\u0106\3\2\2\2\u0108\u010b\3\2\2\2\u0109\u0107\3\2"+
		"\2\2\u0109\u010a\3\2\2\2\u010a<\3\2\2\2\u010b\u0109\3\2\2\2\u010c\u010e"+
		"\5A!\2\u010d\u010c\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u010f\3\2\2\2\u010f"+
		"\u0110\5C\"\2\u0110>\3\2\2\2\u0111\u0113\5A!\2\u0112\u0111\3\2\2\2\u0112"+
		"\u0113\3\2\2\2\u0113\u0114\3\2\2\2\u0114\u011b\5C\"\2\u0115\u0117\7\60"+
		"\2\2\u0116\u0118\5I%\2\u0117\u0116\3\2\2\2\u0118\u0119\3\2\2\2\u0119\u0117"+
		"\3\2\2\2\u0119\u011a\3\2\2\2\u011a\u011c\3\2\2\2\u011b\u0115\3\2\2\2\u011b"+
		"\u011c\3\2\2\2\u011c\u011e\3\2\2\2\u011d\u011f\5G$\2\u011e\u011d\3\2\2"+
		"\2\u011e\u011f\3\2\2\2\u011f@\3\2\2\2\u0120\u0121\7/\2\2\u0121B\3\2\2"+
		"\2\u0122\u012b\7\62\2\2\u0123\u012b\5E#\2\u0124\u0126\5E#\2\u0125\u0127"+
		"\5I%\2\u0126\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u0126\3\2\2\2\u0128"+
		"\u0129\3\2\2\2\u0129\u012b\3\2\2\2\u012a\u0122\3\2\2\2\u012a\u0123\3\2"+
		"\2\2\u012a\u0124\3\2\2\2\u012bD\3\2\2\2\u012c\u012d\4\63;\2\u012dF\3\2"+
		"\2\2\u012e\u0130\t\4\2\2\u012f\u0131\t\5\2\2\u0130\u012f\3\2\2\2\u0130"+
		"\u0131\3\2\2\2\u0131\u0133\3\2\2\2\u0132\u0134\5I%\2\u0133\u0132\3\2\2"+
		"\2\u0134\u0135\3\2\2\2\u0135\u0133\3\2\2\2\u0135\u0136\3\2\2\2\u0136H"+
		"\3\2\2\2\u0137\u0138\4\62;\2\u0138J\3\2\2\2\u0139\u013e\7$\2\2\u013a\u013d"+
		"\n\6\2\2\u013b\u013d\5Y-\2\u013c\u013a\3\2\2\2\u013c\u013b\3\2\2\2\u013d"+
		"\u0140\3\2\2\2\u013e\u013c\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u0141\3\2"+
		"\2\2\u0140\u013e\3\2\2\2\u0141\u0142\7$\2\2\u0142L\3\2\2\2\u0143\u0144"+
		"\7$\2\2\u0144\u0145\7$\2\2\u0145\u0146\7$\2\2\u0146\u0148\3\2\2\2\u0147"+
		"\u0149\5O(\2\u0148\u0147\3\2\2\2\u0148\u0149\3\2\2\2\u0149\u014a\3\2\2"+
		"\2\u014a\u014b\7$\2\2\u014b\u014c\7$\2\2\u014c\u014d\7$\2\2\u014dN\3\2"+
		"\2\2\u014e\u0151\5Q)\2\u014f\u0151\5S*\2\u0150\u014e\3\2\2\2\u0150\u014f"+
		"\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0153\3\2\2\2\u0152\u0150\3\2\2\2\u0153"+
		"P\3\2\2\2\u0154\u0155\7^\2\2\u0155\u0156\7$\2\2\u0156\u0157\7$\2\2\u0157"+
		"\u0158\7$\2\2\u0158R\3\2\2\2\u0159\u015a\t\17\2\2\u015aT\3\2\2\2\u015b"+
		"\u015c\t\20\2\2\u015cV\3\2\2\2\u015d\u0161\7%\2\2\u015e\u0160\5U+\2\u015f"+
		"\u015e\3\2\2\2\u0160\u0163\3\2\2\2\u0161\u015f\3\2\2\2\u0161\u0162\3\2"+
		"\2\2\u0162\u0164\3\2\2\2\u0163\u0161\3\2\2\2\u0164\u0165\b,\2\2\u0165"+
		"X\3\2\2\2\u0166\u0169\7^\2\2\u0167\u016a\t\7\2\2\u0168\u016a\5[.\2\u0169"+
		"\u0167\3\2\2\2\u0169\u0168\3\2\2\2\u016aZ\3\2\2\2\u016b\u016c\7w\2\2\u016c"+
		"\u016d\5]/\2\u016d\u016e\5]/\2\u016e\u016f\5]/\2\u016f\u0170\5]/\2\u0170"+
		"\\\3\2\2\2\u0171\u0172\t\b\2\2\u0172^\3\2\2\2\u0173\u0174\t\t\2\2\u0174"+
		"\u0175\3\2\2\2\u0175\u0176\b\60\3\2\u0176`\3\2\2\2\u0177\u0178\t\n\2\2"+
		"\u0178\u0179\3\2\2\2\u0179\u017a\b\61\3\2\u017ab\3\2\2\2\u017b\u017c\t"+
		"\13\2\2\u017c\u017d\3\2\2\2\u017d\u017e\b\62\3\2\u017ed\3\2\2\2\u017f"+
		"\u0180\t\f\2\2\u0180\u0181\3\2\2\2\u0181\u0182\b\63\3\2\u0182f\3\2\2\2"+
		"\u0183\u0184\t\r\2\2\u0184\u0185\3\2\2\2\u0185\u0186\b\64\3\2\u0186h\3"+
		"\2\2\2\u0187\u0188\7.\2\2\u0188\u0189\3\2\2\2\u0189\u018a\b\65\3\2\u018a"+
		"j\3\2\2\2\u018b\u018c\t\16\2\2\u018c\u018d\3\2\2\2\u018d\u018e\b\66\3"+
		"\2\u018el\3\2\2\2\25\2\u008c\u0109\u010d\u0112\u0119\u011b\u011e\u0128"+
		"\u012a\u0130\u0135\u013c\u013e\u0148\u0150\u0152\u0161\u0169\4\2\4\2\2"+
		"\5\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}