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

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * Configurable properties common to all parsers.
 */
public class ParserContext extends BeanContext {

	/**
	 * <b>Configuration property:</b>  Trim parsed strings.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.trimStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 */
	public static final String PARSER_trimStrings = "Parser.trimStrings";

	/**
	 * <b>Configuration property:</b>  Strict mode.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.strict"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 *
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 *
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 */
	public static final String PARSER_strict = "Parser.strict";

	/**
	 * <b>Configuration property:</b>  Input stream charset.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.inputStreamCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UTF-8"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * The character set to use for converting <code>InputStreams</code> and byte arrays to readers.
	 *
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 */
	public static final String PARSER_inputStreamCharset = "Parser.inputStreamCharset";

	/**
	 * <b>Configuration property:</b>  File charset.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.fileCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"default"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * The character set to use for reading <code>Files</code> from the file system.
	 *
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 *
	 * <p>
	 * <js>"default"</js> can be used to indicate the JVM default file system charset.
	 */
	public static final String PARSER_fileCharset = "Parser.fileCharset";

	/**
	 * <b>Configuration property:</b>  Parser listener.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Parser.listener"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? extends ParserListener&gt;</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 */
	public static final String PARSER_listener = "PARSER.listener";

	final boolean trimStrings, strict;
	final String inputStreamCharset, fileCharset;
	final Class<? extends ParserListener> listener;

	/**
	 * Constructor.
	 *
	 * @param ps The property store that created this context.
	 */
	@SuppressWarnings("unchecked")
	public ParserContext(PropertyStore ps) {
		super(ps);
		this.trimStrings = ps.getProperty(PARSER_trimStrings, boolean.class, false);
		this.strict = ps.getProperty(PARSER_strict, boolean.class, false);
		this.inputStreamCharset = ps.getProperty(PARSER_inputStreamCharset, String.class, "UTF-8");
		this.fileCharset = ps.getProperty(PARSER_fileCharset, String.class, "default");
		this.listener = ps.getProperty(PARSER_listener, Class.class, null);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("ParserContext", new ObjectMap()
				.append("trimStrings", trimStrings)
				.append("strict", strict)
				.append("inputStreamCharset", inputStreamCharset)
				.append("fileCharset", fileCharset)
				.append("listener", listener)
			);
	}
}
