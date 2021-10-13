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
import static org.apache.juneau.internal.ClassUtils.*;
import static java.util.Collections.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;

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

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Thrown";

	/**
	 * An empty unmodifiable Thrown header.
	 */
	public static final Thrown EMPTY = new Thrown((String)null);

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Thrown of(String value) {
		return value == null ? null : new Thrown(value);
	}

	/**
	 * Static creator.
	 *
	 * @param values
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Thrown of(Throwable...values) {
		return new Thrown(Arrays.asList(values).stream().map(Part::new).collect(toList()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final List<Part> value;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(String value) {
		super(NAME, value);
		List<Part> l = new ArrayList<>();
		if (value != null)
			for (String s : split(value))
				l.add(new Part(s));
		this.value = value == null ? null : unmodifiableList(l);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(List<Part> value) {
		super(NAME, join(value, ", "));
		this.value = value == null ? null : unmodifiableList(value);
	}

	/**
	 * Returns the class name portion of the header.
	 *
	 * @return The class name portion of the header, or <jk>null</jk> if not there.
	 */
	public Optional<List<Part>> asParts() {
		return ofNullable(value);
	}

	/**
	 * Represents a single entry in this header.
	 */
	public static class Part {

		String className, message;
		String value;

		/**
		 * Constructor.
		 *
		 * @param value The header part value.
		 */
		public Part(String value) {
			this.value = value;
			int i = value.indexOf(';');
			this.className = urlDecode(i == -1 ? value.trim() : value.substring(0, i).trim());
			this.message = urlDecode(i == -1 ? null : value.substring(i+1).trim());
		}

		/**
		 * Constructor.
		 *
		 * @param value The throwable to create the header part value from.
		 */
		public Part(Throwable value) {
			this.className = className(value);
			this.message = value.getMessage();
			this.value = urlEncode(className) + ';' + urlEncode(value.getMessage());
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

		@Override /* Object */
		public String toString() {
			return value;
		}
	}
}
