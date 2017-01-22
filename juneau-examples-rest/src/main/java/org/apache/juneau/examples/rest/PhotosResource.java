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
import static org.apache.juneau.html.HtmlDocSerializerContext.*;

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
	properties={
		@Property(name=HTMLDOC_title, value="Photo REST service"),
		@Property(name=HTMLDOC_description, value="Use a tool like Poster to upload and retrieve jpeg and png images."),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.examples.rest.PhotosResource)'}"),
		// Resolve all relative URIs so that they're relative to this servlet!
		@Property(name=SERIALIZER_relativeUriBase, value="$R{servletURI}"),
	}
)
public class PhotosResource extends Resource {
	private static final long serialVersionUID = 1L;

	// Our cache of photos
	private Map<Integer,Photo> photos = new TreeMap<Integer,Photo>();

	@Override /* Servlet */
	public void init() {
		try {
			// Preload an image.
			InputStream is = getClass().getResourceAsStream("averycutecat.jpg");
			BufferedImage image = ImageIO.read(is);
			Photo photo = new Photo(0, image);
			photos.put(photo.id, photo);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Our bean class for storing photos */
	public static class Photo {
		int id;
		BufferedImage image;

		Photo(int id, BufferedImage image) {
			this.id = id;
			this.image = image;
		}

		public URI getURI() throws URISyntaxException {
			return new URI(""+id);
		}
	}

	/** GET request handler for list of all photos */
	@RestMethod(name="GET", path="/")
	public Collection<Photo> getAllPhotos() throws Exception {
		return photos.values();
	}

	/** GET request handler for single photo */
	@RestMethod(name="GET", path="/{id}", serializers=ImageSerializer.class)
	public BufferedImage getPhoto(@Path int id) throws Exception {
		Photo p = photos.get(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return p.image;
	}

	/** PUT request handler */
	@RestMethod(name="PUT", path="/{id}", parsers=ImageParser.class)
	public String addPhoto(@Path int id, @Body BufferedImage image) throws Exception {
		photos.put(id, new Photo(id, image));
		return "OK";
	}

	/** POST request handler */
	@RestMethod(name="POST", path="/", parsers=ImageParser.class)
	public Photo setPhoto(@Body BufferedImage image) throws Exception {
		int id = photos.size();
		Photo p = new Photo(id, image);
		photos.put(id, p);
		return p;
	}

	/** DELETE request handler */
	@RestMethod(name="DELETE", path="/{id}")
	public String deletePhoto(@Path int id) throws Exception {
		Photo p = photos.remove(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return "OK";
	}

	/** Serializer for converting images to byte streams */
	@Produces("image/png,image/jpeg")
	public static class ImageSerializer extends OutputStreamSerializer {
		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object o) throws Exception {
			RenderedImage image = (RenderedImage)o;
			String mediaType = session.getProperties().getString("mediaType");
			ImageIO.write(image, mediaType.substring(mediaType.indexOf('/')+1), session.getOutputStream());
		}
	}

	/** Parser for converting byte streams to images */
	@Consumes("image/png,image/jpeg")
	public static class ImageParser extends InputStreamParser {
		@Override /* Parser */
		@SuppressWarnings("unchecked")
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)ImageIO.read(session.getInputStream());
		}
	}
}
