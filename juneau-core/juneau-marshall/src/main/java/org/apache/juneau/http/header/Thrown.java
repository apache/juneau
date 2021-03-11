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
package org.apache.juneau.http.header;

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Collections.*;

import java.util.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Thrown</l> HTTP request header.
 *
 * <p>
 * Contains exception information including name and optionally a message.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found
 * </p>
 *
 * <p>
 * This header isn't part of the RFC2616 specification, but is provided to allow for Java exception information
 * to be delivered to remote REST Java interfaces.
 * 
 * <p>
 * This header supports comma-delimited values for multiple thrown values.
 * <p class='bcode w800'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found,java.lang.RuntimeException;foo
 * </p>
 * 
 * <p>
 * Note that this is equivalent to specifying multiple header values.
 * <p class='bcode w800'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found
 * 	Thrown: java.lang.RuntimeException;foo
 * </p>
 */
@Header("Thrown")
public class Thrown extends BasicCsvArrayHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * An empty unmodifiable Thrown header.
	 */
	public static final Thrown EMPTY = new Thrown((String)null);

	private final List<Part> parts;

	/**
	 * Convenience creator.
	 *
	 * @param values
	 * 	The header value.
	 * @return A new {@link Exception} object.
	 */
	public static Thrown of(Throwable...values) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				sb.append(", ");
			Throwable t = values[i];
			sb.append(urlEncode(t.getClass().getName())).append(";").append(urlEncode(t.getMessage()));
		}
		return new Thrown(sb.toString());
	}

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * @return A new {@link Exception} object.
	 */
	public static Thrown of(String value) {
		if (value == null)
			return null;
		return new Thrown(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(String value) {
		super("Thrown", value);
		List<Part> l = new ArrayList<>();
		for (String s : asList().orElse(emptyList())) {
			int i = value.indexOf(';');
			if (i != -1) {
				l.add(new Part(urlDecode(s.substring(0, i).trim()), urlDecode(s.substring(i+1).trim())));
			} else {
				l.add(new Part(urlDecode(s), null));
			}
		}
		parts = unmodifiableList(l);
	}

	/**
	 * Returns the class name portion of the header.
	 *
	 * @return The class name portion of the header, or <jk>null</jk> if not there.
	 */
	public List<Part> getParts() {
		return parts;
	}

	/**
	 * Represents a single entry in this header.
	 */
	public static class Part {

		String className, message;

		Part(String className, String message) {
			this.className = className;
			this.message = message;
		}

		/**
		 * Returns the message portion of the header.
		 *
		 * @return The message portion of the header, or <jk>null</jk> if not there.
		 */
		public String getClassName() {
			return className;
		}

		/**
		 * Returns the message portion of the header.
		 *
		 * @return The message portion of the header, or <jk>null</jk> if not there.
		 */
		public String getMessage() {
			return message;
		}
	}
}
