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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Thrown</l> HTTP response header.
 *
 * <p>
 * Contains exception information including name and optionally a message.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found
 * </p>
 *
 * <p>
 * This header isn't part of the RFC2616 specification, but is provided to allow for Java exception information
 * to be delivered to remote REST Java interfaces.
 *
 * <p>
 * This header supports comma-delimited values for multiple thrown values.
 * <p class='bcode'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found,java.lang.RuntimeException;foo
 * </p>
 *
 * <p>
 * Note that this is equivalent to specifying multiple header values.
 * <p class='bcode'>
 * 	Thrown: org.apache.juneau.http.response.NotFound;Resource was not found
 * 	Thrown: java.lang.RuntimeException;foo
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Thrown")
public class Thrown extends BasicCsvHeader {

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
		return new Thrown(alist(values).stream().map(Part::new).collect(Collectors.toList()));
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
		List<Part> l = list();
		split(value, x -> l.add(new Part(x)));
		this.value = value == null ? null : unmodifiable(l);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 */
	public Thrown(List<Part> value) {
		super(NAME, join(value, ", "));
		this.value = unmodifiable(value);
	}

	/**
	 * Returns the class name portion of the header.
	 *
	 * @return The class name portion of the header, or <jk>null</jk> if not there.
	 */
	public Optional<List<Part>> asParts() {
		return optional(value);
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
