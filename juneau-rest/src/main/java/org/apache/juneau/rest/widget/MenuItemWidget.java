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

import org.apache.juneau.rest.*;

/**
 * A subclass of widgets for rendering menu items with drop-down windows.
 *
 * <p>
 * Defines some simple CSS and Javascript for enabling drop-down menus in the nav section of the page (although
 * nothing keeps you from using it in an arbirary location in the page).
 *
 * <p>
 * The script specifies a <js>"menuClick(element)"</js> function that toggles the visibility of the next sibling of the
 * element.
 *
 * <p>
 * Subclasses should implement a {@link #getHtml(RestRequest)} that returns the following content:
 * <p class='bcode'>
 * 	<xt>&lt;div</xt> <xa>class</xa>=<xs>'menu-item'</xs><xt>&gt;</xt>
 * 		<xc>&lt;!-- Normally visible content with onclick='menuClick(this)' --&gt;</xc>
 * 		<xt>&lt;div</xt> <xa>class</xa>=<xs>'popup-content'</xs><xt>&gt;</xt>
 *				<xc>&lt;!-- Normally hidden popup-content --&gt;</xc>
 * 		<xt>&lt;/div&gt;</xt>
 * 	<xt>&lt;/div&gt;</xt>
 * </p>
 *
 * <p>
 * For example, to render a link that brings up a simple dialog:
 * <p class='bcode'>
 * 	<xt>&lt;div</xt> <xa>class</xa>=<xs>'menu-item'</xs><xt>&gt;</xt>
 * 		<xt>&lt;a</xt> <xa>class</xa>=<xs>'link'</xs> <xa>onclick</xa>=<xs>'menuClick(this)'</xs><xt>&gt;</xt>my-menu-item<xt>&lt;/a&gt;</xt>
 * 		<xt>&lt;div</xt> <xa>class</xa>=<xs>'popup-content'</xs><xt>&gt;</xt>
 *				Surprise!
 * 		<xt>&lt;/div&gt;</xt>
 * 	<xt>&lt;/div&gt;</xt>
 * </p>
 *
 * <p>
 * The HTML content returned by the {@link #getHtml(RestRequest)} method is added where the <js>"$W{...}"</js> is
 * referenced in the page.
 */
public abstract class MenuItemWidget extends Widget {

	/**
	 * Returns the Javascript needed for the show and hide actions of the menu item.
	 */
	@Override
	public String getScript(RestRequest req) throws Exception {
		return getResourceAsString("MenuItemWidget.js").replaceAll("(?s)\\/\\*{2}(.*?)\\/\\*\\s*", "");
	}

	/**
	 * Defines a <js>"menu-item"</js> class that needs to be used on the outer element of the HTML returned by the
	 * {@link #getHtml(RestRequest)} method.
	 */
	@Override
	public String getStyle(RestRequest req) throws Exception {
		return getResourceAsString("MenuItemWidget.css").replaceAll("(?s)\\/\\*{2}(.*?)\\/\\*\\s*", "");
	}
}
