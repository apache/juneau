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

import java.util.*;

import org.apache.juneau.examples.parser.*;
import org.apache.juneau.examples.serializer.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

import java.awt.image.*;
import java.net.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.Marshalling REST Marshalling}
 * 	<li class='jc'>{@link ImageSerializer}
 * 	<li class='jc'>{@link ImageParser}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Rest(
	path="/photos",
	messages="nls/PhotosResource",
	title="Photo REST service",
	description="Use a tool like Poster to upload and retrieve jpeg and png images."
)
@HtmlDocConfig(
	navlinks="options: ?method=OPTIONS"
)
@SuppressWarnings({ "javadoc" })
public class PhotosResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	// Our cache of photos
	private Map<Integer,Photo> photos = new TreeMap<>();

	/** Bean class for storing photos */
	public static class Photo {
		private int id;
		BufferedImage image;

		Photo(int id, BufferedImage image) {
			this.id = id;
			this.image = image;
		}

		public URI getURI() throws URISyntaxException {
			return new URI("photos/"+id);
		}

		public int getID() {
			return id;
		}
	}

	/** GET request handler for list of all photos */
	@RestGet("/")
	public Collection<Photo> getAllPhotos(RestRequest req, RestResponse res) throws Exception {
		return photos.values();
	}

	/** GET request handler for single photo */
	@RestGet(path="/{id}", serializers=ImageSerializer.class)
	public BufferedImage getPhoto(RestRequest req, @Path("id") int id) throws Exception {
		Photo p = photos.get(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return p.image;
	}

	/** PUT request handler */
	@RestPut(path="/{id}", parsers=ImageParser.class)
	public String addPhoto(RestRequest req, @Path("id") int id, @Body BufferedImage image) throws Exception {
		photos.put(id, new Photo(id, image));
		return "OK";
	}

	/** POST request handler */
	@RestPost(path="/", parsers=ImageParser.class)
	public Photo setPhoto(RestRequest req, @Body BufferedImage image) throws Exception {
		int id = photos.size();
		Photo p = new Photo(id, image);
		photos.put(id, p);
		return p;
	}

	/** DELETE request handler */
	@RestDelete("/{id}")
	public String deletePhoto(RestRequest req, @Path("id") int id) throws Exception {
		Photo p = photos.remove(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return "OK";
	}
}