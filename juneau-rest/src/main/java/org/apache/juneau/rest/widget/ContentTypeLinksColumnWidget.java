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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.*;

/**
 * Widget that returns back a list of hyperlinks for rendering the contents of a page in a variety of content types.
 *
 * <p>
 * The variable it resolves is <js>"$W{contentTypeLinksColumn}"</js>.
 */
public class ContentTypeLinksColumnWidget extends Widget {

	@Override /* Widget */
	public String getName() {
		return "contentTypeLinksColumn";
	}

	@Override /* Widget */
	public String resolve(RestRequest req) throws Exception {
		UriResolver uriResolver = req.getUriResolver();
		P p = p();
		List<MediaType> l = new ArrayList<MediaType>(req.getSerializerGroup().getSupportedMediaTypes());
		Collections.sort(l);
		for (MediaType mt : l)
			p.child(a()._class("link").href(uriResolver.resolve("request:/?plainText=true&Accept="+mt)).child(mt)).child(br());
		return HtmlSerializer.DEFAULT_SQ.serialize(p);
	}

}
