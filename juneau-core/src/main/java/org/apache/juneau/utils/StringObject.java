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
package org.apache.juneau.utils;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A serializer/object pair used for delayed object serialization.
 *
 * <p>
 * Useful in certain conditions such as logging when you don't want to needlessly serialize objects.
 *
 * <p>
 * Instances of this method are created by the {@link WriterSerializer#toStringObject(Object)} method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// The POJO will not be serialized unless DEBUG is enabled.</jc>
 * 	logger.log(<jsf>DEBUG</jsf>, <js>"Object contents are: {0}"</js>, JsonSerializer.<jsf>DEFAULT</jsf>.toObjectString(myPojo));
 * </p>
 */
public class StringObject implements CharSequence, Writable {

	private final WriterSerializer s;
	private final Object o;
	private String results;

	/**
	 * Constructor.
	 *
	 * @param s The serializer to use to serialize the object.
	 * @param o The object to be serialized.
	 */
	public StringObject(WriterSerializer s, Object o) {
		this.s = s;
		this.o = o;
	}

	/**
	 * Constructor with default serializer {@link JsonSerializer#DEFAULT_LAX}
	 *
	 * @param o The object to be serialized.
	 */
	public StringObject(Object o) {
		this(JsonSerializer.DEFAULT_LAX, o);
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
	public MediaType getMediaType() {
		return s.getMediaTypes()[0];
	}
}
