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

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;

import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;

/**
 * A subclass of widgets for rendering menu items with drop-down windows.
 *
 * <p>
 * Defines some simple CSS and Javascript for enabling drop-down menus in the nav section of the page (although
 * nothing keeps you from using it in an arbitrary location in the page).
 *
 * <p>
 * The script specifies a <js>"menuClick(element)"</js> function that toggles the visibility of the next sibling of the
 * element.
 *
 * <p>
 * Subclasses should implement the following two methods:
 * <ul>
 * 	<li class='jm'>{@link #getLabel(RestRequest)} - The menu item label.
 * 	<li class='jm'>{@link #getContent(RestRequest)} - The menu item content.
 * </ul>
 *
 * <p>
 * For example, to render a link that brings up a simple dialog in a div tag:
 * <p class='bcode w800'>
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> String getLabel() {
 * 		<jk>return</jk> <js>"my-menu-item"</js>;
 * 	};
 *
 * 	<ja>@Override</ja>
 * 	<jk>public</jk> Div getLabel() {
 * 		<jk>return</jk> Html5Builder.<jsm>div</jsm>(<js>"Surprise!"</js>).style(<js>"color:red"</js>);
 * 	};
 * </p>
 *
 * <p>
 * The HTML content returned by the {@link #getHtml(RestRequest)} method is added where the <js>"$W{...}"</js> is
 * referenced in the page.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Widgets">Overview &gt; juneau-rest-server &gt; Widgets</a>
 * </ul>
 */
public abstract class MenuItemWidget extends Widget {

	/**
	 * Returns the Javascript needed for the show and hide actions of the menu item.
	 */
	@Override /* Widget */
	public String getScript(RestRequest req) throws Exception {
		return loadScript("MenuItemWidget.js");
	}

	/**
	 * Optional Javascript to execute immediately before a menu item is shown.
	 *
	 * <p>
	 * For example, the following shows how the method could be used to make an AJAX call back to the REST
	 * interface to populate a SELECT element in the contents of the popup dialog:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@Override</ja>
	 * 	<jk>public</jk> String getBeforeShowScript(RestRequest req) {
	 * 		<jk>return</jk> <js>""</js>
	 * 			+ <js>"\n	var xhr = new XMLHttpRequest();"</js>
	 * 			+ <js>"\n	xhr.open('GET', '/petstore/pet?s=status=AVAILABLE&v=id,name', true);"</js>
	 * 			+ <js>"\n	xhr.setRequestHeader('Accept', 'application/json');"</js>
	 * 			+ <js>"\n	xhr.onload = function() {"</js>
	 * 			+ <js>"\n       var pets = JSON.parse(xhr.responseText);"</js>
	 * 			+ <js>"\n 		var select = document.getElementById('addPet_names');"</js>
	 * 			+ <js>"\n 		select.innerHTML = '';"</js>
	 * 			+ <js>"\n 		for (var i in pets) {"</js>
	 * 			+ <js>"\n 			var pet = pets[i];"</js>
	 * 			+ <js>"\n 			var opt = document.createElement('option');"</js>
	 * 			+ <js>"\n 			opt.value = pet.id;"</js>
	 * 			+ <js>"\n 			opt.innerHTML = pet.name;"</js>
	 * 			+ <js>"\n 			select.appendChild(opt);"</js>
	 * 			+ <js>"\n 		}"</js>
	 * 			+ <js>"\n	}"</js>
	 * 			+ <js>"\n	xhr.send();"</js>
	 * 		;
	 * 	}
	 * </p>
	 *
	 * <p>
	 * Note that it's often easier (and cleaner) to use the {@link #loadScript(String)} method and read the Javascript from
	 * your classpath:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@Override</ja>
	 * 	<jk>public</jk> String getBeforeShowScript(RestRequest req) <jk>throws</jk> Exception {
	 * 		<jk>return</jk> loadScript(<js>"AddOrderMenuItem_beforeShow.js"</js>);
	 * 	}
	 * </p>
	 *
	 * @param req The current request.
	 * @return Javascript code to execute, or <jk>null</jk> if there isn't any.
	 * @throws Exception
	 */
	public String getBeforeShowScript(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Optional Javascript to execute immediately after a menu item is shown.
	 *
	 * <p>
	 * Same as {@link #getBeforeShowScript(RestRequest)} except this Javascript gets executed after the popup dialog has become visible.
	 *
	 * @param req The current request.
	 * @return Javascript code to execute, or <jk>null</jk> if there isn't any.
	 * @throws Exception
	 */
	public String getAfterShowScript(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Defines a <js>"menu-item"</js> class that needs to be used on the outer element of the HTML returned by the
	 * {@link #getHtml(RestRequest)} method.
	 */
	@Override /* Widget */
	public String getStyle(RestRequest req) throws Exception {
		return loadStyle("MenuItemWidget.css");
	}

	@Override /* Widget */
	public String getHtml(RestRequest req) throws Exception {
		StringBuilder sb = new StringBuilder();

		// Need a unique number to define unique function names.
		Integer id = null;

		String pre = nullIfEmpty(getBeforeShowScript(req)), post = nullIfEmpty(getAfterShowScript(req));

		sb.append("\n<div class='menu-item'>");
		if (pre != null || post != null) {
			id = getId(req);

			sb.append("\n\t<script>");
			if (pre != null) {
				sb.append("\n\t\tfunction onPreShow" + id + "() {");
				sb.append("\n").append(pre);
				sb.append("\n\t\t}");
			}
			if (post != null) {
				sb.append("\n\t\tfunction onPostShow" + id + "() {");
				sb.append("\n").append(pre);
				sb.append("\n\t\t}");
			}
			sb.append("\n\t</script>");
		}
		String onclick = (pre == null ? "" : "onPreShow"+id+"();") + "menuClick(this);" + (post == null ? "" : "onPostShow"+id+"();");
		sb.append(""
			+ "\n\t<a onclick='"+onclick+"'>"+getLabel(req)+"</a>"
			+ "\n<div class='popup-content'>"
		);
		Object o = getContent(req);
		if (o instanceof Reader) {
			try (Reader r = (Reader)o; Writer w = new StringBuilderWriter(sb)) {
				IOUtils.pipe(r, w);
			}
		} else if (o instanceof CharSequence) {
			sb.append((CharSequence)o);
		} else {
			SerializerSessionArgs args = new SerializerSessionArgs(req.getProperties(), null, req.getLocale(), null, null, req.isDebug() ? true : null, req.getUriContext(), req.isPlainText() ? true : null);
			WriterSerializerSession session = HtmlSerializer.DEFAULT.createSession(args);
			session.indent = 2;
			session.serialize(o, sb);
		}
		sb.append(""
			+ "\n\t</div>"
			+ "\n</div>"
		);
		return sb.toString();
	}

	private Integer getId(RestRequest req) {
		Integer id = (Integer)req.getAttribute("LastMenuItemId");
		if (id == null)
			id = 1;
		else
			id = id + 1;
		req.setAttribute("LastMenuItemId", id);
		return id;
	}

	/**
	 * The label for the menu item as it's rendered in the menu bar.
	 *
	 * @param req The HTTP request object.
	 * @return The menu item label.
	 * @throws Exception
	 */
	public abstract String getLabel(RestRequest req) throws Exception;

	/**
	 * The content of the popup.
	 *
	 * @param req The HTTP request object.
	 * @return
	 * 	The content of the popup.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Reader} - Serialized directly to the output.
	 * 		<li>{@link CharSequence} - Serialized directly to the output.
	 * 		<li>Other - Serialized as HTML using {@link HtmlSerializer#DEFAULT}.
	 * 			<br>Note that this includes any of the {@link org.apache.juneau.dto.html5} beans.
	 * 	</ul>
	 * @throws Exception
	 */
	public abstract Object getContent(RestRequest req) throws Exception;
}
