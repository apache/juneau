/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.html5;

import static org.apache.juneau.marshall.xml.XmlFormat.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.xml.*;

/**
 * A subclass of HTML elements that contain only other elements, not text.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@SuppressWarnings("java:S119")  // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
public abstract class HtmlElementContainer<SELF extends HtmlElementContainer<SELF>> extends HtmlElement<SELF> {

	private List<Object> children;

	/**
	 * Adds a child element to this element.
	 *
	 * @param value The child to add as a child element. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF child(Object value) {
		if (this.children == null)
			this.children = new LinkedList<>();
		this.children.add(value);
		return self();
	}

	/**
	 * Adds one or more child elements to this element.
	 *
	 * @param value The children to add as child elements. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF children(Object...value) {
		if (value.length > 0) {
			if (this.children == null)
				this.children = new LinkedList<>();
			for (var c : value)
				this.children.add(c);
		}
		return self();
	}

	/**
	 * Returns the child node at the specified index.
	 *
	 * @param <T> The class type of the node.
	 * @param type The class type of the node.
	 * @param index The index of the node in the list of children.
	 * @return The child node, or <jk>null</jk> if it doesn't exist.
	 * @throws InvalidConversionException If node is not the expected type.
	 */
	public <T> T getChild(Class<T> type, int index) {
		return (children == null || children.size() <= index || index < 0 ? null : BasicConverter.INSTANCE.to(children.get(index), type));
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
		for (var element : index) {
			if (c instanceof HtmlElementMixed<?> c2)
				c = c2.getChild(element);
			else if (c instanceof HtmlElementContainer<?> c2)
				c = c2.getChild(element);
			else
				return null;
		}
		return c;
	}

	/**
	 * The children of this element.
	 *
	 * @return The children of this element, or <jk>null</jk> if no children are set.
	 */
	@Xml(format = ELEMENTS)
	@BeanProp(name="c") @MarshalledProp(dictionary=HtmlBeanDictionary.class)
	public List<Object> getChildren() { return u(children); }

	/**
	 * Sets the children for this container.
	 *
	 * @param children The new children for this container. Can be <jk>null</jk>.
	 * @return This object.
	 */
	@BeanProp("c")
	public SELF setChildren(List<Object> children) {
		this.children = children;
		return self();
	}
}
