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
import org.apache.juneau.rest.converters.*;

/**
 * Widget that returns a menu-item drop-down form for entering search/view/sort arguments.
 * 
 * <p>
 * The variable it resolves is <js>"$W{QueryMenuItem}"</js>.
 * 
 * <p>
 * This widget is designed to be used in conjunction with the {@link Queryable} converter, although implementations
 * can process the query parameters themselves if they wish to do so by using the {@link RequestQuery#getSearchArgs()}
 * method to retrieve the arguments and process the data themselves.
 * 
 * <p>
 * An example of this widget can be found in the <code>PetStoreResource</code> in the examples that provides
 * search/view/sort capabilities against the collection of POJOs:
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<jsf>GET</jsf>,
 * 		path=<js>"/"</js>,
 * 		widgets={
 * 			QueryMenuItem.<jk>class</jk>,
 * 		},
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			navlinks={
 * 				<js>"up: ..."</js>,
 * 				<js>"options: ..."</js>,
 * 				<js>"$W{QueryMenuItem}"</js>,
 * 				<js>"$W{ContentTypeMenuItem}"</js>,
 * 				<js>"$W{StyleMenuItem}"</js>,
 * 				<js>"source: ..."</js>
 * 			}
 * 		),
 * 		converters=Queryable.<jk>class</jk>
 * 	)
 * 	<jk>public</jk> Collection&lt;Pet&gt; getPets() {
 * </p>
 * 
 * <p>
 * It renders the following popup-box:
 * <br><img class='bordered' src='doc-files/QueryMenuItem_1.png'>
 * 
 * <p>
 * Tooltips are provided by hovering over the field names.
 * <br><img class='bordered' src='doc-files/QueryMenuItem_2.png'>
 * 
 * <p>
 * When submitted, the form submits a GET request against the current URI with special GET search API query parameters.
 * <br>(e.g. <js>"?s=column1=Foo*&amp;v=column1,column2&amp;o=column1,column2-&amp;p=100&amp;l=100"</js>).
 * <br>The {@link Queryable} class knows how to perform these filters against collections of POJOs.
 */
public class QueryMenuItem extends MenuItemWidget {

	/**
	 * Returns CSS for the tooltips.
	 */
	@Override
	public String getStyle(RestRequest req) throws Exception {
		return super.getStyle(req)
			+ "\n"
			+ loadStyle("QueryMenuItem.css");
	}

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req) throws Exception {
		return "query";
	}

	@Override /* MenuItemWidget */
	public String getContent(RestRequest req) throws Exception {
		return loadHtml("QueryMenuItem.html");
	}
}
