// Generated from GraphqlSDL.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class GraphqlSDLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, BooleanValue=14, NullValue=15, 
		FRAGMENT=16, QUERY=17, MUTATION=18, SUBSCRIPTION=19, SCHEMA=20, SCALAR=21, 
		TYPE=22, INTERFACE=23, IMPLEMENTS=24, ENUM=25, UNION=26, INPUT=27, EXTEND=28, 
		DIRECTIVE=29, ON_KEYWORD=30, NAME=31, IntValue=32, FloatValue=33, Sign=34, 
		IntegerPart=35, NonZeroDigit=36, ExponentPart=37, Digit=38, StringValue=39, 
		TripleQuotedStringValue=40, Comment=41, LF=42, CR=43, LineTerminator=44, 
		Space=45, Tab=46, Comma=47, UnicodeBOM=48;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "BooleanValue", "NullValue", "FRAGMENT", 
			"QUERY", "MUTATION", "SUBSCRIPTION", "SCHEMA", "SCALAR", "TYPE", "INTERFACE", 
			"IMPLEMENTS", "ENUM", "UNION", "INPUT", "EXTEND", "DIRECTIVE", "ON_KEYWORD", 
			"NAME", "IntValue", "FloatValue", "Sign", "IntegerPart", "NonZeroDigit", 
			"ExponentPart", "Digit", "StringValue", "TripleQuotedStringValue", "TripleQuotedStringPart", 
			"EscapedTripleQuote", "ExtendedSourceCharacter", "ExtendedSourceCharacterWithoutLineFeed", 
			"Comment", "EscapedChar", "Unicode", "Hex", "LF", "CR", "LineTerminator", 
			"Space", "Tab", "Comma", "UnicodeBOM"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'{'", "'}'", "':'", "'&'", "'('", "')'", "'='", "'|'", "'@'", 
			"'['", "']'", "'$'", "'!'", null, "'null'", "'fragment'", "'query'", 
			"'mutation'", "'subscription'", "'schema'", "'scalar'", "'type'", "'interface'", 
			"'implements'", "'enum'", "'union'", "'input'", "'extend'", "'directive'", 
			"'on'", null, null, null, "'-'", null, null, null, null, null, null, 
			null, null, null, null, null, null, "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, "BooleanValue", "NullValue", "FRAGMENT", "QUERY", "MUTATION", 
			"SUBSCRIPTION", "SCHEMA", "SCALAR", "TYPE", "INTERFACE", "IMPLEMENTS", 
			"ENUM", "UNION", "INPUT", "EXTEND", "DIRECTIVE", "ON_KEYWORD", "NAME", 
			"IntValue", "FloatValue", "Sign", "IntegerPart", "NonZeroDigit", "ExponentPart", 
			"Digit", "StringValue", "TripleQuotedStringValue", "Comment", "LF", "CR", 
			"LineTerminator", "Space", "Tab", "Comma", "UnicodeBOM"
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


	public GraphqlSDLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "GraphqlSDL.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\62\u0197\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\3\2\3\2\3\3\3\3\3\4\3\4\3\5"+
		"\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r"+
		"\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\5\17\u0095\n\17"+
		"\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26"+
		"\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32"+
		"\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3 \3 \7 \u0110\n \f \16 \u0113\13"+
		" \3!\5!\u0116\n!\3!\3!\3\"\5\"\u011b\n\"\3\"\3\"\3\"\6\"\u0120\n\"\r\""+
		"\16\"\u0121\5\"\u0124\n\"\3\"\5\"\u0127\n\"\3#\3#\3$\3$\3$\3$\6$\u012f"+
		"\n$\r$\16$\u0130\5$\u0133\n$\3%\3%\3&\3&\5&\u0139\n&\3&\6&\u013c\n&\r"+
		"&\16&\u013d\3\'\3\'\3(\3(\3(\7(\u0145\n(\f(\16(\u0148\13(\3(\3(\3)\3)"+
		"\3)\3)\3)\5)\u0151\n)\3)\3)\3)\3)\3*\3*\6*\u0159\n*\r*\16*\u015a\3+\3"+
		"+\3+\3+\3+\3,\3,\3-\3-\3.\3.\7.\u0168\n.\f.\16.\u016b\13.\3.\3.\3/\3/"+
		"\3/\5/\u0172\n/\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\62\3\62\3\62"+
		"\3\62\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\66"+
		"\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38\38\38\3\u015a\29\3\3\5\4\7\5"+
		"\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23"+
		"%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G"+
		"%I&K\'M(O)Q*S\2U\2W\2Y\2[+]\2_\2a\2c,e-g.i/k\60m\61o\62\3\2\17\5\2C\\"+
		"aac|\6\2\62;C\\aac|\4\2GGgg\4\2--//\7\2\f\f\17\17$$^^\u202a\u202b\n\2"+
		"$$\61\61^^ddhhppttvv\5\2\62;CHch\3\2\f\f\3\2\17\17\3\2\u202a\u202b\3\2"+
		"\"\"\3\2\13\13\3\2\uff01\uff01\4\5\2\13\2\f\2\17\2\17\2\"\2\1\22\4\2\13"+
		"\2\13\2\"\2\1\22\u01a2\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2"+
		"\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25"+
		"\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2"+
		"\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2"+
		"\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3"+
		"\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2"+
		"\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2"+
		"Q\3\2\2\2\2[\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3"+
		"\2\2\2\2m\3\2\2\2\2o\3\2\2\2\3q\3\2\2\2\5s\3\2\2\2\7u\3\2\2\2\tw\3\2\2"+
		"\2\13y\3\2\2\2\r{\3\2\2\2\17}\3\2\2\2\21\177\3\2\2\2\23\u0081\3\2\2\2"+
		"\25\u0083\3\2\2\2\27\u0085\3\2\2\2\31\u0087\3\2\2\2\33\u0089\3\2\2\2\35"+
		"\u0094\3\2\2\2\37\u0096\3\2\2\2!\u009b\3\2\2\2#\u00a4\3\2\2\2%\u00aa\3"+
		"\2\2\2\'\u00b3\3\2\2\2)\u00c0\3\2\2\2+\u00c7\3\2\2\2-\u00ce\3\2\2\2/\u00d3"+
		"\3\2\2\2\61\u00dd\3\2\2\2\63\u00e8\3\2\2\2\65\u00ed\3\2\2\2\67\u00f3\3"+
		"\2\2\29\u00f9\3\2\2\2;\u0100\3\2\2\2=\u010a\3\2\2\2?\u010d\3\2\2\2A\u0115"+
		"\3\2\2\2C\u011a\3\2\2\2E\u0128\3\2\2\2G\u0132\3\2\2\2I\u0134\3\2\2\2K"+
		"\u0136\3\2\2\2M\u013f\3\2\2\2O\u0141\3\2\2\2Q\u014b\3\2\2\2S\u0158\3\2"+
		"\2\2U\u015c\3\2\2\2W\u0161\3\2\2\2Y\u0163\3\2\2\2[\u0165\3\2\2\2]\u016e"+
		"\3\2\2\2_\u0173\3\2\2\2a\u0179\3\2\2\2c\u017b\3\2\2\2e\u017f\3\2\2\2g"+
		"\u0183\3\2\2\2i\u0187\3\2\2\2k\u018b\3\2\2\2m\u018f\3\2\2\2o\u0193\3\2"+
		"\2\2qr\7}\2\2r\4\3\2\2\2st\7\177\2\2t\6\3\2\2\2uv\7<\2\2v\b\3\2\2\2wx"+
		"\7(\2\2x\n\3\2\2\2yz\7*\2\2z\f\3\2\2\2{|\7+\2\2|\16\3\2\2\2}~\7?\2\2~"+
		"\20\3\2\2\2\177\u0080\7~\2\2\u0080\22\3\2\2\2\u0081\u0082\7B\2\2\u0082"+
		"\24\3\2\2\2\u0083\u0084\7]\2\2\u0084\26\3\2\2\2\u0085\u0086\7_\2\2\u0086"+
		"\30\3\2\2\2\u0087\u0088\7&\2\2\u0088\32\3\2\2\2\u0089\u008a\7#\2\2\u008a"+
		"\34\3\2\2\2\u008b\u008c\7v\2\2\u008c\u008d\7t\2\2\u008d\u008e\7w\2\2\u008e"+
		"\u0095\7g\2\2\u008f\u0090\7h\2\2\u0090\u0091\7c\2\2\u0091\u0092\7n\2\2"+
		"\u0092\u0093\7u\2\2\u0093\u0095\7g\2\2\u0094\u008b\3\2\2\2\u0094\u008f"+
		"\3\2\2\2\u0095\36\3\2\2\2\u0096\u0097\7p\2\2\u0097\u0098\7w\2\2\u0098"+
		"\u0099\7n\2\2\u0099\u009a\7n\2\2\u009a \3\2\2\2\u009b\u009c\7h\2\2\u009c"+
		"\u009d\7t\2\2\u009d\u009e\7c\2\2\u009e\u009f\7i\2\2\u009f\u00a0\7o\2\2"+
		"\u00a0\u00a1\7g\2\2\u00a1\u00a2\7p\2\2\u00a2\u00a3\7v\2\2\u00a3\"\3\2"+
		"\2\2\u00a4\u00a5\7s\2\2\u00a5\u00a6\7w\2\2\u00a6\u00a7\7g\2\2\u00a7\u00a8"+
		"\7t\2\2\u00a8\u00a9\7{\2\2\u00a9$\3\2\2\2\u00aa\u00ab\7o\2\2\u00ab\u00ac"+
		"\7w\2\2\u00ac\u00ad\7v\2\2\u00ad\u00ae\7c\2\2\u00ae\u00af\7v\2\2\u00af"+
		"\u00b0\7k\2\2\u00b0\u00b1\7q\2\2\u00b1\u00b2\7p\2\2\u00b2&\3\2\2\2\u00b3"+
		"\u00b4\7u\2\2\u00b4\u00b5\7w\2\2\u00b5\u00b6\7d\2\2\u00b6\u00b7\7u\2\2"+
		"\u00b7\u00b8\7e\2\2\u00b8\u00b9\7t\2\2\u00b9\u00ba\7k\2\2\u00ba\u00bb"+
		"\7r\2\2\u00bb\u00bc\7v\2\2\u00bc\u00bd\7k\2\2\u00bd\u00be\7q\2\2\u00be"+
		"\u00bf\7p\2\2\u00bf(\3\2\2\2\u00c0\u00c1\7u\2\2\u00c1\u00c2\7e\2\2\u00c2"+
		"\u00c3\7j\2\2\u00c3\u00c4\7g\2\2\u00c4\u00c5\7o\2\2\u00c5\u00c6\7c\2\2"+
		"\u00c6*\3\2\2\2\u00c7\u00c8\7u\2\2\u00c8\u00c9\7e\2\2\u00c9\u00ca\7c\2"+
		"\2\u00ca\u00cb\7n\2\2\u00cb\u00cc\7c\2\2\u00cc\u00cd\7t\2\2\u00cd,\3\2"+
		"\2\2\u00ce\u00cf\7v\2\2\u00cf\u00d0\7{\2\2\u00d0\u00d1\7r\2\2\u00d1\u00d2"+
		"\7g\2\2\u00d2.\3\2\2\2\u00d3\u00d4\7k\2\2\u00d4\u00d5\7p\2\2\u00d5\u00d6"+
		"\7v\2\2\u00d6\u00d7\7g\2\2\u00d7\u00d8\7t\2\2\u00d8\u00d9\7h\2\2\u00d9"+
		"\u00da\7c\2\2\u00da\u00db\7e\2\2\u00db\u00dc\7g\2\2\u00dc\60\3\2\2\2\u00dd"+
		"\u00de\7k\2\2\u00de\u00df\7o\2\2\u00df\u00e0\7r\2\2\u00e0\u00e1\7n\2\2"+
		"\u00e1\u00e2\7g\2\2\u00e2\u00e3\7o\2\2\u00e3\u00e4\7g\2\2\u00e4\u00e5"+
		"\7p\2\2\u00e5\u00e6\7v\2\2\u00e6\u00e7\7u\2\2\u00e7\62\3\2\2\2\u00e8\u00e9"+
		"\7g\2\2\u00e9\u00ea\7p\2\2\u00ea\u00eb\7w\2\2\u00eb\u00ec\7o\2\2\u00ec"+
		"\64\3\2\2\2\u00ed\u00ee\7w\2\2\u00ee\u00ef\7p\2\2\u00ef\u00f0\7k\2\2\u00f0"+
		"\u00f1\7q\2\2\u00f1\u00f2\7p\2\2\u00f2\66\3\2\2\2\u00f3\u00f4\7k\2\2\u00f4"+
		"\u00f5\7p\2\2\u00f5\u00f6\7r\2\2\u00f6\u00f7\7w\2\2\u00f7\u00f8\7v\2\2"+
		"\u00f88\3\2\2\2\u00f9\u00fa\7g\2\2\u00fa\u00fb\7z\2\2\u00fb\u00fc\7v\2"+
		"\2\u00fc\u00fd\7g\2\2\u00fd\u00fe\7p\2\2\u00fe\u00ff\7f\2\2\u00ff:\3\2"+
		"\2\2\u0100\u0101\7f\2\2\u0101\u0102\7k\2\2\u0102\u0103\7t\2\2\u0103\u0104"+
		"\7g\2\2\u0104\u0105\7e\2\2\u0105\u0106\7v\2\2\u0106\u0107\7k\2\2\u0107"+
		"\u0108\7x\2\2\u0108\u0109\7g\2\2\u0109<\3\2\2\2\u010a\u010b\7q\2\2\u010b"+
		"\u010c\7p\2\2\u010c>\3\2\2\2\u010d\u0111\t\2\2\2\u010e\u0110\t\3\2\2\u010f"+
		"\u010e\3\2\2\2\u0110\u0113\3\2\2\2\u0111\u010f\3\2\2\2\u0111\u0112\3\2"+
		"\2\2\u0112@\3\2\2\2\u0113\u0111\3\2\2\2\u0114\u0116\5E#\2\u0115\u0114"+
		"\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0117\3\2\2\2\u0117\u0118\5G$\2\u0118"+
		"B\3\2\2\2\u0119\u011b\5E#\2\u011a\u0119\3\2\2\2\u011a\u011b\3\2\2\2\u011b"+
		"\u011c\3\2\2\2\u011c\u0123\5G$\2\u011d\u011f\7\60\2\2\u011e\u0120\5M\'"+
		"\2\u011f\u011e\3\2\2\2\u0120\u0121\3\2\2\2\u0121\u011f\3\2\2\2\u0121\u0122"+
		"\3\2\2\2\u0122\u0124\3\2\2\2\u0123\u011d\3\2\2\2\u0123\u0124\3\2\2\2\u0124"+
		"\u0126\3\2\2\2\u0125\u0127\5K&\2\u0126\u0125\3\2\2\2\u0126\u0127\3\2\2"+
		"\2\u0127D\3\2\2\2\u0128\u0129\7/\2\2\u0129F\3\2\2\2\u012a\u0133\7\62\2"+
		"\2\u012b\u0133\5I%\2\u012c\u012e\5I%\2\u012d\u012f\5M\'\2\u012e\u012d"+
		"\3\2\2\2\u012f\u0130\3\2\2\2\u0130\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131"+
		"\u0133\3\2\2\2\u0132\u012a\3\2\2\2\u0132\u012b\3\2\2\2\u0132\u012c\3\2"+
		"\2\2\u0133H\3\2\2\2\u0134\u0135\4\63;\2\u0135J\3\2\2\2\u0136\u0138\t\4"+
		"\2\2\u0137\u0139\t\5\2\2\u0138\u0137\3\2\2\2\u0138\u0139\3\2\2\2\u0139"+
		"\u013b\3\2\2\2\u013a\u013c\5M\'\2\u013b\u013a\3\2\2\2\u013c\u013d\3\2"+
		"\2\2\u013d\u013b\3\2\2\2\u013d\u013e\3\2\2\2\u013eL\3\2\2\2\u013f\u0140"+
		"\4\62;\2\u0140N\3\2\2\2\u0141\u0146\7$\2\2\u0142\u0145\n\6\2\2\u0143\u0145"+
		"\5]/\2\u0144\u0142\3\2\2\2\u0144\u0143\3\2\2\2\u0145\u0148\3\2\2\2\u0146"+
		"\u0144\3\2\2\2\u0146\u0147\3\2\2\2\u0147\u0149\3\2\2\2\u0148\u0146\3\2"+
		"\2\2\u0149\u014a\7$\2\2\u014aP\3\2\2\2\u014b\u014c\7$\2\2\u014c\u014d"+
		"\7$\2\2\u014d\u014e\7$\2\2\u014e\u0150\3\2\2\2\u014f\u0151\5S*\2\u0150"+
		"\u014f\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0153\7$"+
		"\2\2\u0153\u0154\7$\2\2\u0154\u0155\7$\2\2\u0155R\3\2\2\2\u0156\u0159"+
		"\5U+\2\u0157\u0159\5W,\2\u0158\u0156\3\2\2\2\u0158\u0157\3\2\2\2\u0159"+
		"\u015a\3\2\2\2\u015a\u015b\3\2\2\2\u015a\u0158\3\2\2\2\u015bT\3\2\2\2"+
		"\u015c\u015d\7^\2\2\u015d\u015e\7$\2\2\u015e\u015f\7$\2\2\u015f\u0160"+
		"\7$\2\2\u0160V\3\2\2\2\u0161\u0162\t\17\2\2\u0162X\3\2\2\2\u0163\u0164"+
		"\t\20\2\2\u0164Z\3\2\2\2\u0165\u0169\7%\2\2\u0166\u0168\5Y-\2\u0167\u0166"+
		"\3\2\2\2\u0168\u016b\3\2\2\2\u0169\u0167\3\2\2\2\u0169\u016a\3\2\2\2\u016a"+
		"\u016c\3\2\2\2\u016b\u0169\3\2\2\2\u016c\u016d\b.\2\2\u016d\\\3\2\2\2"+
		"\u016e\u0171\7^\2\2\u016f\u0172\t\7\2\2\u0170\u0172\5_\60\2\u0171\u016f"+
		"\3\2\2\2\u0171\u0170\3\2\2\2\u0172^\3\2\2\2\u0173\u0174\7w\2\2\u0174\u0175"+
		"\5a\61\2\u0175\u0176\5a\61\2\u0176\u0177\5a\61\2\u0177\u0178\5a\61\2\u0178"+
		"`\3\2\2\2\u0179\u017a\t\b\2\2\u017ab\3\2\2\2\u017b\u017c\t\t\2\2\u017c"+
		"\u017d\3\2\2\2\u017d\u017e\b\62\3\2\u017ed\3\2\2\2\u017f\u0180\t\n\2\2"+
		"\u0180\u0181\3\2\2\2\u0181\u0182\b\63\3\2\u0182f\3\2\2\2\u0183\u0184\t"+
		"\13\2\2\u0184\u0185\3\2\2\2\u0185\u0186\b\64\3\2\u0186h\3\2\2\2\u0187"+
		"\u0188\t\f\2\2\u0188\u0189\3\2\2\2\u0189\u018a\b\65\3\2\u018aj\3\2\2\2"+
		"\u018b\u018c\t\r\2\2\u018c\u018d\3\2\2\2\u018d\u018e\b\66\3\2\u018el\3"+
		"\2\2\2\u018f\u0190\7.\2\2\u0190\u0191\3\2\2\2\u0191\u0192\b\67\3\2\u0192"+
		"n\3\2\2\2\u0193\u0194\t\16\2\2\u0194\u0195\3\2\2\2\u0195\u0196\b8\3\2"+
		"\u0196p\3\2\2\2\25\2\u0094\u0111\u0115\u011a\u0121\u0123\u0126\u0130\u0132"+
		"\u0138\u013d\u0144\u0146\u0150\u0158\u015a\u0169\u0171\4\2\4\2\2\5\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}