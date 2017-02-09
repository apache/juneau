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
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link JsonSerializer} class.
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
 * 		<li class='c'><a class="doclink" href="../serializer/SerializerContext.html#ConfigProperties">SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 */
public final class JsonSerializerContext extends SerializerContext {

	/**
	 * <b>Configuration property:</b>  Simple JSON mode.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.simpleMode"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 */
	public static final String JSON_simpleMode = "JsonSerializer.simpleMode";

	/**
	 * <b>Configuration property:</b>  Use whitespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.useWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String JSON_useWhitespace = "JsonSerializer.useWhitespace";

	/**
	 * <b>Configuration property:</b>  Prefix solidus <js>'/'</js> characters with escapes.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"JsonSerializer.escapeSolidus"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
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

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("JsonSerializerContext", new ObjectMap()
				.append("simpleMode", simpleMode)
				.append("useWhitespace", useWhitespace)
				.append("escapeSolidus", escapeSolidus)
			);
	}
}
