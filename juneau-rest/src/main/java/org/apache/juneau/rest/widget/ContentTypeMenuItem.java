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

import java.net.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Widget that returns back a list of hyperlinks for rendering the contents of a page in a variety of content types.
 *
 * <p>
 * The variable it resolves is <js>"$W{contentTypeMenuItem}"</js>.
 *
 * <p>
 * An example of this widget can be found in the <code>PetStoreResource</code> in the examples that provides
 * a drop-down menu item for rendering all other supported content types in plain text:
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"GET"</js>,
 * 		path=<js>"/"</js>,
 * 		widgets={
 * 			ContentTypeMenuItem.<jk>class</jk>,
 * 		},
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			links={
 * 				<js>"up: ..."</js>,
 * 				<js>"options: ..."</js>,
 * 				<js>"$W{queryMenuItem}"</js>,
 * 				<js>"$W{contentTypeMenuItem}"</js>,
 * 				<js>"$W{styleMenuItem}"</js>,
 * 				<js>"source: ..."</js>
 * 			}
 * 		)
 * 	)
 * 	<jk>public</jk> Collection&lt;Pet&gt; getPets() {
 * </p>
 *
 * <p>
 * It renders the following popup-box:
 * <br><img class='bordered' src='doc-files/ContentTypeMenuItem.png'>
 */
public class ContentTypeMenuItem extends MenuItemWidget {

	/**
	 * Returns <js>"contentTypeMenuItem"</js>.
	 */
	@Override /* Widget */
	public String getName() {
		return "contentTypeMenuItem";
	}

	/**
	 * Looks at the supported media types from the request and constructs a list of hyperlinks to render the data
	 * as plain-text.
	 */
	@Override /* Widget */
	public String getHtml(RestRequest req) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(""
			+ "<div class='menu-item'>"
			+ "\n\t<a class='link' onclick='menuClick(this)'>content-types</a>"
			+ "\n\t<div class='popup-content'>"
		);
		List<MediaType> l = new ArrayList<MediaType>(req.getSerializerGroup().getSupportedMediaTypes());
		Collections.sort(l);
		for (MediaType mt : l) {
			URI uri = req.getUri(true, new AMap<String,String>().append("plainText","true").append("Accept",mt.toString()));
			sb.append("\n\t\t<a class='link' href='").append(uri).append("'>").append(mt).append("</a><br>");
		}
		sb.append(""
			+ "\n\t</div>"
			+ "\n</div>"
		);
		return sb.toString();
	}
}
