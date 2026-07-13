/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.xml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.marshall.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlDocSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlSupport">XML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource", // Writer managed by SerializerPipe; caller closes
	"java:S110", // Session classes inherit many parameters from base
	"java:S115"  // PROP_/ARG_ prefix follows framework convention
})
public class XmlDocSerializerSession extends XmlSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends XmlSerializerSession.Builder<Builder> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(XmlDocSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public XmlDocSerializerSession build() {
			return new XmlDocSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(XmlDocSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected XmlDocSerializerSession(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws java.io.IOException, SerializeException {
		try (var w = getXmlWriter(out)) {
			w.append("<?xml").attr("version", "1.0").attr("encoding", "UTF-8").appendln("?>");
			w.flush();
			super.doSerialize(out, o);
		}
	}
}