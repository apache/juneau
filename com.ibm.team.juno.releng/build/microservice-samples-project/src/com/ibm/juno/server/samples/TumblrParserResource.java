/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.dto.*;
import com.ibm.juno.core.html.dto.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.annotation.*;

@RestResource(
	path="/tumblrParser",
	messages="nls/TumblrParserResource",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.TumblrParserResource)'}"),
		@Property(name=HTMLDOC_title, value="Tumblr parser service"),
		@Property(name=HTMLDOC_description, value="Specify a URL to a Tumblr blog and parse the results.")
	}
)
public class TumblrParserResource extends Resource {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/")
	public String getInstructions() throws Exception {
		return "Append the Tumblr blog name to the URL above (e.g. /juno/sample/tumblrParser/mytumblrblog)";
	}

	@RestMethod(name="GET", path="/{blogName}")
	public ObjectList parseBlog(@Attr String blogName) throws Exception {
		ObjectList l = new ObjectList();
		RestClient rc = new RestClient(JsonSerializer.class, JsonParser.class);
		String site = "http://" + blogName + ".tumblr.com/api/read/json";
		ObjectMap m = rc.doGet(site).getResponse(ObjectMap.class);
		int postsTotal = m.getInt("posts-total");
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
					e.entry = new Img(om.getString("photo-url-250"));
				else
					e.entry = new ObjectMap().append("type", type);
				l.add(e);
			}
		}
		return l;
	}

	public static class Entry {
		public String date;
		public Object entry;
	}
}
