/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.response.*;

/**
 * Represents the contents of a byte stream file with convenience methods for adding HTTP response headers.
 * <p>
 * This class is handled special by the {@link StreamableHandler} class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class StreamResource implements Streamable {

	private byte[] contents;
	private String mediaType;
	private Map<String,String> headers = new LinkedHashMap<String,String>();

	/**
	 * Constructor.
	 * Create a stream resource from a byte array.
	 *
	 * @param contents The resource contents.
	 * @param mediaType The resource media type.
	 */
	public StreamResource(byte[] contents, String mediaType) {
		this.contents = contents;
		this.mediaType = mediaType;
	}

	/**
	 * Constructor.
	 * Create a stream resource from an <code>InputStream</code>.
	 * Contents of stream will be loaded into a reusable byte array.
	 *
	 * @param contents The resource contents.
	 * @param mediaType The resource media type.
	 * @throws IOException
	 */
	public StreamResource(InputStream contents, String mediaType) throws IOException {
		this.contents = IOUtils.readBytes(contents, 1024);
		this.mediaType = mediaType;
	}

	/**
	 * Add an HTTP response header.
	 *
	 * @param name The header name.
	 * @param value The header value, converted to a string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public StreamResource setHeader(String name, Object value) {
		headers.put(name, value == null ? "" : value.toString());
		return this;
	}

	/**
	 * Get the HTTP response headers.
	 *
	 * @return The HTTP response headers.  Never <jk>null</jk>.
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	@Override /* Streamable */
	public void streamTo(OutputStream os) throws IOException {
		os.write(contents);
	}

	@Override /* Streamable */
	public String getMediaType() {
		return mediaType;
	}
}
