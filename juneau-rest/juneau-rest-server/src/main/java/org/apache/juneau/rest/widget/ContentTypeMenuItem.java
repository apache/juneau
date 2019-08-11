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

import java.net.*;
import java.util.*;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Widget that returns back a list of hyperlinks for rendering the contents of a page in a variety of content types.
 *
 * <p>
 * The variable it resolves is <js>"$W{ContentTypeMenuItem}"</js>.
 *
 * <p>
 * An example of this widget can be found in the <c>PetStoreResource</c> in the examples that provides
 * a drop-down menu item for rendering all other supported content types in plain text:
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<jsf>GET</jsf>,
 * 		path=<js>"/"</js>,
 * 		widgets={
 * 			ContentTypeMenuItem.<jk>class</jk>,
 * 		},
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			navlinks={
 * 				<js>"up: ..."</js>,
 * 				<js>"options: ..."</js>,
 * 				<js>"$W{QueryMenuItem}"</js>,
 * 				<js>"$W{ContentTypeMenuItem}"</js>,
 * 				<js>"$W{ThemeMenuItem}"</js>,
 * 				<js>"source: ..."</js>
 * 			}
 * 		)
 * 	)
 * 	<jk>public</jk> Collection&lt;Pet&gt; getPets() {
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation.PredefinedWidgets}
 * </ul>
 */
public class ContentTypeMenuItem extends MenuItemWidget {

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req, RestResponse res) {
		return "content-type";
	}

	@Override /* MenuItemWidget */
	public Div getContent(RestRequest req, RestResponse res) {
		Div div = div();
		Set<MediaType> l = new TreeSet<>();
		for (Serializer s : req.getSerializers().getSerializers())
			l.add(s.getPrimaryMediaType());
		for (MediaType mt : l) {
			URI uri = req.getUri(true, new AMap<String,String>().append("plainText","true").append("Accept",mt.toString()));
			div.children(a(uri, mt), br());
		}
		return div;
	}
}
