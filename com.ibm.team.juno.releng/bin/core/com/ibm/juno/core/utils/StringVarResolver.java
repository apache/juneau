/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.StringUtils.*;
import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

/**
 * Utility class for resolving  variables of the form <code>$X{key}</code> in strings.
 * <p>
 * This class implements the following two methods for resolving variables in strings:
 * <ul>
 * 	<li>{@link #resolve(String)} - Returns a new string with variables replaced.
 * 	<li>{@link #writeTo(String,Writer)} - Resolves variables in the string and sends the result to the writer.
 * </ul>
 * <p>
 * Variables are of the form <code>$X{key}</code>, where <code>X</code> can consist of zero or more ASCII characters.<br>
 * The variable key can contain anything, even nested variables that get recursively resolved.
 * <p>
 * Variable types are defined through the {@link #addVar(String, StringVar)} method.
 * <p>
 * The {@link StringVar} interface defines a simple method for replacing a variable key with a value.
 * <p>
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Create a variable resolver that resolves system properties (e.g. "$S{java.home}")</jc>
 * 	StringVarResolver r = <jk>new</jk> StringVarResolver()
 * 		.addVar(<js>"S"</js>, <jk>new</jk> StringVar() {
 * 			<ja>@Override</ja>
 * 			<jk>public</jk> String resolve(String varVal) {
 * 				<jk>return</jk> System.<jsm>getProperty</jsm>(varVal);
 * 			}
 * 		});
 *
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 * <p>
 * Subclasses of {@link StringVar} are provided for special purposes:
 * <ul>
 * 	<li>{@link StringVarMultipart} - Interface for the resolution of vars that consist of a comma-delimited list (e.g. <js>"$X{foo, bar, baz}"</js>)
 * 	<li>{@link StringVarWithDefault} - Interface for the resolution of vars with a default value if the <code>resolve(String)</code> method returns <jk>null</jk> (e.g. <js>"$S{myProperty,not found}"</js>).
 * </ul>
 *	<p>
 *	The {@link #DEFAULT} instance is a reusable variable resolver that includes support for system properties and environment variables.
 * <p>
 * <code>StringVarResolvers</code> can be extended by using the {@link #StringVarResolver(StringVarResolver)} constructor.
 * <p>
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Create a var resolver that extends the default resolver and appends our own "$URLEncode{...}" variable</jc>
 * 	StringVarResolver r = <jk>new</jk> StringVarResolver(StringVarResolver.<jsf>DEFAULT</jsf>)
 * 		.addVar(<js>"URLEncode"</js>, <jk>new</jk> StringVar() {
 * 			<ja>@Override</ja>
 * 			<jk>public</jk> String resolve(String varVal) {
 * 				<jk>return</jk> URLEncoder.<jsm>encode</jsm>(varVal, <js>"UTF-8"</js>);
 * 			}
 * 		});
 *
 * 	<jc>// Retrieve a system property and URL-encode it if necessary.</jc>
 * 	String myProperty = r.resolve(<js>"$URLEncode{$S{my.property}}"</js>);
 * <p>
 * Variables can be nested arbitrarily deep.
 * <p>
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	<jc>// Look up a property in the following order:
 * 	// 1) MYPROPERTY environment variable.
 * 	// 2) 'my.property' system property if environment variable not found.
 * 	// 3) 'not found' string if system property not found.</jc>
 * 	String myproperty = StringVarResolver.<jsf>DEFAULT</jsf>.resolve(<js>"$E{MYPROPERTY,$S{my.property,not found}}"</js>);
 * </p>
 * <p>
 * Resolved variables can also contain variables.
 * <p class='bcode'>
 * 	<jc>// If MYPROPERTY is "$S{my.property}", and the system property "my.property" is "foo",
 * 	// then the following resolves to "foo".</jc>
 * 	String myproperty = StringVarResolver.<jsf>DEFAULT</jsf>.resolve(<js>"$E{MYPROPERTY}"</js>);
 * </p>
 * <p>
 * <h6 class='topic'>Other notes:</h6>
 * <ul class='spaced-list'>
 * 	<li>The escape character <js>'\'</js> can be used when necessary to escape the following characters: <code>$ , { }</code>
 * 	<li><b>WARNING:</b>  It is possible to cause {@link StackOverflowError StackOverflowErrors} if your nested variables result in
 * 		a recursive loop (e.g. the environment variable <code>'MYPROPERTY'</code> has the value <code>'$E{MYPROPERTY}'</code>).
 * 		So don't do that!
 * 	<li>As a general rule, this class tries to be as efficient as possible by not creating new strings when not needed.<br>
 * 		For example, calling the resolve method on a string that doesn't contain variables (e.g. <code>resolver.resolve(<js>"foobar"</js>)</code>)
 * 		will simply be a no-op and return the same string.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class StringVarResolver {

	/**
	 * Default string variable resolver with support for system properties and environment variables:
	 * <p>
	 * <ul>
	 * 	<li><code>$S{key}</code>,<code>$S{key,default}</code> - System properties.
	 * 	<li><code>$E{key}</code>,<code>$E{key,default}</code> - Environment variables.
	 * </ul>
	 */
	public static final StringVarResolver DEFAULT = new StringVarResolver()
		// System properties.
		.addVar("S", new StringVarWithDefault() {
			@Override /* StringVar */
			public String resolve(String varVal) {
				return System.getProperty(varVal);
			}
		})
		// Environment variables.
		.addVar("E", new StringVarWithDefault() {
			@Override /* StringVar */
			public String resolve(String varVal) {
				return System.getenv(varVal);
			}
		})
	;

	// Map of Vars added through addVar() method.
	private Map<String,StringVar> varMap = new TreeMap<String,StringVar>();

	private StringVarResolver parent;

	/**
	 * Construct an empty var resolver.
	 */
	public StringVarResolver() {}

	/**
	 * Construct an empty var resolver with the specified parent.
	 *
	 * @param parent The parent string variable resolver.  Can be <jk>null</jk>.
	 */
	public StringVarResolver(StringVarResolver parent) {
		this.parent = parent;
	}

	/**
	 * Register a new variable with this resolver.
	 *
	 * @param varName The variable name (e.g. <js>"X"</js> resolves <js>"$X{...}"</js> variables).
	 * @param v The variable resolver.
	 * @return This object (for method chaining).
	 */
	public StringVarResolver addVar(String varName, StringVar v) {
		assertFieldNotNull(v, "v");

		// Need to make sure only ASCII characters are used.
		for (int i = 0; i < varName.length(); i++) {
			char c = varName.charAt(i);
			if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a'))
				illegalArg("Invalid var name.  Must consist of only uppercase and lowercase ASCII letters.");
		}
		varMap.put(varName, v);
		return this;
	}

	/**
	 * Resolve all variables in the specified string.
	 *
	 * @param s The string to resolve variables in.
	 * @return The new string with all variables resolved, or the same string if no variables were found.
	 * 	Null input results in a blank string.
	 */
	public String resolve(String s) {

		if (s == null)
			return "";
		if (s.indexOf('$') == -1 && s.indexOf('\\') == -1)
			return s;

		// Special case where value consists of a single variable with no embedded variables (e.g. "$X{...}").
		// This is a common case, so we want an optimized solution that doesn't involve string builders.
		if (isSimpleVar(s)) {
			String var = s.substring(1, s.indexOf('{'));
			String val = s.substring(s.indexOf('{')+1, s.length()-1);
			StringVar v = getVar(var);
			if (v != null) {
				s = v.doResolve(val);
				return resolve(s);
			}
			return s;
		}

		try {
			return writeTo(s, new StringWriter()).toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens.
		}
	}

	/**
	 * Checks to see if string is of the simple form "$X{...}" with no embedded variables.
	 * This is a common case, and we can avoid using StringWriters.
	 */
	private boolean isSimpleVar(String s) {
		int S1 = 1;	   // Not in variable, looking for $
		int S2 = 2;    // Found $, Looking for {
		int S3 = 3;    // Found {, Looking for }
		int S4 = 4;    // Found }

		int length = s.length();
		int state = S1;
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (state == S1) {
				if (c == '$') {
					state = S2;
				} else {
					return false;
				}
			} else if (state == S2) {
				if (c == '{') {
					state = S3;
				} else if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a')) {   // False trigger "$X "
					return false;
				}
			} else if (state == S3) {
				if (c == '}')
					state = S4;
				else if (c == '{' || c == '$')
					return false;
			} else if (state == S4) {
				return false;
			}
		}
		return state == S4;
	}

	/**
	 * Resolves variables in the specified string and sends the output to the specified writer.
	 * More efficient than first parsing to a string and then serializing to the writer since this
	 * method doesn't need to construct a large string.
	 *
	 * @param s The string to resolve variables in.
	 * @param out The writer to write to.
	 * @return The same writer.
	 * @throws IOException
	 */
	public Writer writeTo(String s, Writer out) throws IOException {

		int S1 = 1;	   // Not in variable, looking for $
		int S2 = 2;    // Found $, Looking for {
		int S3 = 3;    // Found {, Looking for }

		int state = S1;
		boolean isInEscape = false;
		boolean hasInternalVar = false;
		boolean hasInnerEscapes = false;
		String varType = null;
		String varVal = null;
		int x = 0, x2 = 0;
		int depth = 0;
		int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (state == S1) {
				if (isInEscape) {
					if (c == '\\' || c == '$') {
						out.append(c);
					} else {
						out.append('\\').append(c);
					}
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
				} else if (c == '$') {
					x = i;
					x2 = i;
					state = S2;
				} else {
					out.append(c);
				}
			} else if (state == S2) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					hasInnerEscapes = true;
					isInEscape = true;
				} else if (c == '{') {
					varType = s.substring(x+1, i);
					x = i;
					state = S3;
				} else if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a')) {  // False trigger "$X "
					if (hasInnerEscapes)
						out.append(unEscapeChars(s.substring(x, i+1), new char[]{'\\','{'}));
					else
						out.append(s, x, i+1);
					x = i + 1;
					state = S1;
					hasInnerEscapes = false;
				}
			} else if (state == S3) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
					hasInnerEscapes = true;
				} else if (c == '{') {
					depth++;
					hasInternalVar = true;
				} else if (c == '}') {
					if (depth > 0) {
						depth--;
					} else {
						varVal = s.substring(x+1, i);
						varVal = (hasInternalVar ? resolve(varVal) : varVal);
						StringVar r = getVar(varType);
						if (r == null) {
							if (hasInnerEscapes)
								out.append(unEscapeChars(s.substring(x2, i+1), new char[]{'\\','$','{','}'}));
							else
								out.append(s, x2, i+1);
							x = i+1;
						} else {
							String replacement = r.doResolve(varVal);
							if (replacement == null)
								replacement = "";
							// If the replacement also contains variables, replace them now.
							if (replacement.indexOf('$') != -1)
								replacement = resolve(replacement);
							out.append(replacement);
							x = i+1;
						}
						state = 1;
						hasInnerEscapes = false;
					}
				}
			}
		}
		if (isInEscape)
			out.append("\\");
		else if (state == S2)
			out.append("$").append(unEscapeChars(s.substring(x+1), new char[]{'{', '\\'}));
		else if (state == S3)
			out.append("$").append(varType).append('{').append(unEscapeChars(s.substring(x+1), new char[]{'\\','$','{','}'}));
		return out;
	}

	private StringVar getVar(String varType) {
		StringVar v = varMap.get(varType);
		if (v == null && parent != null)
			v = parent.getVar(varType);
		return v;
	}
}

