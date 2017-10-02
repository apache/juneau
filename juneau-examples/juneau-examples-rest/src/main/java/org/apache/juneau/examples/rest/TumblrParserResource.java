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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.Link;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;

@RestResource(
	path="/tumblrParser",
	messages="nls/TumblrParserResource",
	title="Tumblr parser service",
	description="Specify a URL to a Tumblr blog and parse the results.",
	htmldoc=@HtmlDoc(
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},
		aside={
			"<div style='min-width:200px' class='text'>",
			"	<p>An example of a REST interface that retrieves data from another REST interface.</p>",
			"	<p><a class='link' href='$U{servlet:/ibmblr}'>try me</a></p>",
			"</div>"
		}
	)
)
public class TumblrParserResource extends Resource {
	private static final long serialVersionUID = 1L;

	private static final int MAX_POSTS = 100;

	@RestMethod(name=GET, path="/", summary="Get the instructions page")
	public String getInstructions() throws Exception {
		return "Append the Tumblr blog name to the URL above (e.g. /tumblrParser/mytumblrblog)";
	}

	@RestMethod(name=GET, path="/{blogName}", summary="Parse the specified blog")
	public ObjectList parseBlog(@Path String blogName) throws Exception {
		ObjectList l = new ObjectList();
		RestClient rc = new RestClientBuilder().build();
		try {
			String site = "http://" + blogName + ".tumblr.com/api/read/json";
			ObjectMap m = rc.doGet(site).getResponse(ObjectMap.class);
			int postsTotal = Math.min(m.getInt("posts-total"), MAX_POSTS);
			for (int i = 0; i < postsTotal; i += 20) {
				m = rc.doGet(site + "?start=" + i + "&num=20&filter=text").getResponse(ObjectMap.class);
				ObjectList ol = m.getObjectList("posts");
				for (int j = 0; j < ol.size(); j++) {
					ObjectMap om = ol.getObjectMap(j);
					String type = om.getString("type");
					Entry e = new Entry();
					e.date = om.getString("date");
					if (type.equals("link"))
						e.entry = new Link(om.getString("link-text"), om.getString("link-url"));
					else if (type.equals("audio"))
						e.entry = new ObjectMap().append("type","audio").append("audio-caption", om.getString("audio-caption"));
					else if (type.equals("video"))
						e.entry = new ObjectMap().append("type","video").append("video-caption", om.getString("video-caption"));
					else if (type.equals("quote"))
						e.entry = new ObjectMap().append("type","quote").append("quote-source", om.getString("quote-source")).append("quote-text", om.getString("quote-text"));
					else if (type.equals("regular"))
						e.entry = om.getString("regular-body");
					else if (type.equals("photo"))
						e.entry = new Img().src(om.getString("photo-url-250"));
					else
						e.entry = new ObjectMap().append("type", type);
					l.add(e);
				}
			}
		} finally {
			rc.closeQuietly();
		}
		return l;
	}

	public static class Entry {
		public String date;
		public Object entry;
	}
}
