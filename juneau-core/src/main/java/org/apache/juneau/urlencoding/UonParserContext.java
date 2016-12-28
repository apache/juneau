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
package org.apache.juneau.urlencoding;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Configurable properties on the {@link UonParser} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link UonParser#setProperty(String,Object)}
 * 	<li>{@link UonParser#setProperties(ObjectMap)}
 * 	<li>{@link UonParser#addNotBeanClasses(Class[])}
 * 	<li>{@link UonParser#addBeanFilters(Class[])}
 * 	<li>{@link UonParser#addPojoSwaps(Class[])}
 * 	<li>{@link UonParser#addToDictionary(Class[])}
 * 	<li>{@link UonParser#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the URL-Encoding and UON parsers</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th><th>Session overridable</th></tr>
 * 	<tr>
 * 		<td>{@link #UON_decodeChars}</td>
 * 		<td>Decode <js>"%xx"</js> sequences</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk> for {@link UonParser}<br><jk>true</jk> for {@link UrlEncodingParser}</td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #UON_whitespaceAware}</td>
 * 		<td>Whitespace aware</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../parser/ParserContext.html#ConfigProperties'>ParserContext</a> - Configurable properties common to all parsers.
 * 	</ul>
 * </ul>
 */
public class UonParserContext extends ParserContext {

	/**
	 * <b>Configuration property:</b> Decode <js>"%xx"</js> sequences.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonParser.decodeChars"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk> for {@link UonParser}, <jk>true</jk> for {@link UrlEncodingParser}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specify <jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk>
	 * 	if they've already been decoded before being passed to this parser.
	 */
	public static final String UON_decodeChars = "UonParser.decodeChars";

	/**
	 * <b>Configuration property:</b> Whitespace aware.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonParser.whitespaceAware"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Expect input to contain readable whitespace characters from using the {@link UonSerializerContext#UON_useWhitespace} setting.
	 */
	public static final String UON_whitespaceAware = "UonParser.whitespaceAware";


	final boolean
		decodeChars,
		whitespaceAware;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public UonParserContext(ContextFactory cf) {
		super(cf);
		this.decodeChars = cf.getProperty(UON_decodeChars, boolean.class, false);
		this.whitespaceAware = cf.getProperty(UON_whitespaceAware, boolean.class, false);
	}
}
