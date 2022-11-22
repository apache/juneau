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
package org.apache.juneau.xml.annotation;

/**
 * XML format to use when serializing a POJO.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
public enum XmlFormat {

	/**
	 * Normal formatting (default).
	 *
	 * <p>
	 * On a bean class, implies {@link #ELEMENTS} meaning bean properties will be serialized as child elements by default.
	 *
	 * <p>
	 * On a bean property, implies {@link #ELEMENT} meaning the bean property will be serialized as a child element.
	 */
	DEFAULT,

	/**
	 * Render a bean property as an attribute instead of an element.
	 *
	 * <p>
	 * Only applicable for bean properties, not bean classes.
	 *
	 * <p>
	 * Can only be applied to properties (methods/fields) of class types that can be convertible to <c>Strings</c>.
	 */
	ATTR,

	/**
	 * Render property as attributes instead of an element.
	 *
	 * <p>
	 * On a bean class, implies bean properties will be serialized as attributes instead of child elements by default.
	 *
	 * <p>
	 * On bean properties, implies that the bean property value itself should be serialized as attributes on the bean
	 * element.
	 * The bean property data type must be of class type <c>Map&lt;Object,Object&gt;</c> where both
	 * objects are convertible to <c>Strings</c>.
	 */
	ATTRS,

	/**
	 * Render property as an element instead of an attribute.
	 *
	 * <p>
	 * Only applicable for bean properties, not bean classes.
	 *
	 * <p>
	 * Used to override the behavior of the {@link #ATTRS} format applied to the bean class.
	 */
	ELEMENT,

	/**
	 * Render property value directly as the contents of the element.
	 *
	 * <p>
	 * On a bean class, implies that bean properties will be serialized as child elements.
	 * Note that this is equivalent to {@link #DEFAULT}.
	 *
	 * <p>
	 * Only applicable for objects of type array/Collection.
	 *
	 * <p>
	 * On a bean property, implies that the bean property value itself should be serialized as child elements of the
	 * bean element.
	 */
	ELEMENTS,

	/**
	 * Same as {@link #ELEMENTS} except primitive types (string/boolean/number/null for example) are not wrapped in elements.
	 *
	 * <p>
	 * Only applicable for bean properties, not bean classes.
	 *
	 * <p>
	 * Only applicable for objects of type array/Collection.
	 *
	 * <p>
	 * Use of this format may cause data type loss during parsing if the types cannot be inferred through reflection.
	 */
	MIXED,

	/**
	 * Same as {@link XmlFormat#MIXED}, but whitespace in text nodes are not trimmed during parsing.
	 *
	 * <p>
	 * An example use is HTML5 <xt>&lt;pre&gt;</xt> where whitespace should not be discarded.
	 */
	MIXED_PWS,

	/**
	 * Render property value as the text content of the element.
	 *
	 * <p>
	 * Similar to {@link #MIXED} but value must be a single value, not a collection.
	 *
	 * <p>
	 * Only applicable for bean properties, not bean classes.
	 *
	 * <p>
	 * Use of this format may cause data type loss during parsing if the type cannot be inferred through reflection.
	 */
	TEXT,

	/**
	 * Same as {@link XmlFormat#TEXT}, but whitespace in text node is not trimmed during parsing.
	 */
	TEXT_PWS,

	/**
	 * Same as {@link #TEXT} except the content is expected to be fully-formed XML that will get serialized as-is.
	 *
	 * <p>
	 * During parsing, this XML text will be re-serialized and set on the property.
	 *
	 * <p>
	 * Only applicable for bean properties, not bean classes.
	 *
	 * <p>
	 * Use of this format may cause data type loss during parsing if the type cannot be inferred through reflection.
	 */
	XMLTEXT,

	/**
	 * Prevents collections and arrays from being enclosed in <xt>&lt;array&gt;</xt> elements.
	 *
	 * <p>
	 * Can only be applied to properties (methods/fields) of type collection or array, or collection classes.
	 */
	COLLAPSED,

	/**
	 * Identifies a void element.
	 *
	 * <p>
	 * Only applicable for bean classes.
	 *
	 * <p>
	 * Identifies an element that never contains content.
	 *
	 * <p>
	 * The main difference in behavior is how non-void empty elements are handled in the HTML serializer.
	 * Void elements are serialized as collapsed nodes (e.g. <js>"&lt;br/&gt;"</js>) whereas non-void empty elements are
	 * serialized with an end tag (e.g. "&lt;p&gt;&lt;/p&gt;").
	 */
	VOID;

	/**
	 * Returns <jk>true</jk> if this format is one of those specified.
	 *
	 * @param formats The formats to match against.
	 * @return <jk>true</jk> if this format is one of those specified.
	 */
	public boolean isOneOf(XmlFormat...formats) {
		for (XmlFormat format : formats)
			if (format == this)
				return true;
		return false;
	}
}