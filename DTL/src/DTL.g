/*
 * @author Menzo Windhouwer
 *
 *
 * This is the ANTLR grammar for the Data Transformation Language (DTL).
 * The specification defines both the Lexer and the Parser.
 */

/*
 * The header is replicated in each Java source code file generated from
 * the grammar spec.
 */
header {
	/* Put the code in the TDS DTL package */
	package nl.uu.let.languagelink.tds.dtl;

	import nl.uu.let.languagelink.tds.dtl.annotate.*;
	import nl.uu.let.languagelink.tds.dtl.context.*;
	import nl.uu.let.languagelink.tds.dtl.expression.*;
	import nl.uu.let.languagelink.tds.dtl.expression.Error;
	import nl.uu.let.languagelink.tds.dtl.map.*;
	import nl.uu.let.languagelink.tds.dtl.map.Map;
	import nl.uu.let.languagelink.tds.dtl.notion.*;

	import java.io.*;
	import java.net.URL;
	import java.net.MalformedURLException;
	import java.util.*;

	import antlr.TokenStreamSelector;
	import antlr.CharStreamException;
}

/**
 * The DTL Lexer.
 */

class DTLLexer extends Lexer;

options	{
			k=3;								// A lookahead of 2 should be enough
			filter=false;						// Don't allow unspecified tokens
			exportVocab=DTL;					// The export vocabulary is called DTL
			charVocabulary='\u0003'..'\uFFFE';	// The allowed character range
			testLiterals=false;					// Don't test literals
		}

tokens	{
			MARKER;								// MARKERs are dynamically defined
		}

{
	Engine engine = null;						// The Engine which uses this Lexer
	Reader reader = null;						// The Reader this lexer reads from
	
	/**
	 * DTL specific constructor which sets the Engine in use
	 */
	public DTLLexer(Reader r,Engine e) {
		this(r);
		reader = r;
		engine = e;
	}

	/**
	 * When we encounter an EOF we should check if we are busy
	 * processing an INCLUDE, if so we pop the current INCLUDE
	 * and go on with processing.
	 */
	public void uponEOF() throws TokenStreamException, CharStreamException {
		TokenStream ts = engine.selector.getCurrentStream();
		if (ts instanceof DTLLexer) {
			try {
				((DTLLexer)ts).reader.close(); // close the reader
			} catch (IOException e) {
            	Log.error(DTLLexer.class,"Couldn't close the stream: "+e);
			}
		}
		if (ts != engine.lexer ) {
			// don't allow EOF until main lexer.  Force the
			// selector to retry for another token.
			engine.selector.pop(); // return to old lexer/stream
			engine.selector.retry();
		}
	}

	/**
	 * set tabs to 4, just round column up to next tab + 1
	 * 12345678901234567890
	 *    x   x   x   x
	 */
	public void tab() {
		int t = 4;
		int c = getColumn();
		int nc = (((c-1)/t)+1)*t+1;
		setColumn( nc );
	}
}

BOM     : '\ufeff'
        ;
        
COMMENT	: '#' ( ~( '\r'|'\n' ) )* NL
		{
			$setType(Token.SKIP);
		}
		;

ID		options {
			testLiterals=true; // Turn on testing for literals so we don't reuse keywords as IDs
		}
		: LETTER ( LETTER | DIGIT | UNDERSCORE | MINUS )*
		{
			if (engine.isMarker(getText())) {
				$setType(MARKER); // This ID is a dynamically declared MARKER
            }
		}
		;

STRING	: '"'! ( NL | ESCAPE | ~('"'|'\\'|'\r'|'\n') )* '"'! // " ... keep syntax coloring happy
		;

INTEGER	: ( DIGIT )+
		;

/**
 * Embedded XML
 */
XML		: ( "<" ID ( WS ( ~('/'|'>') | ( '/' ~('>') ) )* )? ">" ) => "<" o:ID ( WS ( ~('/'|'>') | ( '/' ~('>') ) )* )? ">" ( e:XML | WS | ~('<'|'\t'|'\r'|'\n'|' ') )* "</" c:ID ">" { o.getText().equals(c.getText()) }?
		{
/*
			System.out.println("Found open/close XML element:"+getText());
			System.out.println(" open  tag["+o.getText()+"]");
			System.out.println(" close tag["+c.getText()+"]");
			if (e != null)
				System.out.println(" embedded:"+e.getText());
*/
		}
		| ( "<" ID ( WS ( ~('/'|'>') | ( '/' ~('>') ) )* )? "/>" ) => "<" s:ID ( WS ( ~('/'|'>') | ( '/' ~('>') ) )* )? "/>"
		{
/*
			System.out.println("Found single XML element:"+getText());
			System.out.println(" single tag:"+s.getText());
*/
		}
		;
        exception catch [SemanticException exception]
        {
		String msg = "XML fragment is not wellformed";
		TokenStream ts = engine.selector.getCurrentStream();
		if (ts instanceof DTLLexer) {
			DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
			msg += " at "+l.getFilename()+":"+l.getLine()+":"+l.getColumn();
		}
            	Log.error(DTLLexer.class,msg);
        }

/**
 * URLs
 *
 * TODO: URLs currently need always the AT prefix, hopefully we can get rid of this
 */
URL		: "@"! ( 'a'..'z' | 'A'..'Z' )+ ":" ( ~( ' '|'\t'|'\r'|'\n'|';' ) )+
		{
/*
			System.out.println("Found URL:"+getText());
*/
		}
		;

/**
 * Handle INCLUDEs
 *
 * Normally this rule would be part of the Parser, but by putting it in the Lexer
 * we can easily temporarily interrupt the current InputStream and replace it
 * with the INCLUDE InputStream.
 */
INCLUDE
		options {
			ignore=WS; // ignore whitespace between the various rule items
		}
		: "INCLUDE" s:STRING SEMICOLON
		{
			// create Lexer to handle INCLUDE
			String name = s.getText();
            if (engine.include(name)) {
              Reader r = engine.resolve(name);
              if (r!=null) {
                  try {
                      if (r.markSupported()) {
                          r.mark(1);
                          int bom = r.read(); // read the bom
                          if (bom != 0xfeff)
                                r.reset(); // no bom, so reset
                      }

                      DTLLexer sublexer = new DTLLexer(r,engine);
                      sublexer.setFilename(name);

                      engine.selector.addInputStream(sublexer,name); // add the INCLUDE Lexer to the selector
                      engine.selector.push(name); // push the INCLUDE Lexer on the selector stack
                      engine.selector.retry(); // throws TokenStreamRetryException, and the retry will access the INCLUDE InputStream
                  } catch(IOException e) {
			String msg = "couldn't open include file:"+name;
			TokenStream ts = engine.selector.getCurrentStream();
			if (ts instanceof DTLLexer) {
				DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
				msg += " at "+l.getFilename()+":"+l.getLine()+":"+l.getColumn();
			}
            		Log.error(DTLLexer.class,msg,e);
                  }
              } else {
		String msg = "couldn't open include file:"+name;
		TokenStream ts = engine.selector.getCurrentStream();
		if (ts instanceof DTLLexer) {
			DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
			msg += " at "+l.getFilename()+":"+l.getLine()+":"+l.getColumn();
		}
            	Log.error(DTLLexer.class,msg);
              }
            } else {
                Log.warning(DTLLexer.class,"skipped include file:"+name);
                engine.selector.retry();
            }
		}
		;

/**
 * Single character tokens
 *
 * Using those the Parser won't contain literals, except for the DTL keywords.
 */

LBRACE	: '{'
		;

RBRACE	: '}'
		;

LPAREN	: '('
		;

RPAREN	: ')'
		;

COLON	: ':'
		;

SEMICOLON
		: ';'
		;

COMMA	: ','
		;

DOT		: '.'
		;

UNDERSCORE
		: '_'
		;

MINUS	: '-'
		;

PLUS	: '+'
		;

EQ		: '='
		;

NEQ		: '!' '='
		;

SLASH	: '/'
		;

WS		:
		(
			  ' '
			| '\t'
			| NL
		)
		{
			$setType(Token.SKIP);
		}
		;


/**
 * Protected lexer rules
 */
protected
LOWER_ROMAN
		: '\u0041'..'\u005a';

protected
LETTER	: LOWER_ROMAN
		| '\u0061'..'\u007a'
		| '\u0e81'..'\u0e82'
		| '\u0e84'
		| '\u0e87'..'\u0e88'
		| '\u0e8a'
		| '\u0e8d'
		| '\u0e94'..'\u0e97'
		| '\u0e99'..'\u0e9f'
		| '\u0ea1'..'\u0ea3'
		| '\u0ea5'
		| '\u0ea7'
		| '\u0eaa'..'\u0eab'
		| '\u0ead'..'\u0eb0'
		| '\u0eb2'..'\u0eb3'
		| '\u0ebd'
		| '\u0ec0'..'\u0ec4'
		| '\u0ec6'
		| '\u0edc'..'\u0edd'
		| '\u0f00'
		| '\u0f40'..'\u0f47'
		| '\u0f49'..'\u0f6a'
		| '\u0f88'..'\u0f8b'
		| '\u1000'..'\u1021'
		| '\u1023'..'\u1027'
		| '\u1029'..'\u102a'
		| '\u1050'..'\u1055'
		| '\u10a0'..'\u10c5'
		| '\u10d0'..'\u10f6'
		| '\u1100'..'\u1159'
		| '\u115f'..'\u11a2'
		| '\u11a8'..'\u11f9'
		| '\u1200'..'\u1206'
		| '\u1208'..'\u1246'
		| '\u1248'
		| '\u124a'..'\u124d'
		| '\u1250'..'\u1256'
		| '\u1258'
		| '\u125a'..'\u125d'
		| '\u1260'..'\u1286'
		| '\u1288'
		| '\u128a'..'\u128d'
		| '\u1290'..'\u12ae'
		| '\u12b0'
		| '\u12b2'..'\u12b5'
		| '\u12b8'..'\u12be'
		| '\u12c0'
		| '\u12c2'..'\u12c5'
		| '\u12c8'..'\u12ce'
		| '\u12d0'..'\u12d6'
		| '\u12d8'..'\u12ee'
		| '\u12f0'..'\u130e'
		| '\u1310'
		| '\u1312'..'\u1315'
		| '\u1318'..'\u131e'
		| '\u1320'..'\u1346'
		| '\u1348'..'\u135a'
		| '\u13a0'..'\u13f4'
		| '\u1401'..'\u166c'
		| '\u166f'..'\u1676'
		| '\u1681'..'\u169a'
		| '\u16a0'..'\u16ea'
		| '\u1780'..'\u17b3'
		| '\u1820'..'\u1877'
		| '\u1880'..'\u18a8'
		| '\u1e00'..'\u1e9b'
		| '\u1ea0'..'\u1ef9'
		| '\u1f00'..'\u1f15'
		| '\u1f18'..'\u1f1d'
		| '\u1f20'..'\u1f45'
		| '\u1f48'..'\u1f4d'
		| '\u1f50'..'\u1f57'
		| '\u1f59'
		| '\u1f5b'
		| '\u1f5d'
		| '\u1f5f'..'\u1f7d'
		| '\u1f80'..'\u1fb4'
		| '\u1fb6'..'\u1fbc'
		| '\u1fbe'
		| '\u1fc2'..'\u1fc4'
		| '\u1fc6'..'\u1fcc'
		| '\u1fd0'..'\u1fd3'
		| '\u1fd6'..'\u1fdb'
		| '\u1fe0'..'\u1fec'
		| '\u1ff2'..'\u1ff4'
		| '\u1ff6'..'\u1ffc'
		| '\u207f'
		| '\u2102'
		| '\u2107'
		| '\u210a'..'\u2113'
		| '\u2115'
		| '\u2119'..'\u211d'
		| '\u2124'
		| '\u2126'
		| '\u2128'
		| '\u212a'..'\u212d'
		| '\u212f'..'\u2131'
		| '\u2133'..'\u2139'
		| '\u3005'..'\u3006'
		| '\u3031'..'\u3035'
		| '\u3041'..'\u3094'
		| '\u309d'..'\u309e'
		| '\u30a1'..'\u30fa'
		| '\u30fc'..'\u30fe'
		| '\u3105'..'\u312c'
		| '\u3131'..'\u318e'
		| '\u31a0'..'\u31b7'
		| '\u3400'..'\u4db5'
		| '\u4e00'..'\u9fa5'
		| '\ua000'..'\ua48c'
		| '\uac00'..'\ud7a3'
		| '\uf900'..'\ufa2d'
		| '\ufb00'..'\ufb06'
		| '\ufb13'..'\ufb17'
		| '\ufb1d'
		| '\ufb1f'..'\ufb28'
		| '\ufb2a'..'\ufb36'
		| '\ufb38'..'\ufb3c'
		| '\ufb3e'
		| '\ufb40'..'\ufb41'
		| '\ufb43'..'\ufb44'
		| '\ufb46'..'\ufbb1'
		| '\ufbd3'..'\ufd3d'
		| '\ufd50'..'\ufd8f'
		| '\ufd92'..'\ufdc7'
		| '\ufdf0'..'\ufdfb'
		| '\ufe70'..'\ufe72'
		| '\ufe74'
		| '\ufe76'..'\ufefc'
		| '\uff21'..'\uff3a'
		| '\uff41'..'\uff5a'
		| '\uff66'..'\uffbe'
		| '\uffc2'..'\uffc7'
		| '\uffca'..'\uffcf'
		| '\uffd2'..'\uffd7'
		| '\uffda'..'\uffdc'
		;

protected
ARABIC_DIGIT
		: '0'..'9'
		;

protected
NON_ARABIC_DIGIT
		: '\u0660'..'\u0669'
		| '\u06f0'..'\u06f9'
		| '\u0966'..'\u096f'
		| '\u09e6'..'\u09ef'
		| '\u0a66'..'\u0a6f'
		| '\u0ae6'..'\u0aef'
		| '\u0b66'..'\u0b6f'
		| '\u0be7'..'\u0bef'
		| '\u0c66'..'\u0c6f'
		| '\u0ce6'..'\u0cef'
		| '\u0d66'..'\u0d6f'
		| '\u0e50'..'\u0e59'
		| '\u0ed0'..'\u0ed9'
		| '\u0f20'..'\u0f29'
		| '\u1040'..'\u1049'
		| '\u1369'..'\u1371'
		| '\u17e0'..'\u17e9'
		| '\u1810'..'\u1819'
		| '\uff10'..'\uff19'
		;

protected
DIGIT	: ARABIC_DIGIT | NON_ARABIC_DIGIT
		;

protected
NL		:
		(
			options {
				generateAmbigWarnings=false;
			}
			: '\r''\n'
			| '\r'
			| '\n'
		)
		{
			newline();
		}
		;

protected
ESCAPE	:
		'\\'
		(
			'"' /* " */
			{
				$setText("\""); /* " */
			}
		|
			'\\'
			{
				$setText("\\");
			}
		)
		;

/**
 * The DTL Parser
 */
class DTLParser extends Parser;

options	{
			k=3;
			buildAST=false; // Build the AST, which is currently only used for debugging purposes.
			defaultErrorHandler=false; // don't generate default error handlers, so the first error is fatal
		}

tokens	{	// These tokens are used to group nodes within the AST
			DTL;
			PRS;
			LST;
			STR;
			INT;
			BIT;
			NIL;
			MRK;
			SID;
		}

{

	Engine engine = null;						// The Engine which uses this Parser
    
	/**
	 * DTL specific constructor which sets the used Engine
     */
	public DTLParser(TokenStream ts,Engine e) {
		this(ts);
		engine = e;
	}

	/**
	 * Convenience method to quickly throw an ERROR
     */
	private void throwException(String msg) throws RecognitionException {
		DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
		throw new RecognitionException(msg,l.getFilename(),l.getLine(),l.getColumn());
	}

	/**
	 * Convenience method to quickly show an WARNING
     */
	private void showWarning(String msg) {
		DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
        Log.warning(DTLParser.class,l.getFilename()+":"+l.getLine()+":"+l.getColumn()+" "+msg);
	}

	/**
	 * Overwrite the getFilename() method so we get the correct filename even
	 * when we're inside an INCLUDE.
	 */
	public String getFilename() {
		DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
		return l.getFilename();
	}
}

/**
 * The DTL specification
 * - global declarations, and
 * - a series of scopes
 */

dtlspec	: ( BOM )? ( global_declaration )* ( warehouse_scope | database_scope | scope )* EOF
		;
        
/**
 * Global declarations are reserved for universal truths, like the dynamic
 * declaration of markers and linking types.
 */
global_declaration
        {
			String    dt = null;
			Annotation a = null;
        }
		: "DECLARE" "ONTOLOGY" ( u:URL | s:STRING) SEMICOLON
		{
			if (u!=null) {
				try {
					engine.loadOntology(""+(new URL(u.getText())));
				} catch (MalformedURLException e) {
					throwException("the ontology URL["+u.getText()+"] is malformed:"+e);
				}
			} else
				engine.loadOntology(s.getText());
		}
		| "DECLARE"	"MARK" m:ID ( a=annotate )? SEMICOLON
		{
			engine.addMarker(m.getText(),a);
		}
		| "DECLARE"	"LINKING" "TYPE" t:ID ( a=annotate )? SEMICOLON
		{
			engine.addLinkingType(t.getText(),a);
		}
		| "DECLARE"	"HINT" h:ID "TYPE" ( "IS" )? dt=datatype SEMICOLON
		{
			engine.addHint(h.getText(),dt);
		}
        | "DECLARE" "TYPE" nt:ID "BASE" "TYPE" bt:ID SEMICOLON
        {
            if (!engine.addDataType(nt.getText(),bt.getText())) {
                if (!engine.hasDataType(bt.getText()))
                    throwException("the type["+nt.getText()+"] can't be declared, its base type["+bt.getText()+"] doesn't exist");
                if (!engine.getBaseDataType(nt.getText()).equals(bt.getText()))
                    throwException("Duplicate declaration of the type["+nt.getText()+"], but base type["+bt.getText()+"] is different (base type["+engine.getBaseDataType(nt.getText())+"] of previous declaration)");
                if (!engine.hasDataType(nt.getText()))
                    showWarning("Duplicate declaration of the type["+nt.getText()+"], this declaration will be ignored");
            }
        }
		;

/**
 * The warehouse scope is the most top level scope (when a hierarchy of scopes is used)
 * and describes the combination of all database scopes. There can be only one warehouse
 * scope.
 */
warehouse_scope
        : "WAREHOUSE" id:ID
		{
			Scope s = engine.getWarehouse();
			if (s!=null)
			    throwException("can't create scope["+id.getText()+"], as there is already a warehouse scope: "+s);
			Scope scope = engine.newScope(id.getText());
			if (scope==null)
				throwException("can't create scope["+id.getText()+"]");
			if (!engine.pushScope(scope))
				throwException("circular dependency on "+scope);
			scope.setType("warehouse");
		}
        LBRACE ( local_declaration | variable )* ( scope | database_scope )* RBRACE
		{
			engine.popScope();
		}
        ;

/**
 * A database scope describes the data of one database. Its declarations provide information
 * on how to access and preprocess the data loaded from this database. Its the only type of scope
 * with a root notion. This notion is the start of the declarative description on how the data is
 * to be transformed, one of the main purposes of the whole DTL specification.
 */
database_scope
        : "DATABASE" id:ID
		{
			Scope scope = engine.newScope(id.getText());
			if (scope==null)
				throwException("can't create scope["+id.getText()+"]");
			if (!engine.pushScope(scope))
				throwException("circular dependency on "+scope);
			scope.setType("database");
		}
        LBRACE ( local_declaration | database_declaration | context_declaration )* ( notion | query )* RBRACE
		{
			engine.popScope();
		}
        ;

/**
 * A normal scope exists as a place to collect thematic notions which can span accross
 * several database scopes.
 */
scope   : "SCOPE" id:ID
		{
			Scope scope = engine.newScope(id.getText());
			if (scope==null)
				throwException("can't create scope["+id.getText()+"]");
			if (!engine.pushScope(scope))
				throwException("circular dependency on "+scope);
		}
        LBRACE ( local_declaration | variable )* ( scope | database_scope )* RBRACE
		{
			engine.popScope();
		}
        ;
        
/**
 * Local declarations are valid within the current scope, and are mostly of a descriptive nature.
 */
local_declaration
		{
			Description	d	= null;
		}
		: "DECLARE" "NAMESPACE" ns:STRING SEMICOLON
		{
			if (!engine.getCurrentScope().setNamespace(ns.getText()))
				throwException("namespace already declared");
		}
		| "DECLARE"	"ABBREVIATION" s:STRING SEMICOLON
		{
			if (!engine.getCurrentScope().setAbbreviation(s.getText()))
				throwException("abbreviation already declared");
		}
		| "DECLARE" "NAME" n:STRING SEMICOLON
		{
			if (!engine.getCurrentScope().setFullname(n.getText()))
		    	throwException("name already declared");
		}
		| "DECLARE"	"DESCRIPTION" d=description SEMICOLON
		{
			if (!engine.getCurrentScope().setDescription(d))
				throwException("description already declared");
		}
		| "DECLARE"	"NOTES" d=description SEMICOLON
		{
			if (!engine.getCurrentScope().setNotes(d))
				throwException("notes already declared");
		}
		| "DECLARE"	"DEFAULT" "TYPE" dt:ID SEMICOLON
		{
            if (engine.hasDataType(dt.getText())) {
                if (!engine.getCurrentScope().setDefaultDataType(dt.getText()))
	    			throwException("default type couln't be set to type["+dt.getText()+"]");
            } else
                throwException("default type["+dt.getText()+" is unknown");
		}
		| declare_map
		| declare_notion
		;

/**
 * Database declarations are valid within the current scope, and are mainly to describe
 * the way to access the database.
 */
database_declaration
		{
			Map 		m	= null;
			Expression	e	= null;
		}
		: "DECLARE" "RESEARCHER" r:STRING SEMICOLON
		{
			if (!engine.getCurrentScope().addResearcher(r.getText()))
				throwException("researcher couldn't be added");
		}
		| "DECLARE" "CONTACT" c:STRING SEMICOLON
		{
			if (!engine.getCurrentScope().addContact(c.getText()))
				throwException("contact couldn't be added");
		}
		| "DECLARE"	"WEBSITE" w:URL SEMICOLON
		{
			try {
				URL site = new URL(w.getText());
				if (!engine.getCurrentScope().setWebsite(site))
					throwException("website already declared");
			} catch(MalformedURLException ex) {
				throwException("website URL is malformed: "+ex);
			}
		}
		| "DECLARE" "ACCESS" ( i:ID )?  a:XML SEMICOLON
		{
			try {
				if (!engine.getCurrentScope().setAccess((i!=null?i.getText():engine.getCurrentScope().getName()),engine.toXML(a.getText())))
					throwException("access already declared, or accessor type is unknown");
			} catch(Exception ex) {
				throwException("invalid XML fragment:"+ex);
			}
		}
		| declare_query
		;

/**
 * Context declarations define how the data from an (implicit) query
 * should be preprocessed or are used to define a variable.
 */

context_declaration
		: ( skip | preprocess | variable )
		;
        
variable
		{
			Variable v = null;
			Expression expr = null;
		}
        : "VARIABLE" i:ID "IS" expr=value_expression SEMICOLON
		{
			v = engine.getCurrentContext().addVariable(i.getText(),expr);
			if (v==null)
				throwException("couldn't create variable["+i.getText()+"]");
		}
		;

skip    {
            Set fields = null;
        }
        : "SKIP" fields=field_references SEMICOLON
        {
            engine.getCurrentContext().addSkipList(fields);
        }
        ;
 
preprocess
        {
            Map map = null;
            Set fields = null;
            Expression expr = null;
            String pattern = null;
            String datatype = null;
        }
        :
        {
            engine.setFocus(new FocusVariable());
        }
        "PREPROCESS" ( "DERIVE" s:STRING )? 
       
        (
            "USE" map=map_reference
            {
                if (map.parameterized()) {
                    if (map.getSize()==1) {
                        Map m = map.useMap(engine.getCurrentScope());
                        if (m==null)
                            throwException("can't use "+map+" for a preprocessing step");
                        map = m;
                    } else {
                        map.check();
                        throwException("can't use "+map+" in a preprocessing step, as not all its parameters can be resolved");
                    }
                }
                datatype = map.getDataType();
                expr = new Lookup(map);
            }
        | 
            expr=value_expression 
        )
        "FOR" ( ( ( all:"ALL" | other:"OTHER" )? "FIELDS" ) | ( "ALL" x:"EXCEPT" )? fields=field_references ) SEMICOLON
        {
            Preprocess pp = null;
            if (s!=null)
                pattern = s.getText();
            if ((fields!=null) && (x==null))
                pp = new Preprocess(pattern,expr,fields);
            else if ((fields!=null) && (x!=null))
                pp = new Preprocess(pattern,expr,true,fields);
            else
                pp = new Preprocess(pattern,expr,(all!=null));
            if (datatype!=null)
                pp.setDataType(datatype);
            engine.getCurrentContext().addPreprocess(pp);
            engine.clearFocus();
        }
        ;

/**
 * Declare a new map.
 */
declare_map
	{
		ID id  = null;
		List p = null;
	}
	: "DECLARE" "MAP" id=scoped_ID ( p=map_params )?
	{
		if (id.hasScope()) {
			if (engine.getCurrentScope()!=id.scope)
				throwException("maps can only be declared within their own scope, the current "+engine.getCurrentScope()+" is not the scope of the map["+id+"]");
		} else
			id.scope = engine.getCurrentScope();
		BaseMap m = engine.addMap(id.name,(p!=null?(List)p.get(0):null));
		if (m==null)
			throwException("map["+id+"] couldn't be created, it already exists");
	}
	( map_type )?
	map_body
	{
		if (p!=null) {
			boolean translate = false;
			List params = (List)p.get(0);
			List defs   = (List)p.get(1);
			for (ListIterator iter = params.listIterator();iter.hasNext();) {
				Expression val = (Expression)defs.get(iter.nextIndex());
				Param    param = (Param)iter.next();
				if (param != val)
					translate = true;
			}
			if (translate) {
				m = m.translateMap((List)p.get(1));
				if (m!=null)
					engine.replaceMap(id.name,m);
			}
		}
		engine.clearCurrentMap();
	}
	;
        
/**
 * Declare the parameters of a map.
 */
map_params
        returns [ List l ]
	{
		Expression val = null;
		Param param = null;
            
		List params = new Vector();
		List defs = new Vector();
            
		l = new Vector();
		l.add(params);
		l.add(defs);
		engine.setFocus(new FocusVariable());
	}
	: LPAREN
	(
		h:ID ( "IS" val=value_expression )?
		{
			param = new Param(h.getText());
			params.add(param);
			if (val==null)
				val = param;
			defs.add(val);
			val=null;
		}
		(
			COMMA t:ID ( "IS" val=value_expression )?
			{
				param = new Param(t.getText());
				params.add(param);
				if (val==null)
					val = param;
				defs.add(val);
				val=null;
			}
		)*
	)?
	RPAREN
        {
		engine.clearFocus();
	}
	;
        
/**
 * Declare the datatype of a map.
 **/
map_type: "RETURNS" dt:ID
        {
            if (engine.hasDataType(dt.getText()))
                engine.getCurrentMap().setDataType(dt.getText());
            else
                throwException(""+engine.getCurrentMap()+" refers to an unknown type["+dt.getText()+"]");
        }
        ;

/**
 * Declare the body of a map.
 */
map_body
        {
            boolean clean_focus = false;
        }
        :
        {
            if (!engine.hasFocus() && (engine.getCurrentMap()!=null) && (engine.getCurrentMap().getSize()==1)) {
                engine.setFocus(engine.getCurrentMap().getParam(0));
                clean_focus = true;
            }
        }
        LBRACE! ( map_import )* ( map_item | map_check )* ( map_otherwise )? ( map_clash )? RBRACE!
        {
            if (clean_focus)
                engine.clearFocus();
        }
        ;
        
map_check
        {
			Message m = null;
			Expression	e = null;
		}
        : ( m=message | m=warning | m=error ) ( "FOR" | "WHEN" ) e=boolean_expression SEMICOLON
        {
		    engine.getCurrentMap().addCheck(m,e);
        }
        ;
        
map_import
		{
			Map  m = null;
			List p = null;
		}
		: "IMPORT" m=map_reference SEMICOLON
		{
			if (!engine.getCurrentMap().importMap(m))
				throwException(""+m+" couldn't be imported");
		}
		;

map_item
		{
			Value		v = null;
			Expression	e = null;
			Annotation	a = null;
		}
		: ( "COPY" )? ( v=value_expression )? ( ( "FOR" | "WHEN" ) e=boolean_expression )? ( a=annotate )? SEMICOLON
		{
			if (v==null) {
                if (engine.hasFocus())
                    v = engine.getFocus();
                else
    			    throwException("don't know which value to COPY");
            }
            if (e==null) {
                if (engine.hasFocus())
                    e=new Equals(engine.getFocus(),v);
                else
                    throwException("FOR/WHEN:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			if (!engine.getCurrentMap().addMapping(v,e,a))
                throwException("mapping to "+v+" couldn't be created");
		}
		| "IGNORE" ( "FOR" | "WHEN" ) e=boolean_expression SEMICOLON
		{
			v = new Ignore();
			if (!engine.getCurrentMap().addMapping(v,e))
                throwException("mapping to "+v+" couldn't be created");
		}
		;

map_otherwise
		{
			Expression		e = null;
            Annotation      a = null;
			Message			m = null;
			FieldVariable	f = null;
		}
		: "OTHERWISE"
        (
            ( "COPY" )? ( e=value_expression )? ( a=annotate )? ( m=warning | m=message )?
            {
                if (e==null) {
                    if (engine.hasFocus())
                        e = engine.getFocus();
					else
						throwException("don't know which value to COPY");
				}
                if ((a!=null) && (e==null))
                    throwException("don't know which value to annotate");
                if (engine.getCurrentMap()!=null)
                    if (!engine.getCurrentMap().setOtherwise(e,a,m))
                    	throwException("couldn't fully initialize OTHERWISE");
			}
        |
            "IGNORE" ( m=warning | m=message )?
            {
				e = new Ignore();
                if (!engine.getCurrentMap().setOtherwise(e,null,m))
                    throwException("couldn't fully initialize OTHERWISE");
            }
        |
            m=error
            {
			    if (!engine.getCurrentMap().setOtherwise(m))
                    throwException("couldn't fully initialize OTHERWISE");
            }
        )
        SEMICOLON
        ;

map_clash
        {
			Message m = null;
		}
		: "CLASH" ( m=error | "ALLOW" ) SEMICOLON
		{
            engine.getCurrentMap().setClash(m); // null means allow multiple matches
		}
		;

/**
 * Declare a new notion.
 */
declare_notion
        : "DECLARE" declare_inner_notion
        ;

declare_inner_notion
		{
			ID			id	= null;
			Annotation	ann	= null;
            boolean     sc  = true;
            ID          sp  = null;
			Literal     hv  = null;
		}
		: ( o:"OPTIONAL" )? ( ( ( ar:"ABSTRACT" )? r:"ROOT" ) | t:"TOP" | c:"COMPLETE" | a:"ANNOTATION" )? "NOTION" id=scoped_ID
		{
            int has_type = 0;
            if (r!=null)
                has_type = Notion.ROOT;
            if (t!=null)
                has_type = Notion.TOP;
            if (c!=null)
                has_type = Notion.COMPLETE;
            if (a!=null)
                has_type = Notion.ANNOTATION;
            boolean is_abs = (ar!=null);
            boolean is_opt = (o!=null);
            if (has_type<=Notion.ANNOTATION) {
                if (!engine.getCurrentScope().getType().equals("warehouse")) {
                    Scope wh = engine.getWarehouse();
                    throwException("an annotation notion can only be declared in "+(wh!=null?wh:"a warehouse scope"));
                }
            }
            if (is_abs && (has_type!=Notion.ROOT))
                throwException("only abstract root notions are currently supported");
			DeclaredNotion notion = engine.getDeclaredNotion(has_type,id);
			if (notion==null) {
				if (id.hasScope()) {
					if (engine.getCurrentScope()!=id.scope)
						throwException("notions can only be declared within their own scope, the current "+engine.getCurrentScope()+" is not the scope of the notion["+id+"]");
				} else
					id.scope = engine.getCurrentScope();
				notion = engine.addDeclaredNotion(has_type,id);
				if (notion==null)
					throwException("notion["+id+"] couldn't be created (is it a duplicate?)");
                notion.setAbstract(is_abs);
			} else {
				if (!notion.checkProperties(has_type,is_opt,is_abs))
					throwException(""+notion+" is wrongly referenced");
				if (engine.addDeclaredNotion(notion)==null)
					throwException(""+notion+" can't be added (is it a duplicate?)");
			}
			engine.pushDeclaredNotion(notion);
		}
        ( "SUPER" "IS" ( ( "ROOT" )? "NOTION" )? sp=scoped_ID )?
        {
            if (sp!=null) {
              Notion n = engine.getCurrentDeclaredNotion();
              if (!n.isRoot())
                  throwException(""+n+" isn't a root notion so can't be a sub notion");
              DeclaredNotion sup = engine.findRootNotion(sp);
              if (sup==null)
                  throwException(""+n+" can't be a sub notion of notion["+sp+"], this super notion doesn't exist");
              else if (!sup.isAbstract())
                  throwException(""+n+" can't be a sub notion of non-abstract notion["+sp+"]");
              if (!n.setSuper(sup)) {
                  if (n.getSuper()!=null)
                      throwException(""+n+" can't be a sub notion of notion["+sup+"], it's already a sub notion of "+n.getSuper());
                  else
                      throwException(""+n+" can't be a sub notion of notion["+sup+"]");
              }
            }
        }
        ( ann=annotate )?
        {
            if (ann!=null && !ann.isEmpty()) {
                Notion n = engine.getCurrentDeclaredNotion();
                if ((n.getAnnotation()!=null) && !n.getAnnotation().isEmpty())
                    throwException(""+n+" has already been annotated: "+n.getAnnotation());
                if(!notion.addAnnotation(ann))
                    throwException(""+n+" couldn't be annotated");
            }
        }
		(
			"HINT" h:ID "IS" hv=literal
			{
				if (engine.isHint(h.getText())) {
					if (engine.allowedHintValue(h.getText(),hv)) {
						engine.getCurrentDeclaredNotion().addHint(h.getText(),hv);
					} else
                		throwException("value of HINT["+h.getText()+"] for "+engine.getCurrentDeclaredNotion()+" is of the wrong type");
				} else
                	throwException(""+engine.getCurrentDeclaredNotion()+" refers to an unknown hint["+h.getText()+"]");
			}
		)*
        (
            "TYPE" "IS" dt:ID
            {
                if (engine.hasDataType(dt.getText())) {
                    if (!engine.getCurrentDeclaredNotion().setDataType(dt.getText()))
                        throwException(""+engine.getCurrentDeclaredNotion()+" already has a type["+engine.getCurrentDeclaredNotion().getDataType()+"]");
                } else
                    throwException(""+engine.getCurrentDeclaredNotion()+" refers to an unknown type["+dt.getText()+"]");
            }
        )?
        ( declare_notion_map {sc=false;} )? ( declare_notion_group {sc=false;} )? ( declare_notion_end ( SEMICOLON )? | ( {sc}? SEMICOLON | {!sc}? ) )
		{
			engine.popDeclaredNotion();
		}
		;

declare_notion_map
        {
            Value      val = null;
            Annotation ann = null;
        }
        : "USE" "MAP" LBRACE
        (
            (v:"VALUE" | k:"KEY" ) val=literal ann=annotate SEMICOLON
            {
                if (v!=null) {
                    if (!engine.getCurrentDeclaredNotion().addValue(val,ann))
                        throwException("couldn't declare "+v+" as value for "+engine.getCurrentDeclaredNotion());
                } else {
                    if (!engine.getCurrentDeclaredNotion().addKey(val,ann))
                        throwException("couldn't declare "+v+" as key value for "+engine.getCurrentDeclaredNotion());
                }
            }
        )+
        RBRACE
        ;

declare_notion_group
        :
        {
            if (engine.getCurrentDeclaredNotion().isAnnotation())
                throwException(""+engine.getCurrentDeclaredNotion()+" is an annotation notion, which can't have member notions");
        }
        "GROUPS" LBRACE ( declare_inner_notion )+ RBRACE
		;

declare_notion_end
        {
            ID id = null;
        }
        : "END" "DECLARE" ( o:"OPTIONAL" )? ( ( ( ar:"ABSTRACT" )? r:"ROOT" ) | t:"TOP" | c:"COMPLETE" | a:"ANNOTATION" )? "NOTION" id=scoped_ID
        {
            int has_type = 0;
            if (r!=null)
                has_type = Notion.ROOT;
            if (t!=null)
                has_type = Notion.TOP;
            if (c!=null)
                has_type = Notion.COMPLETE;
            if (a!=null)
                has_type = Notion.ANNOTATION;
            boolean is_abs = (ar!=null);
            boolean is_opt = (o!=null);
            if (id.scope==null)
                id.scope=engine.getCurrentScope();
            if ((engine.getCurrentDeclaredNotion().getID().scope!=id.scope) || !engine.getCurrentDeclaredNotion().getID().name.equals(id.name))
                throwException("Expecting to end declare notion["+id+"], but in fact we're closing "+engine.getCurrentLocalizedNotion()+". The curly braces are probably not aligned.");
            if ((r!=null) || (t!=null) || (c!=null) || (o!=null) || (a!=null)) // if any property should hold, check it
                if (!engine.getCurrentDeclaredNotion().checkProperties(has_type,is_opt,is_abs))
                    showWarning("The END DECLARE NOTION statement wrongly references "+engine.getCurrentDeclaredNotion());
        }
        ;
/**
 * Declare a query to import data from the database
 */

declare_query
		: "DECLARE" "QUERY" id:ID
        (
          "ON" a:ID
        )?
        {
            Query q = engine.addQuery(id.getText(),(a!=null?a.getText():engine.getCurrentScope().getName()));
            if (q==null)
                throwException("couldn't create query["+id+"]");
            engine.pushQuery(q);
        }
        "IS" query_body  ( query_context | SEMICOLON )
        {
            engine.popQuery();
        }
		;

query_body
        {
            Expression expr = null;
            Set q = null;
            Set f = null;
            Set i = null;
            Expression e = null;
            Expression re = null;
            Literal n = null;
        }
		:
        (
            (
                expr=value_expression
                {
                    engine.getCurrentQuery().addBody(expr);
                }
                (
                    PLUS expr=value_expression
                    {
                        engine.getCurrentQuery().addBody(expr);
                    }
                )*
            )
        |
            "MERGE" LPAREN q=query_references COMMA f=field_references ( COMMA i=field_references )? RPAREN
            {
                engine.getCurrentQuery().setCommand(new Merge(q,f,i));
            }
        |
            "SPLIT" LPAREN e=value_expression COMMA re=value_expression COMMA n=literal RPAREN
            {
                engine.getCurrentQuery().setCommand(new Split(e,re,n.getValue().toString()));
            }
        )
		;

query_context
		: LBRACE ( context_declaration )+ RBRACE
		;

/**
 * Describe a transformation context in the form of a notion.
 */
notion
		{
			ID  		id	= null;
			DeclaredNotion 	sp 	= null;
			Expression	k	= null;
			boolean		inv	= false;
			Annotation	ann	= null;
			boolean    	sc  	= true;
			engine.clearFocus();
		}
		: ( o:"OPTIONAL" )? ( r:"ROOT" | t:"TOP" | c:"COMPLETE" | ( g:"GENERAL" )? a:"ANNOTATION" )? "NOTION" id=scoped_ID
		  ( sp=notion_super )?
		{
			LocalizedNotion n = null;
			int has_type = 0;
			if (r!=null)
				has_type = Notion.ROOT;
			if (t!=null)
				has_type = Notion.TOP;
			if (c!=null)
				has_type = Notion.COMPLETE;
			if (a!=null)
				has_type = Notion.ANNOTATION;
			boolean is_opt = (o!=null);
			boolean is_gen = (g!=null);
			if ((engine.getCurrentLocalizedNotion()==null) && (has_type!=Notion.ROOT))
				throwException("the outer notions in "+engine.getCurrentScope()+" should be a root notion");
			if ((has_type!=Notion.ROOT) && (sp!=null))
				throwException("only root notions are allowed to have a super notion");
			InstantiatedNotion in = engine.getInstantiatedNotion(id,(has_type>=Notion.TOP));
			if (in==null) {
				DeclaredNotion dn = engine.findDeclaredNotion(id,(has_type>=Notion.TOP));
				Log.debug(DTLParser.class,"declared notion:"+(dn!=null?dn:"<NULL>"));
				if (dn!=null) {
					if (!dn.checkProperties(has_type,is_opt))
						throwException(""+dn+" is wrongly referenced");
					if (dn.getSuper()!=null) {
						if ((sp!=null) && (dn.getSuper()!=sp))
							throwException(""+dn+" has been declared with a different super notion ("+dn.getSuper()+"), so you can't assign a new one ("+sp+")");
					} else {
						if (sp!=null)
							throwException(""+dn+" has been declared without a super notion, so you can't assign one ("+sp+")");
					}
					if (dn.isAbstract())
						throwException(""+dn+" can't directly instantiate abstract notions, you can only use them as super notions");
					in = engine.addInstantiatedNotion(dn,(has_type>=Notion.TOP));
					Log.debug(DTLParser.class,"instantiated notion:"+(in!=null?in:"<NULL>")+" based on "+dn);
				} else {
					if (a!=null)
						throwException("an annotation notion should always be declared in the warehouse scope");
					if (id.hasScope()) {
						if (engine.getCurrentScope()!=id.scope)
							throwException("new notions can only be created within their own scope, the current "+engine.getCurrentScope()+" is not the scope of the new notion["+id+"]");
					} else
						id.scope = engine.getCurrentScope();
					if (sp!=null) {
						in = engine.addInstantiatedNotion(id,sp);
						Log.debug(DTLParser.class,"instantiated notion:"+(in!=null?in:"<NULL>")+" sub notion of "+sp);
					} else {
						in = engine.addInstantiatedNotion(id,(has_type>=Notion.TOP));
						in.setType(has_type);
					}
				}
				if (in==null)
					throwException("couldn't create notion["+id+"]");
			} else {
				if (!in.checkProperties(has_type,is_opt))
					throwException(""+in+" is wrongly referenced");
				if (in.getSuper()!=null) {
					if ((sp!=null) && (in.getSuper()!=sp))
						throwException(""+in+" has been used with a different super notion ("+in.getSuper()+"), so you can't assign a new one ("+sp+")");
				} else {
					if (sp!=null)
						throwException(""+in+" has been used without a super notion, so you can't assign one ("+sp+")");
				}
				if (!engine.addInstantiatedNotion(in))
					throwException("couldn't add "+in+" as member");
			}
			Log.debug(DTLParser.class,"instantiated notion:"+(in!=null?in:"<NULL>"));
			n = engine.addLocalizedNotion(in);
			n.setOptional(is_opt);
			n.setGeneral(is_gen);
			n.addScope(engine.getCurrentScope());
			engine.pushLocalizedNotion(n);
		}
		( inv=notion_inverse )?
		{
			n = engine.getCurrentLocalizedNotion();
			if (n.isRoot() && inv) {
				List p = engine.getLocalizationPath();
				if (p.size()>1) {
					for (int i=p.size()-2;i>=0;i--) {
						in = ((LocalizedNotion)p.get(i)).getInstantiationContext();
						if (in.isRoot())
							break;
					}
					n.getInstantiationContext().addMember(in);
					n.createInverse(true);
				} else
					throwException("only nested root notions can have an inverse");
			} else if (inv)
				throwException("only root notions can have an inverse");
		}
		( notion_key )?
		( ann=annotate )?
		{
			in = engine.getCurrentInstantiatedNotion();
			if (ann!=null && !ann.isEmpty()) {
				if (engine.getCurrentScope()!=in.getScope())
					throwException(""+in+" can't be annotated outside its own scope, the current "+engine.getCurrentScope()+" is not the scope of the notion");
				if(!in.addAnnotation(ann))
					throwException(""+in+" couldn't be annotated");
			}
		}
		( notion_hints )?
		( notion_type )? ( notion_is )? ( notion_map_reference | ( notion_map_body {sc=false;} ) )? ( notion_when {sc=true;} )? ( notion_group {sc=false;} )* ( notion_end ( SEMICOLON )? | ( {sc}? SEMICOLON | {!sc}? ) )
		{
			n = engine.getCurrentLocalizedNotion();
			if (!n.isOptional() && n.isRoot() && !n.hasKey())
				throwException("only optional root notions are allowed to have no key");
			engine.popLocalizedNotion();
			engine.clearCurrentMap();
			engine.clearFocus();
		}
		;

notion_key
		: "KEY"
        {
            LocalizedNotion n = engine.getCurrentLocalizedNotion();
			Expression e = null;
            Annotation ann = null;
            Annotation a = null;
            Map m = null;
        }
        (
          (
            "IS" e=value_expression 
            (
              "KEY" a=annotation
              {
                if (ann==null)
                    ann = new Annotation();
				if (!ann.merge(a))
					throwException("couldn't create annotation");
              }
            )*
          )
        | 
          (
            "USE" m=map_reference
            {
			    if (!n.setKeyMap(m))
				    throwException("couldn't use "+m+" as key map for "+n);
                e = new Lookup(m);
            }
          )
        )
        {
			if (e!=null)
				if(!n.setKey(e,ann))
					throwException("couldn't use key expression["+e+"] for "+n);
        }
		;
        
notion_super
        returns [ DeclaredNotion n ]
		{
            n = null;
            ID sp  = null;
		}
        : "SUPER" "IS" ( ( "ROOT" )? "NOTION" )? sp=scoped_ID
        {
            n = engine.findRootNotion(sp);
            if (n==null)
                throwException("can't use "+n+" as a super notion");
            else if (!n.isAbstract())
                throwException("can't use non-abstract "+n+" as a super notion");
        }
        ;

notion_inverse
	returns [ boolean create_inverse ]
	{
		create_inverse = false;
	}
	: "CREATE" "INVERSE"
	{
		create_inverse = true;
	}
	;

notion_hints
		{
			Literal l = null;
		}
		:
		(
			"HINT" h:ID "IS" l=literal
			{
				if (engine.isHint(h.getText())) {
					if (engine.allowedHintValue(h.getText(),l)) {
						engine.getCurrentInstantiatedNotion().addHint(h.getText(),l);
					} else
                		throwException("value of HINT["+h.getText()+"] for "+engine.getCurrentInstantiatedNotion()+" is of the wrong type");
				} else
                	throwException(""+engine.getCurrentInstantiatedNotion()+" refers to an unknown hint["+h.getText()+"]");
			}
		)+
		;

notion_type
		: "TYPE" "IS" t:ID
        {
            if (engine.hasDataType(t.getText())) {
                if (!engine.getCurrentInstantiatedNotion().setDataType(t.getText()))
                    throwException(""+engine.getCurrentInstantiatedNotion()+" already has a type["+engine.getCurrentInstantiatedNotion().getDataType()+"]");
            } else
                throwException(""+engine.getCurrentInstantiatedNotion()+" refers to an unknown type["+t.getText()+"]");
        }
		;

notion_is
		{
			Value v = null;
            Annotation a = null;
		}
		: ( "VALUE" )? "IS" v=value_expression ( ( "VALUE" )? a=annotate )?
		{
			if (!engine.getCurrentLocalizedNotion().setBaseValue(v,a))
				throwException("couldn't set "+v+" as value for "+engine.getCurrentLocalizedNotion());
            engine.setFocus(new FocusVariable());
		}
		;

notion_map_reference
		{
			Map m = null;
		}
		: "USE" m=map_reference
		{
            if ((m.getSize()==1) && engine.getCurrentLocalizedNotion().hasBaseValue()) {
                UseMap vm = m.useMap(engine.getCurrentScope(),engine.getCurrentLocalizedNotion().getBaseValue());
                if (vm==null)
    				throwException("couldn't use "+m+" for "+engine.getCurrentLocalizedNotion());
                m = vm;
            }
			if (!engine.getCurrentLocalizedNotion().setValueMap(m))
				throwException("couldn't use "+m+" for "+engine.getCurrentLocalizedNotion());
            engine.setFocus(new FocusVariable());
		}
		;
        
notion_map_body
        {
            DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
            String name = "m-l"+l.getLine()+"-c"+l.getColumn();
			if (engine.addMap(name,null)==null)
				throwException("local map for "+engine.getCurrentLocalizedNotion()+" couldn't be created, it already exists");
		}
        : "USE" "MAP" map_body
        {
			if (!engine.getCurrentLocalizedNotion().setValueMap(engine.getCurrentMap()))
				throwException("couldn't use "+engine.getCurrentMap()+" for "+engine.getCurrentLocalizedNotion());
            engine.setFocus(new FocusVariable());
        }
		;

notion_when
		{
			Expression e = null;
		}
		: "WHEN" ( e=boolean_expression | "HAS" )
		{
			if ((e!=null) && !engine.getCurrentLocalizedNotion().setPreCondition(e))
				throwException("couldn't set the precondition for "+engine.getCurrentLocalizedNotion());
            else if ((e==null) && !engine.getCurrentLocalizedNotion().checkMembers(true))
                throwException("couldn't set the enable member check for "+engine.getCurrentLocalizedNotion());
		}
		;

notion_group
		{
            Expression e = null;
            Query q = null;
		}
		:
        {
            if (engine.getCurrentLocalizedNotion().isAnnotation())
                throwException(""+engine.getCurrentLocalizedNotion()+" is an annotation notion, which can't have member notions");
            engine.getCurrentLocalizedNotion().addGroup();
            if (engine.getCurrentLocalizedNotion().hasBaseValue() || engine.getCurrentLocalizedNotion().hasValueMap())
                engine.setFocus(new FocusVariable());
        }
        "GROUPS" ( "WHEN" e=boolean_expression )? LBRACE ( notion | query )+ RBRACE
        {
            if (e!=null)
    			if (!engine.getCurrentLocalizedNotion().setGroupCondition(e))
	    			throwException("couldn't set the group condition for "+engine.getCurrentLocalizedNotion());
        }
		;
        
notion_end
        {
            ID id = null;
        }
        : "END" ( o:"OPTIONAL" )? ( r:"ROOT" | t:"TOP" | c:"COMPLETE" | ( g:"GENERAL" )? a:"ANNOTATION" )? "NOTION" id=scoped_ID
        {
            int has_type = 0;
            if (r!=null)
                has_type = Notion.ROOT;
            if (t!=null)
                has_type = Notion.TOP;
            if (c!=null)
                has_type = Notion.COMPLETE;
            if (a!=null)
                has_type = Notion.ANNOTATION;
            boolean is_opt = (o!=null);
            boolean is_gen = (g!=null);
            if (id.scope==null)
                id.scope=engine.getCurrentScope();
            if ((engine.getCurrentLocalizedNotion().getID().scope!=id.scope) || !engine.getCurrentLocalizedNotion().getID().name.equals(id.name))
                throwException("Expecting to end notion["+id+"], but in fact we're closing "+engine.getCurrentLocalizedNotion()+". The curly braces are probably not aligned.");
            if ((r!=null) || (t!=null) || (c!=null) || (o!=null) || (a!=null) || (g!=null)) // if any property should hold, check it
                if (!engine.getCurrentLocalizedNotion().checkProperties(has_type,is_opt,is_gen))
                    showWarning("The END NOTION statement wrongly references "+engine.getCurrentLocalizedNotion());
        }
        ;

/**
 * Queries
 */

query	
        {
            Query q = null;
            boolean nq = false;
        }
        : "FOREACH" "USE" 
        ( 
            q=query_reference
            {
                engine.pushQuery(q);
            }
        |
            "QUERY"
            ( "ON" a:ID )?
            "IS"
            {
                DTLLexer l = (DTLLexer)engine.selector.getCurrentStream();
                String name = "q-l"+l.getLine()+"-c"+l.getColumn();
                q = engine.addQuery(name,(a!=null?a.getText():engine.getCurrentScope().getName()));
                if (q==null)
                    throwException("couldn't create new query");
                engine.pushQuery(q);
                nq = true;
            }
            query_body 
        )
        {
            if (engine.getCurrentLocalizedNotion()!=null)
                engine.getCurrentLocalizedNotion().pushQuery(new Foreach(q));
            else
                engine.getCurrentScope().pushQuery(new Foreach(q));
        }
		LBRACE ( {nq}? context_declaration )* ( notion | query )+ RBRACE
        {
            if (engine.getCurrentLocalizedNotion()!=null)
                engine.getCurrentLocalizedNotion().popQuery();
            else
                engine.getCurrentScope().popQuery();
            engine.popQuery();
        }
		;

/**
 * Semantic annotations
 */
annotate
		returns [ Annotation ann ]
		{
						ann	= new Annotation();
			Annotation	a	= null;
		}
		:
		( a=annotation
			{
				if (!ann.merge(a))
					throwException("couldn't create annotation");
			}
		)+
		;

annotation
		returns [ Annotation ann ]
		{
			ann = new Annotation();
			java.util.Map m	= null;
			Expression l	= null;
			Expression d	= null;
			Note n          = null;
			URL dc          = null;
		}
		:
		(
			m=notions
			{
				if (m != null) {
					if (ann.notions == null)
						ann.notions = m;
					else for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
						String notion = (String)(iter.next());
						if (ann.notions.containsKey(notion)) {
							Link link = (Link)ann.concepts.get(notion);
							if (!link.merge((Link)m.get(notion)))
								throwException("notion["+notion+"] has already been linked, and the two links couldn't be merged");
						} else
							ann.notions.put(notion,m.get(notion));
					}
				}
			}
		)
		|
		(
			m=concepts
			{
				if (m != null) {
					if (ann.concepts == null)
						ann.concepts = m;
					else for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
						String concept = (String)(iter.next());
						if (ann.concepts.containsKey(concept)) {
							Link link = (Link)ann.concepts.get(concept);
							if (!link.merge((Link)m.get(concept)))
								throwException("concept["+concept+"] has already been linked, and the two links couldn't be merged");
						} else
							ann.concepts.put(concept,m.get(concept));
					}
				}
			}
		)
		|
		(
			dc=datcat
			{
				if (dc!=null) {
					if (ann.datcat==null)
						ann.datcat = dc;
					else
						throwException("datcat has already been specified");
				}
			}
		)
		|
		(
			l=label
			{
				if (l!=null) {
					if (ann.label==null)
						ann.label = l;
					else
						throwException("label has already been specified");
				}
			}
		)
		|
		(
			d=descr
			{
				if (d!=null) {
					if (ann.description==null)
						ann.description = d;
					else
						throwException("description has already been specified");
				}
			}
		)
		|
		(
			n=note
			{
				if (n!=null)
					ann.add(n);
			}
		)
		;

notions
        returns [ java.util.Map links ]
		{
					links		= null;
			Set		set			= null;
		}
		: "LINK" "TO" set=notion_references
		{
			if (set!=null) {
				links = new HashMap();
				for (Iterator iter = set.iterator();iter.hasNext();) {
					Notion n = (Notion)iter.next();
					links.put(""+n.getID(),new NotionLink(n));
				}
			}
		}
		;

concepts
		returns [ java.util.Map links ]
		{
					links		= null;
			String	type		= null;
			String	resemblance	= null;
			Set		set			= null;
		}
		: "LINK" ( t:ID )?
		{
			if (t!=null) {
				type = t.getText();
				if (!engine.isLinkingType(type))
					throwException("unknown linking type:"+type);
			}
		}
		(
			"TO" ( e:"equivalent" | l:"looser" | s:"stricter" | o:"overlapping" )?
			{
				if (e!=null)
					resemblance = "equivalent";
				else if (l!=null)
					resemblance = "looser";
				else if (s!=null)
					resemblance = "stricter";
				else if (o!=null)
					resemblance = "overlapping";
			}
		|
			"AS"
			{
				resemblance = "equivalent";
			}
		) set=concept_references
		{
			if (set!=null) {
				links = new HashMap();
				for (Iterator iter = set.iterator();iter.hasNext();) {
					String concept = (String)iter.next();
					links.put(concept,new ConceptLink(type,resemblance,concept));
				}
			}
		}
		;

datcat
        returns [ URL dc ]
		{
			dc = null;
		}
		: "LINK" "TO" dc=datcat_reference
		;
		
label	returns [ Expression e ]
		{
			e = null;
		}
		: "LABEL" ( "IS" )? e=value_expression;

descr	returns [ Expression e ]
		{
			e = null;
		}
		: "DESCRIPTION" ( "IS" )? e=value_expression;

note	returns [ Note n ]
		{
			Description d = null;
			n = null;
		}
		: "NOTE" ( "AUTHOR" "IS" a:STRING )? ( "TYPE" "IS" t:STRING )? d=description
		{
			n = new Note(d);
			if (t!=null)
				n.setType(t.getText());
			if (a!=null)
				n.setAuthor(a.getText());
		}
		;

/**
 * Expressions
 */
boolean_expression
        returns [ Expression e ]
        : e=logical_expression
        ;
        
plogical_expression
        returns [ Expression e ]
        : LPAREN e=logical_expression RPAREN
        ;

logical_expression
        returns [ Expression e ]
        {
            e = null;
            Expression t = null;
        }
        : ( e=plogical_expression | e=compare_expression | e=function_expression )
        (
            ( and:"AND" | or:"OR" ) ( t=compare_expression | t=plogical_expression | t=function_expression )
            {
                if (and!=null)
                    e = new And(e,t);
                else
                    e = new Or(e,t);
            }
        )*
        ;
        
function_expression        
        returns [ Expression e ]
        {
            e = null;
            Expression m = null;
        }
        : "NOT" LPAREN e=boolean_expression RPAREN
        {
            e = new Not(e);
        }
        | "FIND" LPAREN m=value_expression ( COMMA e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e=m;
                    m = engine.getFocus();
                } else
                    throwException("FIND:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			e = new Find(m,e);
		}
        | "MATCH" LPAREN m=value_expression ( COMMA e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e=m;
                    m = engine.getFocus();
                } else
                    throwException("MATCH:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			e = new Match(m,e);
		}
        ;
        
compare_expression
        returns [ Expression e ]
        {
            e = null;
            Value l = null;
            Value r = null;
        }
        : l=value_expression
        (
            (
                  EQ ( r=value_expression | "ANY" )
                {
                    if (r!=null)
                       e = new Equals(l,r);
                    else {
                       r = new Null();
                       e = new Not(new Equals(l,r));;
                    }
                }
                | NEQ r=value_expression
                {
                    e = new Not(new Equals(l,r));
                }
            )?
            {
                if (r==null) {
                    if (engine.hasFocus())
                        e = new Equals(engine.getFocus(),l);
                    else
                        throwException("(NOT) EQUALS:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
                }
            }
        )
        | "ANY"
        {
            if (engine.hasFocus())
                e = new Not(new Equals(engine.getFocus(),new Null()));
            else
                throwException("(NOT) EQUALS:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
        }
        ;

value_expression
		returns [ Value v ]
        {
            v = null;
        }
		: v=literal
        | v=param_reference
        | v=field_reference
        | v=variable_reference
        | v=notion_reference
        | v=value_reference
        | v=marker_reference
        | v=regexp
        | v=regsub
        | v=lookup
        | v=upper
        | v=lower
        | v=trim
        | v=unique
        | v=concat
        | v=convert
        | v=memo
        | v=memo_lookup
        | v=memo_tolerant_lookup
        | v=tidy
        | v=get_label
        | v=get_label_or_value
        | v=get_description
        | v=or_default
        | v=once
		;

trim	returns [ Value v ]
		{
			v = null;
            Value e = null;
		}
		: "TRIM" LPAREN ( e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e = engine.getFocus();
                } else
                    throwException("TRIM:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Trim(e);
		}
		;
        
upper	returns [ Value v ]
		{
			v = null;
            Value e = null;
		}
		: "UPPER" LPAREN ( e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e = engine.getFocus();
                } else
                    throwException("UPPER:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Upper(e);
		}
		;
        
lower	returns [ Value v ]
		{
			v = null;
            Value e = null;
		}
		: "LOWER" LPAREN ( e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e = engine.getFocus();
                } else
                    throwException("LOWER:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Lower(e);
		}
		;
        
regsub	returns [ Value v ]
		{
			v = null;
			Value m = null;
            Value r = null;
            Value e = null;
		}
		: "REGSUB" LPAREN m=value_expression COMMA r=value_expression ( COMMA e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e=r;
                    r=m;
                    m = engine.getFocus();
                } else
                    throwException("REGSUB:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Regsub(m,r,e);
		}
		;
        
regexp	returns [ Value v ]
		{
			v = null;
			Value m = null;
            Value e = null;
		}
		: "REGEXP" LPAREN m=value_expression ( COMMA e=value_expression )? RPAREN
		{
            if (e==null) {
                if (engine.hasFocus()) {
                    e=m;
                    m = engine.getFocus();
                } else
                    throwException("REGEXP:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Regexp(m,e);
		}
		;

lookup
		returns [ Value v ]
		{
            v = null;
			Map  m = null;
		}
		: "LOOKUP" LPAREN m=map_reference RPAREN
		{
			v = new Lookup(m);
		}
		;
        
unique
        returns [ Value v ]
        {
            v = null;
		}
		: "UNIQUE" LPAREN RPAREN
		{
			v = new Unique();
		}
		;
        
concat
        returns [ Value v ]
        {
            v = null;
            List p = new Vector();
        }
        : "CONCAT" LPAREN v=value_expression
        {
            p.add(v);
        }
        (
            COMMA v=value_expression
            {
                p.add(v);
            }
        )*
        RPAREN
        {
            v = new Concat(p);
        }
        ;

convert returns [ Value v ]
		{
			v = null;
			Value map = null;
            Value val = null;
            Value drp = null;
		}
		: "CONVERT" LPAREN map=value_expression COMMA drp=value_expression ( COMMA val=value_expression )? RPAREN
		{
            if (val==null) {
                if (engine.hasFocus())
                    val = engine.getFocus();
                else
                    throwException("CONVERT:there is no field, param, variable in the focus, so you'll have to specify one explicitly");
            }
			v = new Convert(map,drp,val);
		}
        ;

memo    returns [ Value v ]
		{
			        v = null;
            Literal n = null;
            List    p = new Vector();
		}
        : "MEMO" LPAREN n=literal COMMA v=value_expression
        {
            p.add(v);
        }
        (
            COMMA v=value_expression
            {
                p.add(v);
            }
        )*
        RPAREN
        {
            v = new Memo(n.getValue().toString(),p,false,false);
        }
        ;
        
memo_lookup
        returns [ Value v ]
		{
			        v = null;
            Literal n = null;
            List    p = new Vector();
		}
        : "MEMO-LOOKUP" LPAREN n=literal COMMA v=value_expression
        {
            p.add(v);
        }
        (
            COMMA v=value_expression
            {
                p.add(v);
            }
        )*
        RPAREN
        {
            v = new Memo(n.getValue().toString(),p,true,false);
        }
        ;
        
memo_tolerant_lookup
        returns [ Value v ]
		{
			        v = null;
            Literal n = null;
            List    p = new Vector();
		}
        : "MEMO-TOLERANT-LOOKUP" LPAREN n=literal COMMA v=value_expression
        {
            p.add(v);
        }
        (
            COMMA v=value_expression
            {
                p.add(v);
            }
        )*
        RPAREN
        {
            v = new Memo(n.getValue().toString(),p,true,true);
        }
        ;

tidy    returns [ Value v ]
        {
                   v = null;
            Value in = null;
        }
        : "TIDY" LPAREN ( in=value_expression )? RPAREN
        {
            v = new Tidy(in);
        }
        ;
        
get_label
        returns [ Value v ]
        {
                   v = null;
            Value in = null;
        }
        : "GET-LABEL" LPAREN ( in=value_expression )? RPAREN
        {
            v = new AnnotationExtractor("LABEL",in);
        }
        ;
        
get_label_or_value
        returns [ Value v ]
        {
                   v = null;
            Value in = null;
        }
        : "GET-LABEL-OR-VALUE" LPAREN ( in=value_expression )? RPAREN
        {
            v = new AnnotationExtractor("LABEL OR VALUE",in);
        }
        ;
        
get_description
        returns [ Value v ]
        {
                   v = null;
            Value in = null;
        }
        : "GET-DESCRIPTION" LPAREN ( in=value_expression )? RPAREN
        {
            v = new AnnotationExtractor("DESCRIPTION",in);
        }
        ;
        
or_default
        returns [ Value v ]
        {
                   v = null;
            Value expr = null;
            Value def  = null;
        }
        : "DEFAULT" LPAREN expr=value_expression ( COMMA def=value_expression )? RPAREN
        {
            if (def==null) {
                def = expr;
                expr = null;
            }
            v = new Default(expr,def);
        }
        ;
        
once
        returns [ Value v ]
        {
                    v = null;
            Literal n = null;
            Value   e = null;
        }
        : "ONCE" LPAREN n=literal ( COMMA e=value_expression )? RPAREN
        {
            v = new Once(n.getValue().toString(),e);
        }
        ;
        
/**
 * References
 */
marker_reference
        returns [ Marker v ]
        : "MARK" m:MARKER
		{
			v = new Marker(m.getText());
		}
        ;
 
value_reference
        returns [ Value v ]
        {
            v = null;
            Literal l = null;
            Marker m = null;
        }
        : "VALUE" l=literal ( m=marker_reference )?
        {
            if (m!=null)
                l.setMarker(m);
            v = l;
        }
        ;
        
param_reference
        returns [ Value v ]
        {
            v = null;
        }
        : i:ID
        {
			if (engine.getCurrentMap()!=null) {
                v = engine.getCurrentMap().getParam(i.getText());
                if (v==null)
                    throwException("parameter ["+i.getText()+"] isn't known in "+engine.getCurrentMap());
            } else
                throwException("you can't reference parameter["+i.getText()+"] outside a map");
        }
        ;

variable_reference
		returns [ Variable v ]
		{
            v = null;
			ID id = null;
		}
		: "VARIABLE" id=scoped_ID
		{
			v = engine.getVariable(id);
			if (v==null)
				throwException("variable["+id+"] couldn't be found");
		}
		;

notion_reference
		returns [ NotionVariable v ]
        {
            v = null;
            ID id = null;
        }
		: "NOTION" id=notion_reference_path
		{
			Notion n = engine.findNotion(id);
			if (n==null)
				throwException("notion["+id+"] couldn't be found");
            v = new NotionVariable(n);
		}
		;

notion_reference_path
        returns [ ID id ]
        {
        }
		: id=scoped_ID ( SLASH ( SLASH )? id=scoped_ID )*
		;

field_reference
		returns [ FieldVariable f ]
		{
			f = null;
		}
		: "FIELD" i:ID
		{
			f = new FieldVariable(i.getText());
		}
		;

map_reference
		returns [ Map m ]
		{
                    m = null;
            BaseMap b = null;
			ID     id = null;
            Value   v = null;
            List    p = null;
		}
		: "MAP" id=scoped_ID
        (
            {
                p = new Vector();
            }
            LPAREN
            v=value_expression
            {
                p.add(v);
            }
            (
                COMMA v=value_expression
                {
                    p.add(v);
                }
            )*
            RPAREN
        )?
		{
			b = engine.getMap(id);
			if (b==null)
				throwException("map["+id+"] couldn't be found");
            if (p!=null) {
                m = b.useMap(engine.getCurrentScope(),p);
                if (m==null)
                    throwException(""+b+" was wrongly referenced");
            } else
                m = b; // use the base map directly, or call useMap later
		}
		;
        
concept_references
		returns [ Set s ]
		{
			s = null;
		}
		: s=concept_reference
		| "CONCEPTS" s=concept_set
		;

concept_reference
		returns [ Set s ]
		{
			s = new HashSet();
			String c = null;
		}
		: "CONCEPT" c=concept
		{
			try {
				if (!s.add(c))
					throwException("duplicate concept["+c+"]");
			} catch(Exception e) {
				throwException("error adding concept["+c+"] to the concept set:"+e);
			}
		}
		;

concept_set
		returns [ Set s ]
		{
			s = new HashSet();
			String c = null;
		}
		:
		(
			c=concept
			{
				try {
					if (!s.add(c))
						showWarning("duplicate concept["+c+"]");
				} catch(Exception e) {
					throwException("error adding concept["+c+"] to the concept set:"+e);
				}
			}
		|
			LPAREN c=concept
			{
				try {
					if (!s.add(c))
						showWarning("duplicate concept["+c+"]");
				} catch(Exception e) {
					throwException("error adding concept["+c+"] to the concept set:"+e);
				}
			}
			(
				COMMA c=concept
				{
					try {
						if (!s.add(c))
							showWarning("duplicate concept["+c+"]");
					} catch(Exception e) {
						throwException("error adding concept["+c+"] to the concept set:"+e);
					}
				}
			)* RPAREN
		)
		;

concept	returns [ String c ]
		{
			c = null;
		}
		: ( i:ID DOT )? r:ID
		{
			c = r.getText();
			if (i!=null)
				c = i.getText();
			if (!engine.isConcept(c))
				showWarning("unknown concept["+c+"]");//throwException("unknown concept["+c+"]");
		}
		;
        
datcat_reference
		returns [ URL dc ]
		{
			dc = null;
		}
		: "DATCAT" u:URL
		{
			try {
				dc = new URL(u.getText());
			} catch(MalformedURLException ex) {
				throwException("datcat URL["+u+"] is malformed: "+ex);
			}
		}
		;

notion_references
		returns [ Set s ]
		{
	        	s = null;
	        	NotionVariable n = null;
		}
		: n=notion_reference
		{
			s = new HashSet();
			try {
				if (!s.add(n.getNotion()))
					throwException("duplicate "+n);
			} catch(Exception e) {
				throwException("error adding "+n+" to the notion set:"+e);
			}
		}
		| "NOTIONS" s=notion_set
		;

notion_set
		returns [ Set s ]
		{
			        s = new HashSet();
            Notion  n = null;
			ID     id = null;
		}
		:
		(
			id=notion_reference_path
			{
                n = engine.findNotion(id);
                if (n==null)
				    throwException("notion["+id+"] couldn't be found");
				try {
					if (!s.add(n))
						throwException("duplicate "+n);
				} catch(Exception e) {
					throwException("error adding "+n+" to the notion set:"+e);
				}
			}
		|
			LPAREN id=notion_reference_path
			{
                n = engine.findNotion(id);
                if (n==null)
				    throwException("notion["+id+"] couldn't be found");
				try {
					if (!s.add(n))
						throwException("duplicate "+n);
				} catch(Exception e) {
					throwException("error adding "+n+" to the notion set:"+e);
				}
			}
			(
				COMMA id=notion_reference_path
				{
                    n = engine.findNotion(id);
                    if (n==null)
                        throwException("notion["+id+"] couldn't be found");
                    try {
                        if (!s.add(n))
                            throwException("duplicate "+n);
                    } catch(Exception e) {
                        throwException("error adding "+n+" to the notion set:"+e);
                    }
				}
			)* RPAREN
		)
		;

field_references
		returns [ Set s ]
		{
            s = null;
            FieldVariable f = null;
		}
		: f=field_reference
        {
            s = new HashSet();
			try {
				if (!s.add(f.getName()))
					throwException("duplicate "+f);
			} catch(Exception e) {
				throwException("error adding "+f+" to the field set:"+e);
			}
        }
		| "FIELDS" s=field_set
		;

field_set
		returns [ Set s ]
		{
            s = new HashSet();
		}
		:
		(
			i:ID
			{
				try {
					if (!s.add(i.getText()))
						throwException("duplicate field["+i.getText()+"]");
				} catch(Exception e) {
					throwException("error adding field["+i.getText()+"] to the field set:"+e);
				}
			}
		|
			LPAREN h:ID
			{
				try {
					if (!s.add(h.getText()))
						throwException("duplicate field["+h.getText()+"]");
				} catch(Exception e) {
					throwException("error adding field["+h.getText()+"] to the field set:"+e);
				}
			}
			(
				COMMA t:ID
				{
                    try {
                        if (!s.add(t.getText()))
                            throwException("duplicate field["+t.getText()+"]");
                    } catch(Exception e) {
                        throwException("error adding field["+t.getText()+"] to the field set:"+e);
                    }
				}
			)* RPAREN
		)
		;

query_reference
		returns [ Query q ]
		{
            q = null;
		}
		: "QUERY" i:ID
		{
			q = engine.getQuery(new ID(engine.getCurrentScope(),i.getText()));
			if (q==null)
				throwException("query["+i.getText()+"] couldn't be found");
		}
		;
        
query_references
		returns [ Set s ]
		{
            s = null;
            Query q = null;
		}
		: q=query_reference
        {
            s = new HashSet();
			try {
				if (!s.add(q))
					throwException("duplicate "+q);
			} catch(Exception e) {
				throwException("error adding "+q+" to the query set:"+e);
			}
        }
		| "QUERIES" s=query_set
		;

query_set
		returns [ Set s ]
		{
            s = new HashSet();
		}
		:
		(
			i:ID
			{
				try {
                    Query q = engine.getQuery(new ID(engine.getCurrentScope(),i.getText()));
                    if (q==null)
                        throwException("query["+i.getText()+"] couldn't be found");
					if (!s.add(q))
						throwException("duplicate "+q);
				} catch(Exception e) {
					throwException("error adding query["+i.getText()+"] to the query set:"+e);
				}
			}
		|
			LPAREN h:ID
			{
				try {
                    Query q = engine.getQuery(new ID(engine.getCurrentScope(),h.getText()));
                    if (q==null)
                        throwException("query["+h.getText()+"] couldn't be found");
					if (!s.add(q))
						throwException("duplicate "+q);
				} catch(Exception e) {
					throwException("error adding query["+h.getText()+"] to the query set:"+e);
				}
			}
			(
				COMMA t:ID
				{
                    try {
                      Query q = engine.getQuery(new ID(engine.getCurrentScope(),t.getText()));
                      if (q==null)
                          throwException("query["+t.getText()+"] couldn't be found");
                      if (!s.add(q))
                          throwException("duplicate "+q);
                    } catch(Exception e) {
                        throwException("error adding query["+t.getText()+"] to the query set:"+e);
                    }
				}
			)* RPAREN
		)
		;

/**
 * Messages
 */
error	returns [ Message m ]
		{
			m = null;
            Value v = null;
		}
		: "ERROR" v=value_expression
		{
			m = new Error(v);
		}
		;

warning	returns [ Message m ]
		{
			m = null;
            Value v = null;
		}
		: "WARNING" v=value_expression
		{
			m = new Warning(v);
		}
		;

message returns [ Message m ]
		{
			m = null;
            Value v = null;
		}
		: "MESSAGE" v=value_expression
		{
			m = new Message(v);
		}
		;

/**
 * Idenitifiers
 */
scoped_ID
		returns [ ID id ]
		{
			id = null;
		}
		: si:ID ( COLON! i:ID )?
		{
			Scope s = null;
			String n = null;
			if (i!=null) {
				s = engine.getScope(si.getText());
				if (s==null)
					throwException("scope["+si.getText()+"] doesn't exist");
                if (s.invalid())
                    throwException("refering to "+s+" isn't allowed from "+engine.getCurrentScope());
				n = i.getText();
			} else
				n = si.getText();
			id = new ID(s,n);
		}
		;

/**
 * Literals
 */
datatype returns [ String dt ]
		{
			dt = null;
		}
		: ( "STR" | "str" )
		{
			dt = "str";
		}
		| ( "INT" | "int" )
		{
			dt = "int";
		}
		| ( "FLT" | "flt" )
		{
			dt = "flt";
		}
		| ( "URL" | "url" )
		{
			dt = "url";
		}
		| ( "BIT" | "bit" )
		{
			dt = "bit";
		}
		;

literal	returns [ Literal l ]
		{
			             l = null;
            XMLLiteral xml = null;
		}
		: s:STRING
		{
			l = new Literal(s.getText());
		}
		| i:INTEGER ( DOT f:INTEGER )?
		{
            if (f==null) {
			    try {
				    l = new Literal(new Integer(i.getText()));
                } catch(NumberFormatException e) {
                    throwException(e.toString());
                }
            } else {
			    try {
				    l = new Literal(new Float(i.getText()+"."+f.getText()));
                } catch(NumberFormatException e) {
				    throwException(e.toString());
                }
            }
		}
		| u:URL
		{
			try {
				l = new URLLiteral(new URL(u.getText()));
			} catch (MalformedURLException e) {
				throwException("the URL["+u.getText()+"] is malformed:"+e);
			}
		}
		| ( x:XML
			{
				nu.xom.Document doc = null;
				try {
					doc = engine.toXML(x.getText());
				} catch(Exception e) {
					throwException("invalid XML fragment:"+e);
				}
				
				if (xml==null)
					xml = new XMLLiteral(doc);
                else
                    xml.add(doc);
                
                l = xml;
			}
		  )+
        | ( "TRUE" | "true" )
		{
			l = new Literal(new Boolean(true));
		}
		| ( "FALSE" | "false" )
		{
			l = new Literal(new Boolean(false));
		}
		| "NULL"
		{
			l = new Null();
		}
		;

/* descriptions */

description
		returns [ Description d ]
		{
			d = null;
		}
		: s:STRING
		{
			d = new TextDescription(s.getText());
		}
		| u:URL
		{
			try {
				d = new URLDescription(new URL(u.getText()));
			} catch (MalformedURLException e) {
				throwException("the URL["+u.getText()+"] is malformed:"+e);
			}
		}
		| ( x:XML
			{
				nu.xom.Document doc = null;
				try {
					doc = engine.toXML(x.getText());
				} catch(Exception e) {
					throwException("invalid XML fragment:"+e);
				}
				
				if (d==null)
					d = new XMLDescription(doc);
				else
					((XMLDescription)d).add(doc);
			}
		  )+
		;
