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
import org.apache.juneau.internal.*;

/**
 * Parent class for all serializer contexts.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class SerializerContext extends Context {

	/**
	 * Max serialization depth ({@link Integer}, default=<code>100</code>).
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 * This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 */
	public static final String SERIALIZER_maxDepth = "Serializer.maxDepth";

	/**
	 * Initial depth ({@link Integer}, default=<code>0</code>).
	 * <p>
	 * The initial indentation level at the root.
	 * Useful when constructing document fragments that need to be indented at a certain level.
	 */
	public static final String SERIALIZER_initialDepth = "Serializer.initialDepth";

	/**
	 * Automatically detect POJO recursions ({@link Boolean}, default=<jk>false</jk>).
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
	 * Note:  Checking for recursion can cause a small performance penalty.
	 */
	public static final String SERIALIZER_detectRecursions = "Serializer.detectRecursions";

	/**
	 * Ignore recursion errors ({@link Boolean}, default=<jk>false</jk>).
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
	 * Debug mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>Enables {@link #SERIALIZER_detectRecursions}.
	 * </ul>
	 */
	public static final String SERIALIZER_debug = "Serializer.debug";

	/**
	 * Use indentation in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, newlines and indentation is added to the output to improve readability.
	 */
	public static final String SERIALIZER_useIndentation = "Serializer.useIndentation";

	/**
	 * Add class attributes to output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, then <js>"_class"</js> attributes will be added to beans if their type cannot be inferred through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from the value type.
	 */
	public static final String SERIALIZER_addClassAttrs = "Serializer.addClassAttrs";

	/**
	 * Quote character ({@link Character}, default=<js>'"'</js>).
	 * <p>
	 * This is the character used for quoting attributes and values.
	 */
	public static final String SERIALIZER_quoteChar = "Serializer.quoteChar";

	/**
	 * Trim null bean property values from output ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 * <p>
	 *	Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 */
	public static final String SERIALIZER_trimNullProperties = "Serializer.trimNullProperties";

	/**
	 * Trim empty lists and arrays from output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>Map entries with empty list values will be lost.
	 * 	<li>Bean properties with empty list values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyLists = "Serializer.trimEmptyLists";

	/**
	 * Trim empty maps from output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 *	<ul class='spaced-list'>
	 * 	<li>Bean properties with empty map values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyMaps = "Serializer.trimEmptyMaps";

	/**
	 * Trim strings in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	public static final String SERIALIZER_trimStrings = "Serializer.trimStrings";

	/**
	 * URI base for relative URIs ({@link String}, default=<js>""</js>).
	 * <p>
	 * Prepended to relative URIs during serialization (along with the {@link #SERIALIZER_absolutePathUriBase} if specified.
	 * (i.e. URIs not containing a schema and not starting with <js>'/'</js>).
	 * (e.g. <js>"foo/bar"</js>)
	 *
	 * <dl>
	 * 	<dt>Examples:</dt>
	 * 	<dd>
	 * 		<table class='styled'>
	 * 			<tr><th>SERIALIZER_relativeUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>mywebapp</code></td>
	 * 				<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>/mywebapp</code></td>
	 * 				<td><code>/mywebapp</code></td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>http://mywebapp</code></td>
	 * 				<td><code>http://mywebapp</code></td>
	 * 			</tr>
	 * 		</table>
	 * 	</dd>
	 * </dl>
	 */
	public static final String SERIALIZER_relativeUriBase = "Serializer.relativeUriBase";

	/**
	 * Sort arrays and collections alphabetically before serializing ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortCollections = "Serializer.sortCollections";

	/**
	 * Sort maps alphabetically before serializing ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortMaps = "Serializer.sortMaps";

	/**
	 * URI base for relative URIs with absolute paths ({@link String}, default=<js>""</js>).
	 * <p>
	 * Prepended to relative absolute-path URIs during serialization.
	 * (i.e. URIs starting with <js>'/'</js>).
	 * (e.g. <js>"/foo/bar"</js>)
	 *
	 * <dl>
	 * 	<dt>Examples:</dt>
	 * 	<dd>
	 * 		<table class='styled'>
	 * 			<tr><th>SERIALIZER_absolutePathUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>mywebapp</code></td>
	 * 				<td><code>mywebapp</code></td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>/mywebapp</code></td>
	 * 				<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td><code>http://foo:9080/bar/baz</code></td>
	 * 				<td><code>http://mywebapp</code></td>
	 * 				<td><code>http://mywebapp</code></td>
	 * 			</tr>
	 * 		</table>
	 * 	</dd>
	 * </dl>
	 */
	public static final String SERIALIZER_absolutePathUriBase = "Serializer.absolutePathUriBase";


	final int maxDepth, initialDepth;
	final boolean
		debug,
		detectRecursions,
		ignoreRecursions,
		useIndentation,
		addClassAttrs,
		trimNulls,
		trimEmptyLists,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps;
	final char quoteChar;
	final String relativeUriBase, absolutePathUriBase;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public SerializerContext(ContextFactory cf) {
		super(cf);
		maxDepth = cf.getProperty(SERIALIZER_maxDepth, int.class, 100);
		initialDepth = cf.getProperty(SERIALIZER_initialDepth, int.class, 0);
		debug = cf.getProperty(SERIALIZER_debug, boolean.class, false);
		detectRecursions = cf.getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = cf.getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useIndentation = cf.getProperty(SERIALIZER_useIndentation, boolean.class, false);
		addClassAttrs = cf.getProperty(SERIALIZER_addClassAttrs, boolean.class, false);
		trimNulls = cf.getProperty(SERIALIZER_trimNullProperties, boolean.class, true);
		trimEmptyLists = cf.getProperty(SERIALIZER_trimEmptyLists, boolean.class, false);
		trimEmptyMaps = cf.getProperty(SERIALIZER_trimEmptyMaps, boolean.class, false);
		trimStrings = cf.getProperty(SERIALIZER_trimStrings, boolean.class, false);
		sortCollections = cf.getProperty(SERIALIZER_sortCollections, boolean.class, false);
		sortMaps = cf.getProperty(SERIALIZER_sortMaps, boolean.class, false);
		quoteChar = cf.getProperty(SERIALIZER_quoteChar, String.class, "\"").charAt(0);
		relativeUriBase = resolveRelativeUriBase(cf.getProperty(SERIALIZER_relativeUriBase, String.class, ""));
		absolutePathUriBase = resolveAbsolutePathUriBase(cf.getProperty(SERIALIZER_absolutePathUriBase, String.class, ""));
	}

	private String resolveRelativeUriBase(String s) {
		if (StringUtils.isEmpty(s))
			return null;
		if (s.equals("/"))
			return s;
		else if (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		return s;
	}

	private String resolveAbsolutePathUriBase(String s) {
		if (StringUtils.isEmpty(s))
			return null;
		if (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		return s;
	}
}
