/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import java.io.*;

import org.apache.http.entity.*;
import org.apache.http.message.*;

import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * HttpEntity for serializing POJOs as the body of HTTP requests.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RestRequestEntity extends BasicHttpEntity {
	final Object output;
	final Serializer<?> serializer;
	byte[] outputBytes;

	/**
	 * Constructor.
	 * @param input The POJO to serialize.  Can also be a {@link Reader} or {@link InputStream}.
	 * @param serializer The serializer to use to serialize this response.
	 */
	public RestRequestEntity(Object input, Serializer<?> serializer) {
		this.output = input;
		this.serializer = serializer;
		if (serializer != null)
			setContentType(new BasicHeader("Content-Type", serializer.getResponseContentType()));
	}

	@Override /* BasicHttpEntity */
	public void writeTo(OutputStream os) throws IOException {
		if (output instanceof InputStream) {
			IOPipe.create(output, os).closeOut().run();
		} else if (output instanceof Reader) {
			IOPipe.create(output, new OutputStreamWriter(os, IOUtils.UTF8)).closeOut().run();
		} else {
			try {
				if (serializer == null) {
					// If no serializer specified, just close the stream.
					os.close();
				} else if (! serializer.isWriterSerializer()) {
					OutputStreamSerializer s2 = (OutputStreamSerializer)serializer;
					s2.serialize(output, os);
					os.close();
				} else {
					Writer w = new OutputStreamWriter(os, IOUtils.UTF8);
					WriterSerializer s2 = (WriterSerializer)serializer;
					s2.serialize(output, w);
					w.close();
				}
			} catch (SerializeException e) {
				throw new com.ibm.juno.client.RestCallException(e);
			}
		}
	}

	@Override /* BasicHttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* BasicHttpEntity */
	public InputStream getContent() {
		if (outputBytes == null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				writeTo(baos);
				outputBytes = baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new ByteArrayInputStream(outputBytes);
	}
}
