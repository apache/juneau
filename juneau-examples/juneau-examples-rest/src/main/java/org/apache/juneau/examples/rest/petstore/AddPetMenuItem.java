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
package org.apache.juneau.examples.rest.petstore;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.widget.*;

/**
 * Menu item for adding a Pet.
 */
public class AddPetMenuItem extends MenuItemWidget {

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req) throws Exception {
		return "add";
	}

	@Override /* Widget */
	public Object getContent(RestRequest req) throws Exception {
		return div(
			form().id("form").action("servlet:/").method(POST).children(
				table(
					tr(
						th("Name:"),
						td(input().name("name").type("text")),
						td(new Tooltip("(?)", "The name of the pet.", br(), "e.g. 'Fluffy'")) 
					),
					tr(
						th("Species:"),
						td(
							select().name("kind").children(
								option("CAT"), option("DOG"), option("BIRD"), option("FISH"), option("MOUSE"), option("RABBIT"), option("SNAKE")
							)
						),
						td(new Tooltip("(?)", "The kind of animal.")) 
					),
					tr(
						th("Price:"),
						td(input().name("price").type("number").placeholder("1.0").step("0.01").min(1).max(100)),
						td(new Tooltip("(?)", "The price to charge for this pet.")) 
					),
					tr(
						td().colspan(2).style("text-align:right").children(
							button("reset", "Reset"),
							button("button","Cancel").onclick("window.location.href='/'"),
							button("submit", "Submit")
						)
					)
				).style("white-space:nowrap")
			)
		);
	}
}
