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
package org.apache.juneau.json;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Configurable properties on the {@link JsonParser} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link JsonParser#setProperty(String,Object)}
 * 	<li>{@link JsonParser#setProperties(ObjectMap)}
 * 	<li>{@link JsonParser#addNotBeanClasses(Class[])}
 * 	<li>{@link JsonParser#addBeanFilters(Class[])}
 * 	<li>{@link JsonParser#addPojoSwaps(Class[])}
 * 	<li>{@link JsonParser#addToDictionary(Class[])}
 * 	<li>{@link JsonParser#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the JSON parser</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #JSON_strictMode}</td>
 * 		<td>Strict mode</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
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
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class JsonParserContext extends ParserContext {

	/**
	 * <b>Configuration property:</b>  Strict mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonParser.strictMode"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When in strict mode, parser throws exceptions on the following invalid JSON syntax:
	 * <ul class='spaced-list'>
	 * 	<li>Unquoted attributes.
	 * 	<li>Missing attribute values.
	 * 	<li>Concatenated strings.
	 * 	<li>Javascript comments.
	 * 	<li>Numbers and booleans when Strings are expected.
	 * </ul>
	 */
	public static final String JSON_strictMode = "JsonParser.strictMode";

	final boolean strictMode;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public JsonParserContext(ContextFactory cf) {
		super(cf);
		this.strictMode = cf.getProperty(JSON_strictMode, boolean.class, false);
	}
}
