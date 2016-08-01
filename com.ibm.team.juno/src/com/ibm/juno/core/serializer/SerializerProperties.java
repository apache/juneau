/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import com.ibm.juno.core.*;

/**
 * Configurable properties common to all {@link Serializer} classes.
 * <p>
 * 	Use the {@link Serializer#setProperty(String, Object)} method to set property values.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class SerializerProperties implements Cloneable {

	/**
	 * Max depth ({@link Integer}, default=<code>100</code>).
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
	 * <ul>
	 * 	<li>When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>Enables {#link SERIALIZER_detectRecursions}.
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
	 * Quote character ({@link Character}, default=<js>'"'</js>.
	 * <p>
	 * This is the character used for quoting attributes and values.
	 */
	public static final String SERIALIZER_quoteChar = "Serializer.quoteChar";

	/**
	 * Boolean.  Trim null bean property values from output.
	 * <p>
	 * 	If <jk>true</jk>, null bean values will not be serialized to the output.
	 * <p>
	 * 	Note that enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with <jk>null</jk> values will be lost.
	 * 	</ul>
	 * <p>
	 * 	Default is <jk>true</jk>.
	 */
	public static final String SERIALIZER_trimNullProperties = "Serializer.trimNullProperties";

	/**
	 * Boolean.  Trim empty lists and arrays from output.
	 * <p>
	 * 	If <jk>true</jk>, empty list values will not be serialized to the output.
	 * <p>
	 * 	Note that enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Map entries with empty list values will be lost.
	 * 		<li>Bean properties with empty list values will not be set.
	 * 	</ul>
	 * <p>
	 * 	Default is <jk>false</jk>.
	 */
	public static final String SERIALIZER_trimEmptyLists = "Serializer.trimEmptyLists";

	/**
	 * Boolean.  Trim empty maps from output.
	 * <p>
	 * 	If <jk>true</jk>, empty map values will not be serialized to the output.
	 * <p>
	 * 	Note that enabling this setting has the following effects on parsing:
	 * 	<ul>
	 * 		<li>Bean properties with empty map values will not be set.
	 * 	</ul>
	 * <p>
	 * 	Default is <jk>false</jk>.
	 */
	public static final String SERIALIZER_trimEmptyMaps = "Serializer.trimEmptyMaps";

	/**
	 * Boolean.  Trim strings in output.
	 * <p>
	 * 	If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 * <p>
	 * 	Default is <jk>false</jk>.
	 */
	public static final String SERIALIZER_trimStrings = "Serializer.trimStrings";

	/**
	 * String.  URI base for relative URIs.
	 * <p>
	 * 	Prepended to relative URIs during serialization (along with the {@link #SERIALIZER_absolutePathUriBase} if specified.
	 * 	(i.e. URIs not containing a schema and not starting with <js>'/'</js>).
	 * 	(e.g. <js>"foo/bar"</js>)
	 * <p>
	 * 	Default is <js>""</js>.
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
	 * Boolean.  Sort arrays and collections alphabetically before serializing.
	 * <p>
	 * 	Note that this introduces a performance penalty.
	 * <p>
	 * 	Default is <jk>false</jk>.
	 */
	public static final String SERIALIZER_sortCollections = "Serializer.sortCollections";

	/**
	 * Boolean.  Sort maps alphabetically before serializing.
	 * <p>
	 * 	Note that this introduces a performance penalty.
	 * <p>
	 * 	Default is <jk>false</jk>.
	 */
	public static final String SERIALIZER_sortMaps = "Serializer.sortMaps";

	/**
	 * String.  URI base for relative URIs with absolute paths.
	 * <p>
	 * 	Prepended to relative absolute-path URIs during serialization.
	 * 	(i.e. URIs starting with <js>'/'</js>).
	 * 	(e.g. <js>"/foo/bar"</js>)
	 * <p>
	 * 	Default is <js>""</js>.
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

	int maxDepth = 100, initialDepth = 0;
	boolean
		debug = false,
		detectRecursions = false,
		ignoreRecursions = false,
		useIndentation = false,
		addClassAttrs = false,
		trimNulls = true,
		trimEmptyLists = false,
		trimEmptyMaps = false,
		trimStrings = false,
		sortCollections = false,
		sortMaps = false;
	char quoteChar = '"';
	String relativeUriBase="", absolutePathUriBase="";

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(SERIALIZER_maxDepth))
			maxDepth = bc.convertToType(value, Integer.class);
		else if (property.equals(SERIALIZER_debug))
			debug = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_detectRecursions))
			detectRecursions = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_ignoreRecursions))
			ignoreRecursions = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_useIndentation))
			useIndentation = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_addClassAttrs))
			addClassAttrs = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_quoteChar))
			quoteChar = bc.convertToType(value, Character.class);
		else if (property.equals(SERIALIZER_trimNullProperties))
			trimNulls = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_trimEmptyLists))
			trimEmptyLists = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_trimEmptyMaps))
			trimEmptyMaps = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_trimStrings))
			trimStrings = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_relativeUriBase))
			relativeUriBase = value == null ? null : value.toString();
		else if (property.equals(SERIALIZER_absolutePathUriBase))
			absolutePathUriBase = value == null ? null : value.toString();
		else if (property.equals(SERIALIZER_sortCollections))
			sortCollections = bc.convertToType(value, Boolean.class);
		else if (property.equals(SERIALIZER_sortMaps))
			sortMaps = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public SerializerProperties clone() {
		try {
			return (SerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Won't happen.
		}
	}
}
