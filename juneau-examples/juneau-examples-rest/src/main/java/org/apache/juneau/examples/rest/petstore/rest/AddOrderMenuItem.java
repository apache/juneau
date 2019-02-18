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
package org.apache.juneau.examples.rest.petstore.rest;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.widget.*;

/**
 * Menu item for adding a Pet.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class AddOrderMenuItem extends MenuItemWidget {

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req, RestResponse res) throws Exception {
		return "add";
	}

	@Override /* MenuItemWidget */
	public String getBeforeShowScript(RestRequest req, RestResponse res) throws Exception {
		return loadScript("AddOrderMenuItem_beforeShow.js");
	}

	@Override /* Widget */
	public Object getContent(RestRequest req, RestResponse res) throws Exception {

		return div(
			form().id("form").action("servlet:/store/order").method(POST).children(
				table(
					tr(
						th("Pet:"),
						td(
							select().id("addPet_names").name("petId")
						),
						td(new Tooltip("&#x2753;", "The pet to purchase."))
					),
					tr(
						th("Ship date:"),
						td(input().name("shipDate").type("date")),
						td(new Tooltip("&#x2753;", "The requested ship date."))
					),
					tr(
						td().colspan(2).style("text-align:right").children(
							button("reset", "Reset"),
							button("submit", "Submit")
						)
					)
				).style("white-space:nowrap")
			)
		);
	}
}
