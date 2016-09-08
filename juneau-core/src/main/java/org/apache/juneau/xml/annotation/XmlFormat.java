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
 * @author James Bognar (james.bognar@salesforce.com)
 */
public enum XmlFormat {

	/**
	 * Normal formatting (default)
	 */
	NORMAL,

	/**
	 * Render property as an attribute instead of an element.
	 * <p>
	 * 	Can only be applied to properties (methods/fields) of class types that can be convertible to <code>Strings</code>.
	 */
	ATTR,

	/**
	 * Render property as attributes instead of an element.
	 * <p>
	 * 	Can only be applied to properties (methods/fields) of class type <code>Map&lt;Object,Object&gt;</code> where both
	 * 	objects are convertible to <code>Strings</code>.
	 */
	ATTRS,

	/**
	 * Render property as an element instead of an attribute.
	 * <p>
	 * 	Can be applied to URL and ID bean properties that would normally be rendered as attributes.
	 */
	ELEMENT,

	/**
	 * Prevents collections and arrays from being enclosed in <xt>&lt;array&gt;</xt> elements.
	 * <p>
	 * 	Can only be applied to properties (methods/fields) of type collection or array, or collection classes.
	 */
	COLLAPSED,

	/**
	 * Render property value directly as content of element.
	 * <p>
	 * 	By default, content is converted to plain text.
	 * <p>
	 * 	Can be used in combination with {@link Xml#contentHandler()} to produce something other
	 * 	than plain text, such as embedded XML.
	 */
	CONTENT,

	/**
	 * Render a collection/array as mixed child content of the element.
	 */
	MIXED
}