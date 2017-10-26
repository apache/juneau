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
package org.apache.juneau.serializer;

import static org.apache.juneau.serializer.Serializer.*;

import org.apache.juneau.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link Serializer} class.
 */
public class SerializerContext extends BeanContext {

	/**
	 * Default context with all default values.
	 */
	static final SerializerContext DEFAULT = new SerializerContext(PropertyStore.create());


	final int maxDepth, initialDepth, maxIndent;
	final boolean
		detectRecursions,
		ignoreRecursions,
		useWhitespace,
		addBeanTypeProperties,
		trimNulls,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		abridged;
	final char quoteChar;
	final UriContext uriContext;
	final UriResolution uriResolution;
	final UriRelativity uriRelativity;
	final Class<? extends SerializerListener> listener;

	/**
	 * Constructor.
	 *
	 * @param ps The property store that created this context.
	 */
	public SerializerContext(PropertyStore ps) {
		super(ps);
		maxDepth = ps.getProperty(SERIALIZER_maxDepth, int.class, 100);
		initialDepth = ps.getProperty(SERIALIZER_initialDepth, int.class, 0);
		detectRecursions = ps.getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = ps.getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useWhitespace = ps.getProperty(SERIALIZER_useWhitespace, boolean.class, false);
		maxIndent = ps.getProperty(SERIALIZER_maxIndent, int.class, 100);
		addBeanTypeProperties = ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true);
		trimNulls = ps.getProperty(SERIALIZER_trimNullProperties, boolean.class, true);
		trimEmptyCollections = ps.getProperty(SERIALIZER_trimEmptyCollections, boolean.class, false);
		trimEmptyMaps = ps.getProperty(SERIALIZER_trimEmptyMaps, boolean.class, false);
		trimStrings = ps.getProperty(SERIALIZER_trimStrings, boolean.class, false);
		sortCollections = ps.getProperty(SERIALIZER_sortCollections, boolean.class, false);
		sortMaps = ps.getProperty(SERIALIZER_sortMaps, boolean.class, false);
		abridged = ps.getProperty(SERIALIZER_abridged, boolean.class, false);
		quoteChar = ps.getProperty(SERIALIZER_quoteChar, String.class, "\"").charAt(0);
		uriContext = ps.getProperty(SERIALIZER_uriContext, UriContext.class, UriContext.DEFAULT);
		uriResolution = ps.getProperty(SERIALIZER_uriResolution, UriResolution.class, UriResolution.NONE);
		uriRelativity = ps.getProperty(SERIALIZER_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);
		listener = ps.getProperty(SERIALIZER_listener, Class.class, null);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("SerializerContext", new ObjectMap()
				.append("maxDepth", maxDepth)
				.append("initialDepth", initialDepth)
				.append("detectRecursions", detectRecursions)
				.append("ignoreRecursions", ignoreRecursions)
				.append("useWhitespace", useWhitespace)
				.append("maxIndent", maxIndent)
				.append("addBeanTypeProperties", addBeanTypeProperties)
				.append("trimNulls", trimNulls)
				.append("trimEmptyCollections", trimEmptyCollections)
				.append("trimEmptyMaps", trimEmptyMaps)
				.append("trimStrings", trimStrings)
				.append("sortCollections", sortCollections)
				.append("sortMaps", sortMaps)
				.append("parserKnowsRootTypes", abridged)
				.append("quoteChar", quoteChar)
				.append("uriContext", uriContext)
				.append("uriResolution", uriResolution)
				.append("uriRelativity", uriRelativity)
				.append("listener", listener)
			);
	}
}
