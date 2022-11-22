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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;

import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;

/**
 * A subclass of widgets for rendering menu items with drop-down windows.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * </ul>
 */
public abstract class MenuItemWidget extends Widget {

	/**
	 * Returns the Javascript needed for the show and hide actions of the menu item.
	 */
	@Override /* Widget */
	public String getScript(RestRequest req, RestResponse res) {
		return loadScript(req, "scripts/MenuItemWidget.js");
	}

	/**
	 * Optional Javascript to execute immediately before a menu item is shown.
	 *
	 * <p>
	 * For example, the following shows how the method could be used to make an AJAX call back to the REST
	 * interface to populate a SELECT element in the contents of the popup dialog:
	 *
	 * <p class='bjava'>
	 * 	<ja>@Override</ja>
	 * 	<jk>public</jk> String getBeforeShowScript(RestRequest <jv>req</jv>) {
	 * 		<jk>return</jk> <js>""</js>
	 * 			+ <js>"\n	var xhr = new XMLHttpRequest();"</js>
	 * 			+ <js>"\n	xhr.open('GET', '/petstore/pet?s=status=AVAILABLE&amp;v=id,name', true);"</js>
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
	 * Note that it's often easier (and cleaner) to use the {@link #loadScript(RestRequest,String)} method and read the Javascript from
	 * your classpath:
	 *
	 * <p class='bjava'>
	 * 	<ja>@Override</ja>
	 * 	<jk>public</jk> String getBeforeShowScript(RestRequest <jv>req</jv>) <jk>throws</jk> Exception {
	 * 		<jk>return</jk> loadScript(<js>"AddOrderMenuItem_beforeShow.js"</js>);
	 * 	}
	 * </p>
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @return Javascript code to execute, or <jk>null</jk> if there isn't any.
	 */
	public String getBeforeShowScript(RestRequest req, RestResponse res) {
		return null;
	}

	/**
	 * Optional Javascript to execute immediately after a menu item is shown.
	 *
	 * <p>
	 * Same as {@link #getBeforeShowScript(RestRequest,RestResponse)} except this Javascript gets executed after the popup dialog has become visible.
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @return Javascript code to execute, or <jk>null</jk> if there isn't any.
	 */
	public String getAfterShowScript(RestRequest req, RestResponse res) {
		return null;
	}

	/**
	 * Defines a <js>"menu-item"</js> class that needs to be used on the outer element of the HTML returned by the
	 * {@link #getHtml(RestRequest,RestResponse)} method.
	 */
	@Override /* Widget */
	public String getStyle(RestRequest req, RestResponse res) {
		return loadStyle(req, "styles/MenuItemWidget.css");
	}

	@Override /* Widget */
	public String getHtml(RestRequest req, RestResponse res) {
		StringBuilder sb = new StringBuilder();

		// Need a unique number to define unique function names.
		Integer id = null;

		String pre = nullIfEmpty(getBeforeShowScript(req, res)), post = nullIfEmpty(getAfterShowScript(req, res));

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
			+ "\n\t<a onclick='"+onclick+"'>"+getLabel(req, res)+"</a>"
			+ "\n<div class='popup-content'>"
		);
		Object o = getContent(req, res);
		if (o instanceof Reader) {
			try (Reader r = (Reader)o; Writer w = new StringBuilderWriter(sb)) {
				pipe(r, w);
			} catch (IOException e) {
				throw asRuntimeException(e);
			}
		} else if (o instanceof CharSequence) {
			sb.append((CharSequence)o);
		} else {
			WriterSerializerSession session = HtmlSerializer.DEFAULT
				.createSession()
				.properties(req.getAttributes().asMap())
				.debug(req.isDebug() ? true : null)
				.uriContext(req.getUriContext())
				.useWhitespace(req.isPlainText() ? true : null)
				.resolver(req.getVarResolverSession())
				.build();
			session.indent = 2;
			try {
				session.serialize(o, sb);
			} catch (Exception e) {
				throw asRuntimeException(e);
			}
		}
		sb.append(""
			+ "\n\t</div>"
			+ "\n</div>"
		);
		return sb.toString();
	}

	private Integer getId(RestRequest req) {
		Integer id = req.getAttribute("LastMenuItemId").as(Integer.class).orElse(0) + 1;
		req.setAttribute("LastMenuItemId", id);
		return id;
	}

	/**
	 * The label for the menu item as it's rendered in the menu bar.
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @return The menu item label.
	 */
	public abstract String getLabel(RestRequest req, RestResponse res);

	/**
	 * The content of the popup.
	 *
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @return
	 * 	The content of the popup.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Reader} - Serialized directly to the output.
	 * 		<li>{@link CharSequence} - Serialized directly to the output.
	 * 		<li>Other - Serialized as HTML using {@link HtmlSerializer#DEFAULT}.
	 * 			<br>Note that this includes any of the {@link org.apache.juneau.dto.html5} beans.
	 * 	</ul>
	 */
	public abstract Object getContent(RestRequest req, RestResponse res);
}
