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

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * A subclass of HTML elements that contain text only.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Html5}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HtmlElementText extends HtmlElement {

	private Object text;

	/**
	 * Returns the inner text of this element.
	 *
	 * @return The inner text of this element, or <jk>null</jk> if no text is set.
	 */
	@Xml(format=XmlFormat.TEXT)
	@Beanp("c")
	public Object getText() {
		return text;
	}

	/**
	 * Sets the inner text of this element.
	 *
	 * @param text The inner text of this element, or <jk>null</jk> if no text is set.
	 * @return This object.
	 */
	@Beanp("c")
	public HtmlElement setText(Object text) {
		this.text = text;
		return this;
	}

	/**
	 * Sets the text node on this element.
	 *
	 * @param text The text node to add to this element.
	 * @return This object.
	 */
	public HtmlElement text(Object text) {
		this.text = text;
		return this;
	}
}
