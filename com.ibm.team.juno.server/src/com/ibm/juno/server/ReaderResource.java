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
 * Represents the contents of a text file with convenience methods for resolving
 * 	{@link StringVar} variables and adding HTTP response headers.
 * <p>
 * This class is handled special by the {@link WritableHandler} class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ReaderResource implements Writable {

	private String contents;
	private String mediaType;
	private StringVarResolver vr;
	private Map<String,String> headers = new LinkedHashMap<String,String>();

	/**
	 * Constructor.
	 *
	 * @param contents The contents of this resource.
	 * @param mediaType The HTTP media type.
	 */
	protected ReaderResource(String contents, String mediaType) {
		this.contents = contents;
		this.mediaType = mediaType;
	}

	/**
	 * Add an HTTP response header.
	 *
	 * @param name The header name.
	 * @param value The header value converted to a string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public ReaderResource setHeader(String name, Object value) {
		headers.put(name, value == null ? "" : value.toString());
		return this;
	}

	/**
	 * Use the specified {@link StringVarResolver} to resolve any {@link StringVar StringVars} in the
	 * contents of this file when the {@link #writeTo(Writer)} or {@link #toString()} methods are called.
	 *
	 * @param vr The string variable resolver to use to resolve string variables.
	 * @return This object (for method chaining).
	 */
	public ReaderResource setVarResolver(StringVarResolver vr) {
		this.vr = vr;
		return this;
	}

	/**
	 * Get the HTTP response headers.
	 *
	 * @return The HTTP response headers.
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	@Override /* Writeable */
	public void writeTo(Writer w) throws IOException {
		if (vr != null)
			vr.writeTo(contents, w);
		else
			w.write(contents);
	}

	@Override /* Streamable */
	public String getMediaType() {
		return mediaType;
	}

	@Override /* Object */
	public String toString() {
		if (vr != null)
			return vr.resolve(contents);
		return contents;
	}
}
