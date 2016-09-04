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
 * 	<li>{@link JsonParser#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class JsonParserContext extends ParserContext {

	/**
	 * Set strict mode ({@link Boolean}, default=<jk>false</jk>).
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
