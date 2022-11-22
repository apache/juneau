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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.widget.*;

import java.awt.image.*;
import java.net.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Marshalling">REST Marshalling</a>
 * 	<li class='jc'>{@link ImageSerializer}
 * 	<li class='jc'>{@link ImageParser}
 * </ul>
 */
@Rest(
	path="/photos",
	messages="nls/PhotosResource",
	title="Photo REST service",
	description="Use a tool like Poster to upload and retrieve jpeg and png images."
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class
	},
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats",
		"$W{ContentTypeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/PhotosResource.java"
	},
	aside={
		"<div class='text'>",
		"	<p>Examples of serialized beans in the org.apache.juneau.rest.utilitybeans package.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
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

		/**
		 * The photo URL.
		 *
		 * @return The photo URL.
		 */
		public URI getURI() {
			try {
				return new URI("photos/"+id);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e); // Shouldn't happen.
			}
		}

		/**
		 * The photo ID
		 *
		 * @return The photo ID.
		 */
		public int getID() {
			return id;
		}
	}

	/**
	 * [HTTP GET /photos]
	 * GET request handler for list of all photos.
	 *
	 * @return A list of photo beans.
	 */
	@RestGet("/")
	public Collection<Photo> getAllPhotos() {
		return photos.values();
	}

	/**
	 * [HTTP GET /photos/{id}]
	 * GET request handler for single photo.
	 *
	 * @param id The photo ID.
	 * @return The photo image.
	 * @throws NotFound If photo not found.
	 */
	@RestGet(path="/{id}", serializers=ImageSerializer.class)
	public BufferedImage getPhoto(@Path("id") int id) throws NotFound {
		Photo p = photos.get(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return p.image;
	}

	/**
	 * [HTTP PUT /photos/{id}]
	 * PUT request handler.
	 *
	 * @param id The photo ID.
	 * @param image The photo image.
	 * @return OK.
	 */
	@RestPut(path="/{id}", parsers=ImageParser.class)
	public Ok addPhoto(@Path("id") int id, @Content BufferedImage image) {
		photos.put(id, new Photo(id, image));
		return Ok.OK;
	}

	/**
	 * [HTTP POST /photos]
	 * POST request handler.
	 *
	 * @param image The photo image.
	 * @return The created photo bean.
	 */
	@RestPost(path="/", parsers=ImageParser.class)
	public Photo setPhoto(@Content BufferedImage image) {
		int id = photos.size();
		Photo p = new Photo(id, image);
		photos.put(id, p);
		return p;
	}

	/**
	 * [HTTP DELETE /photos/{id}]
	 * DELETE request handler
	 *
	 * @param id The photo ID.
	 * @return OK.
	 * @throws NotFound If photo not found.
	 */
	@RestDelete("/{id}")
	public Ok deletePhoto(@Path("id") int id) throws NotFound {
		Photo p = photos.remove(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return Ok.OK;
	}
}