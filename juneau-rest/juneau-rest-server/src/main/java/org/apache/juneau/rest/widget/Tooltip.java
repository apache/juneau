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
package org.apache.juneau.rest.widget;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;

/**
 * Simple template for adding tooltips to HTML5 bean constructs, typically in menu item widgets.
 *
 * <p>
 * Tooltips depend on the existence of the <c>tooltip</c> and <c>tooltiptext</c> styles that should be present in the stylesheet for the document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * </ul>
 */
public class Tooltip {

	private final HtmlText display;
	private final List<Object> content;

	/**
	 * Constructor.
	 *
	 * @param display The normal display text. <br>
	 * 	This is what gets rendered normally. <br>
	 * 	The format is raw HTML and can contain markup.
	 * @param content The hover contents. <br>
	 * 	Typically a list of strings, but can also include any HTML5 beans as well.
	 */
	public Tooltip(String display, Object... content) {
		this.display = new HtmlText(display);
		this.content = ulist(content);
	}

	/**
	 * The swap method.
	 *
	 * <p>
	 * Converts this bean into a div tag with contents.
	 *
	 * @param session The bean session.
	 * @return The swapped contents of this bean.
	 */
	public Div swap(BeanSession session) {
		return div(small(display), span()._class("tooltiptext").children(content))._class("tooltip");
	}
}
