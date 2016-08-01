/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import java.io.*;

import com.ibm.juno.core.*;

/**
 * A serializer/object pair used for delayed object serialization.
 * <p>
 * Useful in certain conditions such as logging when you don't want to needlessly serialize objects.
 * <p>
 * Instances of this method are created by the {@link WriterSerializer#toStringObject(Object)} method.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<jc>// The POJO will not be serialized unless DEBUG is enabled.</jc>
 * 	logger.log(<jsf>DEBUG</jsf>, <js>"Object contents are: {0}"</js>, JsonSerializer.<jsf>DEFAULT</jsf>.toObjectString(myPojo));
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class StringObject implements CharSequence, Writable {

	private final WriterSerializer s;
	private final Object o;
	private String results;

	/**
	 * Constructor.
	 * @param s The serializer to use to serialize the object.
	 * @param o The object to be serialized.
	 */
	public StringObject(WriterSerializer s, Object o) {
		this.s = s;
		this.o = o;
	}

	@Override /* Object */
	public String toString() {
		if (results == null)
			results = s.toString(o);
		return results;
	}

	@Override /* CharSequence */
	public int length() {
		return toString().length();
	}

	@Override /* CharSequence */
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override /* CharSequence */
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override /* Writable */
	public void writeTo(Writer w) throws IOException {
		try {
			s.serialize(o, w);
		} catch (SerializeException e) {
			throw new IOException(e);
		}
	}

	@Override /* Writable */
	public String getMediaType() {
		return s.getMediaTypes()[0];
	}
}
