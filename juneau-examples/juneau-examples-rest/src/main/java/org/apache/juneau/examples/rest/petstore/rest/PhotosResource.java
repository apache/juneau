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
package org.apache.juneau.examples.rest.petstore.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.jsonschema.annotation.ExternalDocs;
import org.apache.juneau.jsonschema.annotation.Schema;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map;

import javax.imageio.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.matchers.*;
import org.apache.juneau.serializer.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@RestResource(
	path="/photos",
	messages="nls/PhotosResource",
	title="Photo REST service",
	description="Sample resource that allows images to be uploaded and retrieved.",
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
@HtmlDocConfig(
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"$W{UploadPhotoMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
	},
	aside={
		"<div style='max-width:400px;min-width:200px' class='text'>",
		"	<p>Shows an example of using custom serializers and parsers to create REST interfaces over binary resources.</p>",
		"	<p>In this case, our resources are marshalled jpeg and png binary streams and are stored in an in-memory 'database' (also known as a <c>TreeMap</c>).</p>",
		"</div>"
	},
	widgets={
		UploadPhotoMenuItem.class
	},
	stylesheet="servlet:/htdocs/themes/dark.css"
)
@HtmlConfig(
	// Make the anchor text on URLs be just the path relative to the servlet.
	uriAnchorText="SERVLET_RELATIVE"
)
public class PhotosResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	// Our cache of photos
	private Map<String,Photo> photos = new TreeMap<>();

	@Override /* Servlet */
	public void init() {
		try (InputStream is = getClass().getResourceAsStream("photos/cat.jpg")) {
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

		/**
		 * @return The URI of this photo.
		 * @throws URISyntaxException ID could not be converted to a URI.
		 */
		public URI getURI() throws URISyntaxException {
			return new URI("servlet:/" + id);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// REST methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Show the list of all currently loaded photos
	 *
	 * @return The list of all currently loaded photos.
	 * @throws Exception Error occurred.
	 */
	@RestMethod(
		name=GET,
		path="/",
		summary="Show the list of all currently loaded photos"
	)
	public Collection<Photo> getAllPhotos() throws Exception {
		return photos.values();
	}

	/**
	 * Shows how to use a custom serializer to serialize a BufferedImage object to a stream.
	 *
	 * @param id The photo ID.
	 * @return The image.
	 * @throws NotFound Image was not found.
	 */
	@RestMethod(
		name=GET,
		path="/{id}",
		serializers=ImageSerializer.class,
		summary="Get a photo by ID",
		description="Shows how to use a custom serializer to serialize a BufferedImage object to a stream."
	)
	@Response(
		schema=@Schema(type="file")
	)
	public BufferedImage getPhoto(@Path("id") String id) throws NotFound {
		Photo p = photos.get(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return p.image;
	}

	/**
	 * Shows how to use a custom parser to parse a stream into a BufferedImage object.
	 *
	 * @param id The photo ID.
	 * @param image Binary contents of image.
	 * @return <js>"OK"</jk> if successful.
	 * @throws Exception Error occurred.
	 */
	@RestMethod(
		name=PUT,
		path="/{id}",
		parsers=ImageParser.class,
		summary="Add or overwrite a photo",
		description="Shows how to use a custom parser to parse a stream into a BufferedImage object."
	)
	public String addPhoto(
			@Path("id") String id,
			@Body(
				description="Binary contents of image.",
				schema=@Schema(type="file")
			)
			BufferedImage image
		) throws Exception {
		photos.put(id, new Photo(id, image));
		return "OK";
	}

	/**
	 * Shows how to use a custom parser to parse a stream into a BufferedImage object.
	 *
	 * @param image Binary contents of image.
	 * @return The Photo bean.
	 * @throws Exception Error occurred.
	 */
	@RestMethod(
		name=POST,
		path="/",
		parsers=ImageParser.class,
		summary="Add a photo",
		description="Shows how to use a custom parser to parse a stream into a BufferedImage object."
	)
	public Photo setPhoto(
			@Body(
				description="Binary contents of image.",
				schema=@Schema(type="file")
			)
			BufferedImage image
		) throws Exception {
		Photo p = new Photo(UUID.randomUUID().toString(), image);
		photos.put(p.id, p);
		return p;
	}

	/**
	 * Upload a photo from a multipart form post.
	 *
	 * @param req HTTP request.
	 * @return Redirect to servlet root.
	 * @throws Exception Error occurred.
	 */
	@RestMethod(
		name=POST,
		path="/upload",
		matchers=MultipartFormDataMatcher.class,
		summary="Upload a photo from a multipart form post",
		description="Shows how to parse a multipart form post containing a binary field.",
		swagger=@MethodSwagger(
			parameters={
				"{in:'formData', name:'id', description:'Unique identifier to assign to image.', type:'string', required:false},",
				"{in:'formData', name:'file', description:'The binary contents of the image file.', type:'file', required:true}"
			}
		)
	)
    public SeeOtherRoot uploadFile(RestRequest req) throws Exception {
       MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String)null);
       req.setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
       String id = UUID.randomUUID().toString();
       BufferedImage img = null;
       for (Part part : req.getParts()) {
    	   switch (part.getName()) {
    		   case "id":
    			   id = IOUtils.read(part.getInputStream());
    			   break;
    		   case "file":
    			   img = ImageIO.read(part.getInputStream());
    	   }
       }
       addPhoto(id, img);
       return new SeeOtherRoot(); // Redirect to the servlet root.
    }

	/**
	 * Removes a photo from the database.
	 *
	 * @param id ID of photo to remove.
	 * @return <js>"OK"</jk> if successful.
	 * @throws NotFound Photo was not found.
	 */
	@RestMethod(
		name=DELETE,
		path="/{id}",
		summary="Delete a photo by ID"
	)
	public String deletePhoto(@Path("id") String id) throws NotFound {
		Photo p = photos.remove(id);
		if (p == null)
			throw new NotFound("Photo not found");
		return "OK";
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Custom serializers and parsers.
	//-----------------------------------------------------------------------------------------------------------------

	/** Serializer for converting images to byte streams */
	public static class ImageSerializer extends OutputStreamSerializer {

		/**
		 * Constructor.
		 * @param ps The property store containing all the settings for this object.
		 */
		public ImageSerializer(PropertyStore ps) {
			super(ps, null, "image/png,image/jpeg");
		}

		@Override /* Serializer */
		public OutputStreamSerializerSession createSession(SerializerSessionArgs args) {
			return new OutputStreamSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
					RenderedImage image = (RenderedImage)o;
					String mediaType = getProperty("mediaType", String.class, (String)null);
					ImageIO.write(image, mediaType.substring(mediaType.indexOf('/')+1), out.getOutputStream());
				}
			};
		}
	}

	/** Parser for converting byte streams to images */
	public static class ImageParser extends InputStreamParser {

		/**
		 * Constructor.
		 * @param ps The property store containing all the settings for this object.
		 */
		public ImageParser(PropertyStore ps) {
			super(ps, "image/png", "image/jpeg");
		}

		@Override /* Parser */
		public InputStreamParserSession createSession(final ParserSessionArgs args) {
			return new InputStreamParserSession(args) {

				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
					return (T)ImageIO.read(pipe.getInputStream());
				}
			};
		}
	}
}