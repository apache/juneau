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
package org.apache.juneau.petstore.rest;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.widget.*;

/**
 * Menu item for uploading a Photo.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class UploadPhotoMenuItem extends MenuItemWidget {

	@Override /* MenuItemWidget */
	public String getLabel(RestRequest req, RestResponse res) throws Exception {
		return "upload";
	}

	@Override /* Widget */
	public Object getContent(RestRequest req, RestResponse res) throws Exception {
		return div(
			form().id("form").action("servlet:/upload").method(POST).enctype("multipart/form-data").children(
				table(
					tr(
						th("ID:"),
						td(input().name("id").type("text")),
						td(new Tooltip("&#x2753;", "The unique identifier of the photo.", br(), "e.g. 'Fluffy'"))
					),
					tr(
						th("File:"),
						td(input().name("file").type("file").accept("image/*")),
						td(new Tooltip("&#x2753;", "The image file."))
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
