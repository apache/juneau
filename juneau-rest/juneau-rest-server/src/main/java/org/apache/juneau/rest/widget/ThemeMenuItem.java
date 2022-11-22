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
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;

/**
 * Widget that returns back a list of hyperlinks for rendering the contents of a page in the various default styles.
 *
 * <p>
 * The variable it resolves is <js>"$W{ThemeMenuItem}"</js>.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlPredefinedWidgets">Predefined Widgets</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.HtmlWidgets">Widgets</a>
 * </ul>
 */
public class ThemeMenuItem extends MenuItemWidget {

	private static final String[] BUILT_IN_STYLES = {"devops", "light", "original", "dark"};

	@Override /* Widget */
	public String getLabel(RestRequest req, RestResponse res) {
		return "themes";
	}

	@Override /* MenuItemWidget */
	public Div getContent(RestRequest req, RestResponse res) {
		Div div = div();
		for (String s : BUILT_IN_STYLES) {
			java.net.URI uri = req.getUri(true, CollectionUtils.<String,Object>map("stylesheet","htdocs/themes/"+s+".css"));
			div.children(a(uri, s), br());
		}
		return div;
	}
}
