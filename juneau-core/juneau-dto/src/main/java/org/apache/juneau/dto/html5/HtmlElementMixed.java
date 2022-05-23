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
package org.apache.juneau.dto.html5;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * A subclass of HTML elements that contain mixed content (elements and text).
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HtmlElementMixed extends HtmlElement {

	private LinkedList<Object> children;

	/**
	 * The children of this element.
	 *
	 * @return The children of this element.
	 */
	@Xml(format=MIXED)
	@Beanp(dictionary=HtmlBeanDictionary.class, name="c")
	public LinkedList<Object> getChildren() {
		return children;
	}

	/**
	 * Sets the children of this element.
	 *
	 * @param children The new children of this element.
	 * @return This object.
	 */
	@Beanp("c")
	public HtmlElement setChildren(LinkedList<Object> children) {
		this.children = children;
		return this;
	}

	/**
	 * Returns the child node at the specified index.
	 *
	 * @param index The index of the node in the list of children.
	 * @return The child node, or <jk>null</jk> if it doesn't exist.
	 */
	public Object getChild(int index) {
		return (children == null || children.size() <= index || index < 0 ? null : children.get(index));
	}

	/**
	 * Returns the child node at the specified address.
	 *
	 * <p>
	 * Indexes are zero-indexed.
	 *
	 * <p>
	 * For example, calling <c>getChild(1,2,3);</c> will return the 4th child of the 3rd child of the 2nd child.
	 *
	 * @param index The child indexes.
	 * @return The child node, or <jk>null</jk> if it doesn't point to a valid child.
	 */
	public Object getChild(int...index) {
		if (index.length == 0)
			return null;
		if (index.length == 1)
			return getChild(index[0]);
		Object c = this;
		for (int i = 0; i < index.length; i++) {
			if (c instanceof HtmlElementMixed)
				c = ((HtmlElementMixed)c).getChild(index[i]);
			else if (c instanceof HtmlElementContainer)
				c = ((HtmlElementContainer)c).getChild(index[i]);
			else
				return null;
		}
		return c;
	}

	/**
	 * Returns the child node at the specified index.
	 *
	 * @param <T> he class type of the node.
	 * @param type The class type of the node.
	 * @param index The index of the node in the list of children.
	 * @return The child node, or <jk>null</jk> if it doesn't exist.
	 * @throws InvalidDataConversionException If node is not the expected type.
	 */
	public <T> T getChild(Class<T> type, int index) {
		return (
			children == null || children.size() <= index || index < 0
			? null
			: ConverterUtils.toType(children.get(index), type)
		);
	}

	/**
	 * Adds one or more child elements to this element.
	 *
	 * @param children
	 * 	The children to add as child elements.
	 * 	Can be a mixture of strings and {@link HtmlElement} objects.
	 * 	Can also be containers of strings and elements.
	 * @return This object.
	 */
	public HtmlElement children(Object...children) {
		if (children.length != 0)
			for (Object c : children)
				child(c);
		return this;
	}

	/**
	 * Adds a child element to this element.
	 *
	 * @param child
	 * 	The child to add as a child element.
	 * 	Can be a string or {@link HtmlElement}.
	 * 	Can also be a container of strings and elements.
	 * @return This object.
	 */
	public HtmlElement child(Object child) {
		if (this.children == null)
			this.children = new LinkedList<>();
		if (child instanceof Collection)
			this.children.addAll((Collection<?>)child);
		else
			this.children.add(child);
		return this;
	}
}
