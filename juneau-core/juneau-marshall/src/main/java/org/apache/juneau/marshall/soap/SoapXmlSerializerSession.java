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
package org.apache.juneau.marshall.soap;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link SoapXmlSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class SoapXmlSerializerSession extends XmlSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends XmlSerializerSession.Builder<Builder> {

		private SoapXmlSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(SoapXmlSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public SoapXmlSerializerSession build() {
			return new SoapXmlSerializerSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(SoapXmlSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final SoapXmlSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected SoapXmlSerializerSession(Builder builder) {
		super(builder);

		ctx = builder.ctx;
	}

	@Override /* Overridden from Serializer */
	public Map<String,String> getResponseHeaders() { return map("SOAPAction", getSoapAction()); }

	/**
	 * The SOAPAction HTTP header value to set on responses.
	 *
	 * @see SoapXmlSerializer.Builder#soapAction(String)
	 * @return
	 * 	The SOAPAction HTTP header value to set on responses.
	 */
	public String getSoapAction() { return ctx.getSoapAction(); }

	@Override /* Overridden from SerializerSession */
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		try (XmlWriter w = getXmlWriter(out)) {
			// @formatter:off
			w.append("<?xml")
				.attr("version", "1.0")
				.attr("encoding", "UTF-8")
				.appendln("?>");
			w.oTag("soap", "Envelope")
				.attr("xmlns", "soap", getSoapAction())
				.appendln(">");
			w.sTag(1, "soap", "Body").nl(1);
			indent += 2;
			w.flush();
			super.doWrite(out, o);
			w.ie(1).eTag("soap", "Body").nl(1);
			w.eTag("soap", "Envelope").nl(0);
			// @formatter:on
		}
	}
}