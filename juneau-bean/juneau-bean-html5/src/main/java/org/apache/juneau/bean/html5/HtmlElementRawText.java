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

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.xml.*;

/**
 * A subclass of HTML elements that contain <a href="https://www.w3.org/TR/html51/syntax.html#raw-text">raw text</a>
 * only.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@SuppressWarnings("java:S119")  // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
public abstract class HtmlElementRawText<SELF extends HtmlElementRawText<SELF>> extends HtmlElement<SELF> {

	private Object text;

	/**
	 * Returns the inner text of this element.
	 *
	 * @return The inner text of this element, or <jk>null</jk> if no text is set.
	 */
	@Xml(format = XmlFormat.TEXT_PWS)
	@BeanProp("c")
	public Object getText() { return text; }

	/**
	 * Sets the inner text of this element.
	 *
	 * @param text The inner text of this element, or <jk>null</jk> if no text is set.
	 * @return This object.
	 */
	@BeanProp("c")
	public SELF setText(Object text) {
		this.text = text;
		return self();
	}

	/**
	 * Sets the text node on this element.
	 *
	 * @param value The text node to add to this element. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF text(Object value) {
		text = value;
		return self();
	}
}
