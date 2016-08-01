/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static javax.servlet.http.HttpServletResponse.*;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.net.URI;
import java.util.*;

import javax.imageio.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 */
@RestResource(
	path="/photos",
	messages="nls/PhotosResource",
	properties={
		@Property(name=HTMLDOC_title, value="Photo REST service"),
		@Property(name=HTMLDOC_description, value="Use a tool like Poster to upload and retrieve jpeg and png images."),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.PhotosResource)'}"),
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
			InputStream is = getClass().getResourceAsStream("averycutedog.jpg");
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
	public BufferedImage getPhoto(@Attr int id) throws Exception {
		Photo p = photos.get(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return p.image;
	}

	/** PUT request handler */
	@RestMethod(name="PUT", path="/{id}", parsers=ImageParser.class)
	public String addPhoto(@Attr int id, @Content BufferedImage image) throws Exception {
		photos.put(id, new Photo(id, image));
		return "OK";
	}

	/** POST request handler */
	@RestMethod(name="POST", path="/", parsers=ImageParser.class)
	public Photo setPhoto(@Content BufferedImage image) throws Exception {
		int id = photos.size();
		Photo p = new Photo(id, image);
		photos.put(id, p);
		return p;
	}

	/** DELETE request handler */
	@RestMethod(name="DELETE", path="/{id}")
	public String deletePhoto(@Attr int id) throws Exception {
		Photo p = photos.remove(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Photo not found");
		return "OK";
	}

	/** Serializer for converting images to byte streams */
	@Produces({"image/png","image/jpeg"})
	public static class ImageSerializer extends OutputStreamSerializer {
		@Override /* Serializer */
		protected void doSerialize(Object o, OutputStream out, SerializerContext ctx) throws IOException, SerializeException {
			RenderedImage image = (RenderedImage)o;
			String mediaType = ctx.getProperties().getString("mediaType");
			ImageIO.write(image, mediaType.substring(mediaType.indexOf('/')+1), out);
		}
	}

	/** Parser for converting byte streams to images */
	@Consumes({"image/png","image/jpeg"})
	public static class ImageParser extends InputStreamParser {
		@Override /* Parser */
		@SuppressWarnings("unchecked")
		protected <T> T doParse(InputStream in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
			BufferedImage image = ImageIO.read(in);
			return (T)image;
		}
	}
}
