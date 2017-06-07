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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlSerializerContext.*;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.net.URI;
import java.util.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 */
@RestResource(
	path="/photos",
	messages="nls/PhotosResource",
	title="Photo REST service",
	description="Sample resource that allows images to be uploaded and retrieved.",
	htmldoc=@HtmlDoc(
		links="{up:'request:/..',options:'servlet:/?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/PhotosResource.java'}",
		aside=""
			+ "<div style='max-width:400px;min-width:200px' class='text'>"
			+ "	<p>Shows an example of using custom serializers and parsers to create REST interfaces over binary resources.</p>"
			+ "	<p>In this case, our resources are marshalled jpeg and png binary streams and are stored in an in-memory 'database' (also known as a <code>TreeMap</code>).</p>"
			+ "</div>"
	),
	properties={
		// Make the anchor text on URLs be just the path relative to the servlet.
		@Property(name=HTML_uriAnchorText, value="SERVLET_RELATIVE")
	}
)
public class PhotosResource extends Resource {
	private static final long serialVersionUID = 1L;

	// Our cache of photos
	private Map<String,Photo> photos = new TreeMap<String,Photo>();

	@Override /* Servlet */
	public void init() {
		try {
			// Preload an image.
			InputStream is = getClass().getResourceAsStream("averycutecat.jpg");
			BufferedImage image = ImageIO.read(is);
			Photo photo = new Photo("cat", image);
			photos.put(photo.id, photo);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Our bean class for storing photos */
	public static class Photo {
		String id;
		BufferedImage image;

		Photo(String id, BufferedImage image) {
			this.id = id;
			this.image = image;
		}

		public URI getURI() throws URISyntaxException {
			return new URI("servlet:/" + id);
		}
	}

	/** GET request handler for list of all photos */
	@RestMethod(name="GET", path="/", summary="Show the list of all currently loaded photos")
	public Collection<Photo> getAllPhotos() throws Exception {
		return photos.values();
	}

	/** GET request handler for single photo */
	@RestMethod(name="GET", path="/{id}", serializers=ImageSerializer.class, summary="Get a photo by ID")
	public BufferedImage getPhoto(@Path String id) throws Exception {
		Photo p = photos.get(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return p.image;
	}

	/** PUT request handler */
	@RestMethod(name="PUT", path="/{id}", parsers=ImageParser.class, summary="Add or overwrite a photo")
	public String addPhoto(@Path String id, @Body BufferedImage image) throws Exception {
		photos.put(id, new Photo(id, image));
		return "OK";
	}
	
	/** POST request handler */
	@RestMethod(name="POST", path="/", parsers=ImageParser.class, summary="Add a photo")
	public Photo setPhoto(@Body BufferedImage image) throws Exception {
		Photo p = new Photo(UUID.randomUUID().toString(), image);
		photos.put(p.id, p);
		return p;
	}

	/** DELETE request handler */
	@RestMethod(name="DELETE", path="/{id}", summary="Delete a photo by ID")
	public String deletePhoto(@Path String id) throws Exception {
		Photo p = photos.remove(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return "OK";
	}

	/** Serializer for converting images to byte streams */
	@Produces("image/png,image/jpeg")
	public static class ImageSerializer extends OutputStreamSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public ImageSerializer(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object o) throws Exception {
			RenderedImage image = (RenderedImage)o;
			String mediaType = session.getProperty("mediaType");
			ImageIO.write(image, mediaType.substring(mediaType.indexOf('/')+1), session.getOutputStream());
		}
	}

	/** Parser for converting byte streams to images */
	@Consumes("image/png,image/jpeg")
	public static class ImageParser extends InputStreamParser {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public ImageParser(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* Parser */
		@SuppressWarnings("unchecked")
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)ImageIO.read(session.getInputStream());
		}
	}
}
