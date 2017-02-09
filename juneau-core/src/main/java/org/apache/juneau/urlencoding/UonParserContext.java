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
 * See {@link ContextFactory} for more information about context properties.
 *
 * <h5 class='section'>Inherited configurable properties:</h5>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class="doclink" href="../BeanContext.html#ConfigProperties">BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class="doclink" href="../parser/ParserContext.html#ConfigProperties">ParserContext</a> - Configurable properties common to all parsers.
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

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UonParserContext", new ObjectMap()
				.append("decodeChars", decodeChars)
				.append("whitespaceAware", whitespaceAware)
			);
	}
}
