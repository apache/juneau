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

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Widget that returns back a list of hyperlinks for rendering the contents of a page in the various default styles.
 *
 * <p>
 * The variable it resolves is <js>"$W{StyleMenuItem}"</js>.
 *
 * <p>
 * An example of this widget can be found in the <code>PetStoreResource</code> in the examples that provides
 * a drop-down menu item for rendering all other supported content types in plain text:
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"GET"</js>,
 * 		path=<js>"/"</js>,
 * 		widgets={
 * 			StyleMenuItem.<jk>class</jk>,
 * 		},
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			links={
 * 				<js>"up: ..."</js>,
 * 				<js>"options: ..."</js>,
 * 				<js>"$W{QueryMenuItem}"</js>,
 * 				<js>"$W{ContentTypeMenuItem}"</js>,
 * 				<js>"$W{StyleMenuItem}"</js>,
 * 				<js>"source: ..."</js>
 * 			}
 * 		)
 * 	)
 * 	<jk>public</jk> Collection&lt;Pet&gt; getPets() {
 * </p>
 */
public class StyleMenuItem extends MenuItemWidget {

	private static final String[] BUILT_IN_STYLES = {"devops", "light", "original"};

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req) {
		return "styles";
	}
	/**
	 * Looks at the supported media types from the request and constructs a list of hyperlinks to render the data
	 * as plain-text.
	 */
	@Override /* Widget */
	public Div getContent(RestRequest req) throws Exception {
		Div div = div();
		for (String s : BUILT_IN_STYLES) {
			java.net.URI uri = req.getUri(true, new AMap<String,String>().append("stylesheet", "styles/"+s+".css"));
			div.children(a(uri, s), br());
		}
		return div;
	}
}
