// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.parser;

import static org.apache.juneau.parser.ParserContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;

/**
 * Session object that lives for the duration of a single use of {@link Parser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class ParserSession extends BeanSession {

	private final boolean trimStrings, strict;
	private final String inputStreamCharset, fileCharset;

	private final Method javaMethod;
	private final Object outer;
	private final Object input;
	private String inputString;
	private InputStream inputStream;
	private Reader reader, noCloseReader;
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;
	private final ParserListener listener;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for this object.
	 * @param input The input.
	 * <br>For character-based parsers, this can be any of the following types:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>{@link Reader}
	 * 	<li>{@link CharSequence}
	 * 	<li>{@link InputStream} containing UTF-8 encoded text (or whatever the encoding specified by {@link ParserContext#PARSER_inputStreamCharset}).
	 * 	<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or whatever the encoding specified by {@link ParserContext#PARSER_inputStreamCharset}).
	 * 	<li>{@link File} containing system encoded text (or whatever the encoding specified by {@link ParserContext#PARSER_fileCharset}).
	 * </ul>
	 * <br>For byte-based parsers, this can be any of the following types:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>{@link InputStream}
	 * 	<li><code><jk>byte</jk>[]</code>
	 * 	<li>{@link File}
	 * </ul>
	 * @param op The override properties.
	 * These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	public ParserSession(ParserContext ctx, ObjectMap op, Object input, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, locale, timeZone, mediaType);
		Class<?> listenerClass;
		if (op == null || op.isEmpty()) {
			trimStrings = ctx.trimStrings;
			strict = ctx.strict;
			inputStreamCharset = ctx.inputStreamCharset;
			fileCharset = ctx.fileCharset;
			listenerClass = ctx.listener;
		} else {
			trimStrings = op.getBoolean(PARSER_trimStrings, ctx.trimStrings);
			strict = op.getBoolean(PARSER_strict, ctx.strict);
			inputStreamCharset = op.getString(PARSER_inputStreamCharset, ctx.inputStreamCharset);
			fileCharset = op.getString(PARSER_fileCharset, ctx.fileCharset);
			listenerClass = op.get(Class.class, PARSER_listener, ctx.listener);
		}
		this.input = input;
		this.javaMethod = javaMethod;
		this.outer = outer;
		this.listener = newInstance(ParserListener.class, listenerClass);
	}

	/**
	 * Wraps the specified input object inside an input stream.
	 * Subclasses can override this method to implement their own input streams.
	 *
	 * @return The input object wrapped in an input stream, or <jk>null</jk> if the object is null.
	 * @throws ParseException If object could not be converted to an input stream.
	 */
	public InputStream getInputStream() throws ParseException {
		try {
			if (input == null)
				return null;
			if (input instanceof InputStream) {
				if (isDebug()) {
					byte[] b = readBytes((InputStream)input, 1024);
					inputString = toHex(b);
					return new ByteArrayInputStream(b);
				}
				return (InputStream)input;
			}
			if (input instanceof byte[]) {
				if (isDebug())
					inputString = toHex((byte[])input);
				return new ByteArrayInputStream((byte[])input);
			}
			if (input instanceof String) {
				inputString = (String)input;
				return new ByteArrayInputStream(fromHex((String)input));
			}
			if (input instanceof File) {
				if (isDebug()) {
					byte[] b = readBytes((File)input);
					inputString = toHex(b);
					return new ByteArrayInputStream(b);
				}
				inputStream = new FileInputStream((File)input);
				return inputStream;
			}
		} catch (IOException e) {
			throw new ParseException(e);
		}
		throw new ParseException("Cannot convert object of type {0} to an InputStream.", input.getClass().getName());
	}


	/**
	 * Wraps the specified input object inside a reader.
	 * Subclasses can override this method to implement their own readers.
	 *
	 * @return The input object wrapped in a Reader, or <jk>null</jk> if the object is null.
	 * @throws Exception If object could not be converted to a reader.
	 */
	public Reader getReader() throws Exception {
		if (input == null)
			return null;
		if (input instanceof Reader) {
			if (isDebug()) {
				inputString = read((Reader)input);
				return new StringReader(inputString);
			}
			return (Reader)input;
		}
		if (input instanceof CharSequence) {
			inputString = input.toString();
			if (reader == null)
				reader = new ParserReader((CharSequence)input);
			return reader;
		}
		if (input instanceof InputStream || input instanceof byte[]) {
			InputStream is = (input instanceof InputStream ? (InputStream)input : new ByteArrayInputStream((byte[])input));
			if (noCloseReader == null) {
				CharsetDecoder cd = ("default".equalsIgnoreCase(inputStreamCharset) ? Charset.defaultCharset() : Charset.forName(inputStreamCharset)).newDecoder();
				if (strict) {
					cd.onMalformedInput(CodingErrorAction.REPORT);
					cd.onUnmappableCharacter(CodingErrorAction.REPORT);
				} else {
					cd.onMalformedInput(CodingErrorAction.REPLACE);
					cd.onUnmappableCharacter(CodingErrorAction.REPLACE);
				}
				noCloseReader = new InputStreamReader(is, cd);
			}
			if (isDebug()) {
				inputString = read(noCloseReader);
				return new StringReader(inputString);
			}
			return noCloseReader;
		}
		if (input instanceof File) {
			if (reader == null) {
				CharsetDecoder cd = ("default".equalsIgnoreCase(fileCharset) ? Charset.defaultCharset() : Charset.forName(fileCharset)).newDecoder();
				if (strict) {
					cd.onMalformedInput(CodingErrorAction.REPORT);
					cd.onUnmappableCharacter(CodingErrorAction.REPORT);
				} else {
					cd.onMalformedInput(CodingErrorAction.REPLACE);
					cd.onUnmappableCharacter(CodingErrorAction.REPLACE);
				}
				reader = new InputStreamReader(new FileInputStream((File)input), cd);
			}
			if (isDebug()) {
				inputString = read(reader);
				return new StringReader(inputString);
			}
			return reader;
		}
		throw new ParseException("Cannot convert object of type {0} to a Reader.", input.getClass().getName());
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <code>{line:123,column:456,currentProperty:"foobar"}</code>
	 */
	public Map<String,Object> getLastLocation() {
		Map<String,Object> m = new LinkedHashMap<String,Object>();
		if (currentClass != null)
			m.put("currentClass", currentClass.toString(true));
		if (currentProperty != null)
			m.put("currentProperty", currentProperty);
		return m;
	}

	/**
	 * Returns the raw input object passed into this session.
	 *
	 * @return The raw input object passed into this session.
	 */
	protected Object getInput() {
		return input;
	}

	/**
	 * Returns the Java method that invoked this parser.
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 *
	 * @return The Java method that invoked this parser.
	*/
	public final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the outer object used for instantiating top-level non-static member classes.
	 * When using the REST API, this is the servlet object.
	 *
	 * @return The outer object.
	*/
	public final Object getOuter() {
		return outer;
	}

	/**
	 * Sets the current bean property being parsed for proper error messages.
	 * @param currentProperty The current property being parsed.
	 */
	public void setCurrentProperty(BeanPropertyMeta currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being parsed for proper error messages.
	 * @param currentClass The current class being parsed.
	 */
	public void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Returns the {@link ParserContext#PARSER_trimStrings} setting value for this session.
	 *
	 * @return The {@link ParserContext#PARSER_trimStrings} setting value for this session.
	 */
	public final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the {@link ParserContext#PARSER_strict} setting value for this session.
	 *
	 * @return The {@link ParserContext#PARSER_strict} setting value for this session.
	 */
	public final boolean isStrict() {
		return strict;
	}

	/**
	 * Trims the specified object if it's a <code>String</code> and {@link #isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The object to trim.
	 * @return The trimmmed string if it's a string.
	 */
	@SuppressWarnings("unchecked")
	public final <K> K trim(K o) {
		if (trimStrings && o instanceof String)
			return (K)o.toString().trim();
		return o;

	}

	/**
	 * Trims the specified string if {@link ParserSession#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param s The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String trim(String s) {
		if (trimStrings && s != null)
			return s.trim();
		return s;
	}

	/**
	 * Converts the specified <code>ObjectMap</code> into a bean identified by the <js>"_type"</js>
	 * property in the map.
	 *
	 * @param m The map to convert to a bean.
	 * @param pMeta The current bean property being parsed.
	 * @param eType The current expected type being parsed.
	 * @return The converted bean, or the same map if the <js>"_type"</js> entry wasn't found
	 * 	or didn't resolve to a bean.
	 */
	public final Object cast(ObjectMap m, BeanPropertyMeta pMeta, ClassMeta<?> eType) {

		String btpn = getBeanTypePropertyName(eType);

		Object o = m.get(btpn);
		if (o == null)
			return m;
		String typeName = o.toString();

		ClassMeta<?> cm = getClassMeta(typeName, pMeta, eType);

		if (cm != null) {
			BeanMap<?> bm = m.getBeanSession().newBeanMap(cm.getInnerClass());

			// Iterate through all the entries in the map and set the individual field values.
			for (Map.Entry<String,Object> e : m.entrySet()) {
				String k = e.getKey();
				Object v = e.getValue();
				if (! k.equals(btpn)) {
					// Attempt to recursively cast child maps.
					if (v instanceof ObjectMap)
						v = cast((ObjectMap)v, pMeta, eType);
					bm.put(k, v);
				}
			}
			return bm.getBean();
		}

		return m;
	}

	/**
	 * Give the specified dictionary name, resolve it to a class.
	 *
	 * @param typeName The dictionary name to resolve.
	 * @param pMeta The bean property we're currently parsing.
	 * @param eType The expected type we're currently parsing.
	 * @return The resolved class, or <jk>null</jk> if the type name could not be resolved.
	 */
	public final ClassMeta<?> getClassMeta(String typeName, BeanPropertyMeta pMeta, ClassMeta<?> eType) {
		BeanRegistry br = null;

		// Resolve via @BeanProperty(beanDictionary={})
		if (pMeta != null) {
			br = pMeta.getBeanRegistry();
			if (br != null && br.hasName(typeName))
				return br.getClassMeta(typeName);
		}

		// Resolve via @Bean(beanDictionary={}) on the expected type where the
		// expected type is an interface with subclasses.
		if (eType != null) {
			br = eType.getBeanRegistry();
			if (br != null && br.hasName(typeName))
				return br.getClassMeta(typeName);
		}

		// Last resort, resolve using the session registry.
		return getBeanRegistry().getClassMeta(typeName);
	}

	/**
	 * Method that gets called when an unknown bean property name is encountered.
	 *
	 * @param propertyName The unknown bean property name.
	 * @param beanMap The bean that doesn't have the expected property.
	 * @param line The line number where the property was found.  <code>-1</code> if line numbers are not available.
	 * @param col The column number where the property was found.  <code>-1</code> if column numbers are not available.
	 * @throws ParseException Automatically thrown if {@link BeanContext#BEAN_ignoreUnknownBeanProperties} setting
	 * 	on this parser is <jk>false</jk>
	 * @param <T> The class type of the bean map that doesn't have the expected property.
	 */
	public <T> void onUnknownProperty(String propertyName, BeanMap<T> beanMap, int line, int col) throws ParseException {
		if (propertyName.equals(getBeanTypePropertyName(beanMap.getClassMeta())))
			return;
		if (! isIgnoreUnknownBeanProperties())
			throw new ParseException(this, "Unknown property ''{0}'' encountered while trying to parse into class ''{1}''", propertyName, beanMap.getClassMeta());
		if (listener != null)
			listener.onUnknownBeanProperty(this, propertyName, beanMap.getClassMeta().getInnerClass(), beanMap.getBean(), line, col);
	}

	/**
	 * Returns the input to this parser as a plain string.
	 * <p>
	 * This method only returns a value if {@link BeanContext#BEAN_debug} is enabled.
	 *
	 * @return The input as a string, or <jk>null</jk> if debug mode not enabled.
	 */
	public String getInputAsString() {
		return inputString;
	}

	/**
	 * Perform cleanup on this context object if necessary.
	 */
	@Override
	public boolean close() {
		if (super.close()) {
			try {
				if (inputStream != null)
					inputStream.close();
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				throw new BeanRuntimeException(e);
			}
			return true;
		}
		return false;
	}
}
