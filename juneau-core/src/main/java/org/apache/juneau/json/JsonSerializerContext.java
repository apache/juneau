/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.json;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link JsonSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link JsonSerializer#setProperty(String,Object)}
 * 	<li>{@link JsonSerializer#setProperties(ObjectMap)}
 * 	<li>{@link JsonSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link JsonSerializer#addBeanFilters(Class[])}
 * 	<li>{@link JsonSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link JsonSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class JsonSerializerContext extends SerializerContext {

	/**
	 * Simple JSON mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 */
	public static final String JSON_simpleMode = "JsonSerializer.simpleMode";

	/**
	 * Use whitespace in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String JSON_useWhitespace = "JsonSerializer.useWhitespace";

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents
	 * 	confusion when trying to serialize <xt>&lt;\/script&gt;</xt>.
	 */
	public static final String JSON_escapeSolidus = "JsonSerializer.escapeSolidus";


	final boolean
		simpleMode,
		useWhitespace,
		escapeSolidus;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public JsonSerializerContext(ContextFactory cf) {
		super(cf);
		simpleMode = cf.getProperty(JSON_simpleMode, boolean.class, false);
		useWhitespace = cf.getProperty(JSON_useWhitespace, boolean.class, false);
		escapeSolidus = cf.getProperty(JSON_escapeSolidus, boolean.class, false);
	}
}
