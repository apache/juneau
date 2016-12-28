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
 * Configurable properties common to all serializers.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties common to all serializers</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th><th>Session overridable</th></tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_maxDepth}</td>
 * 		<td>Max serialization depth.</td>
 * 		<td><code>Integer</code></td>
 * 		<td><code>100</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_initialDepth}</td>
 * 		<td>Initial depth.</td>
 * 		<td><code>Integer</code></td>
 * 		<td><code>0</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_detectRecursions}</td>
 * 		<td>Automatically detect POJO recursions.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_ignoreRecursions}</td>
 * 		<td>Ignore recursion errors.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_useIndentation}</td>
 * 		<td>Use indentation.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_addBeanTypeProperties}</td>
 * 		<td>Add <js>"_type"</js> properties when needed.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_quoteChar}</td>
 * 		<td>Quote character.</td>
 * 		<td><code>Character</code></td>
 * 		<td><js>'"'</js></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_trimNullProperties}</td>
 * 		<td>Trim null bean property values.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_trimEmptyCollections}</td>
 * 		<td>Trim empty lists and arrays.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_trimEmptyMaps}</td>
 * 		<td>Trim empty maps.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_trimStrings}</td>
 * 		<td>Trim strings.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_relativeUriBase}</td>
 * 		<td>URI base for relative URIs.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>""</js></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_absolutePathUriBase}</td>
 * 		<td>URI base for relative URIs with absolute paths.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>""</js></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_sortCollections}</td>
 * 		<td>Sort arrays and collections alphabetically.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #SERIALIZER_sortMaps}</td>
 * 		<td>Sort maps alphabetically.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * </table>
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
	 * Note:  Checking for recursion can cause a small performance penalty.
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
	 * <b>Configuration property:</b>  Use indentation.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.useIndentation"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, newlines and indentation is added to the output to improve readability.
	 */
	public static final String SERIALIZER_useIndentation = "Serializer.useIndentation";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
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
	 *	Note that enabling this setting has the following effects on parsing:
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
	 *	<ul class='spaced-list'>
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
	 * <b>Configuration property:</b>  URI base for relative URIs.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.relativeUriBase"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Prepended to relative URIs during serialization (along with the {@link #SERIALIZER_absolutePathUriBase} if specified.
	 * (i.e. URIs not containing a schema and not starting with <js>'/'</js>).
	 * (e.g. <js>"foo/bar"</js>)
	 *
	 * <h6 class='topic'>Example:</h6>
	 * <table class='styled'>
	 *		<tr><th>SERIALIZER_relativeUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 		<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 	</tr>
	 * </table>
	 */
	public static final String SERIALIZER_relativeUriBase = "Serializer.relativeUriBase";

	/**
	 * <b>Configuration property:</b>  URI base for relative URIs with absolute paths.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.absolutePathUriBase"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Prepended to relative absolute-path URIs during serialization.
	 * (i.e. URIs starting with <js>'/'</js>).
	 * (e.g. <js>"/foo/bar"</js>)
	 *
	 * <h6 class='topic'>Examples:</h6>
	 * <table class='styled'>
	 * 	<tr><th>SERIALIZER_absolutePathUriBase</th><th>URI</th><th>Serialized URI</th></tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 		<td><code>mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>/mywebapp</code></td>
	 * 		<td><code>http://foo:9080/bar/baz/mywebapp</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>http://foo:9080/bar/baz</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 		<td><code>http://mywebapp</code></td>
	 * 	</tr>
	 * </table>
	 */
	public static final String SERIALIZER_absolutePathUriBase = "Serializer.absolutePathUriBase";

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


	final int maxDepth, initialDepth;
	final boolean
		detectRecursions,
		ignoreRecursions,
		useIndentation,
		addBeanTypeProperties,
		trimNulls,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps;
	final char quoteChar;
	final String relativeUriBase, absolutePathUriBase;

	/**
	 * Constructor.
	 *
	 * @param cf The factory that created this context.
	 */
	public SerializerContext(ContextFactory cf) {
		super(cf);
		maxDepth = cf.getProperty(SERIALIZER_maxDepth, int.class, 100);
		initialDepth = cf.getProperty(SERIALIZER_initialDepth, int.class, 0);
		detectRecursions = cf.getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = cf.getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useIndentation = cf.getProperty(SERIALIZER_useIndentation, boolean.class, false);
		addBeanTypeProperties = cf.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, false);
		trimNulls = cf.getProperty(SERIALIZER_trimNullProperties, boolean.class, true);
		trimEmptyCollections = cf.getProperty(SERIALIZER_trimEmptyCollections, boolean.class, false);
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
