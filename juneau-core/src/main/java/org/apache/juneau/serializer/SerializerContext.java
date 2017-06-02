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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Configurable properties common to all serializers.
 */
public class SerializerContext extends BeanContext {

	/**
	 * <b>Configuration property:</b>  Max serialization depth.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.maxDepth"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>100</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 * This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 */
	public static final String SERIALIZER_maxDepth = "Serializer.maxDepth";

	/**
	 * <b>Configuration property:</b>  Initial depth.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.initialDepth"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>0</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The initial indentation level at the root.
	 * Useful when constructing document fragments that need to be indented at a certain level.
	 */
	public static final String SERIALIZER_initialDepth = "Serializer.initialDepth";

	/**
	 * <b>Configuration property:</b>  Automatically detect POJO recursions.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.detectRecursions"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 * <p>
	 * Recursions can occur when serializing models that aren't true trees, but rather contain loops.
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link #SERIALIZER_ignoreRecursions}.
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>SERIALIZER_ignoreRecursions</jsf> is <jk>true</jk>...
	 * <code>{A:{B:{C:null}}}</code><br>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 */
	public static final String SERIALIZER_detectRecursions = "Serializer.detectRecursions";

	/**
	 * <b>Configuration property:</b>  Ignore recursion errors.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.ignoreRecursions"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Used in conjunction with {@link #SERIALIZER_detectRecursions}.
	 * Setting is ignored if <jsf>SERIALIZER_detectRecursions</jsf> is <jk>false</jk>.
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree,
	 * 	we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 */
	public static final String SERIALIZER_ignoreRecursions = "Serializer.ignoreRecursions";

	/**
	 * <b>Configuration property:</b>  Use whitespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.useWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String SERIALIZER_useWhitespace = "Serializer.useWhitespace";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from the value type.
	 */
	public static final String SERIALIZER_addBeanTypeProperties = "Serializer.addBeanTypeProperties";

	/**
	 * <b>Configuration property:</b>  Quote character.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.quoteChar"</js>
	 * 	<li><b>Data type:</b> <code>Character</code>
	 * 	<li><b>Default:</b> <js>'"'</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * This is the character used for quoting attributes and values.
	 */
	public static final String SERIALIZER_quoteChar = "Serializer.quoteChar";

	/**
	 * <b>Configuration property:</b>  Trim null bean property values.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimNullProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 */
	public static final String SERIALIZER_trimNullProperties = "Serializer.trimNullProperties";

	/**
	 * <b>Configuration property:</b>  Trim empty lists and arrays.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyLists"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>Map entries with empty list values will be lost.
	 * 	<li>Bean properties with empty list values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyCollections = "Serializer.trimEmptyLists";

	/**
	 * <b>Configuration property:</b>  Trim empty maps.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyMaps"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>Bean properties with empty map values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyMaps = "Serializer.trimEmptyMaps";

	/**
	 * <b>Configuration property:</b>  Trim strings.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	public static final String SERIALIZER_trimStrings = "Serializer.trimStrings";

	/**
	 * <b>Configuration property:</b>  URI context bean.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriContext"</js>
	 * 	<li><b>Data type:</b> {@link UriContext}
	 * 	<li><b>Default:</b> {@link UriContext#DEFAULT}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 * <p>
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>
	 * </p>
	 */
	public static final String SERIALIZER_uriContext = "Serializer.uriContext";

	/**
	 * <b>Configuration property:</b>  URI resolution.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriResolution"</js>
	 * 	<li><b>Data type:</b> {@link UriResolution}
	 * 	<li><b>Default:</b> {@link UriResolution#ROOT_RELATIVE}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriResolution#ABSOLUTE}
	 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#ROOT_RELATIVE}
	 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#NONE}
	 * 		- Don't do any URL resolution.
	 * </ul>
	 */
	public static final String SERIALIZER_uriResolution = "Serializer.uriResolution";

	/**
	 * <b>Configuration property:</b>  URI relativity.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriRelativity"</js>
	 * 	<li><b>Data type:</b> {@link UriRelativity}
	 * 	<li><b>Default:</b> {@link UriRelativity#RESOURCE}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li>{@link UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 */
	public static final String SERIALIZER_uriRelativity = "Serializer.uriRelativity";

	/**
	 * <b>Configuration property:</b>  Sort arrays and collections alphabetically.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortCollections"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortCollections = "Serializer.sortCollections";

	/**
	 * <b>Configuration property:</b>  Sort maps alphabetically.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortMaps"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortMaps = "Serializer.sortMaps";

	/**
	 * <b>Configuration property:</b>  Abridged output.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.abridged"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled, it is assumed that the parser knows the exact Java POJO type being parsed,
	 * and therefore top-level type information that might normally be included to determine
	 * the data type will not be serialized.
	 * <p>
	 * For example, when serializing a POJO with a {@link Bean#typeName()} value, a <js>"_type"</js>
	 * will be added when this setting is disabled, but not added when it is enabled.
	 */
	public static final String SERIALIZER_abridged = "Serializer.abridged";

	/**
	 * <b>Configuration property:</b>  Serializer listener.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.listener"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? extends SerializerListener&gt;</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 */
	public static final String SERIALIZER_listener = "Serializer.listener";


	final int maxDepth, initialDepth;
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
	@SuppressWarnings("unchecked")
	public SerializerContext(PropertyStore ps) {
		super(ps);
		maxDepth = ps.getProperty(SERIALIZER_maxDepth, int.class, 100);
		initialDepth = ps.getProperty(SERIALIZER_initialDepth, int.class, 0);
		detectRecursions = ps.getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = ps.getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useWhitespace = ps.getProperty(SERIALIZER_useWhitespace, boolean.class, false);
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
		uriResolution = ps.getProperty(SERIALIZER_uriResolution, UriResolution.class, UriResolution.ROOT_RELATIVE);
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
