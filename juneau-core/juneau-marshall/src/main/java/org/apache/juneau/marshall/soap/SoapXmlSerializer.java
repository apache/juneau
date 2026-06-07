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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.xml.*;

/**
 * Serializes POJOs to HTTP responses as XML+SOAP.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/xml+soap</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/xml+soap</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially the same output as {@link XmlDocSerializer}, except wrapped in a standard SOAP envelope.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class SoapXmlSerializer extends XmlSerializer implements SoapXmlMetaProvider {

	// Property name constants
	private static final String PROP_soapAction = "soapAction";

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends XmlSerializer.Builder<Builder> {

		private static final Cache<HashKey,SoapXmlSerializer> CACHE = Cache.of(HashKey.class, SoapXmlSerializer.class).build();

		private String soapAction;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/xml");
			accept("text/xml+soap");
			soapAction = "http://www.w3.org/2003/05/soap-envelope";
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			soapAction = copyFrom.soapAction;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(SoapXmlSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			soapAction = copyFrom.soapAction;
		}

		@Override /* Overridden from Context.Builder<?> */
		public SoapXmlSerializer build() {
			return cache(CACHE).build(SoapXmlSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				soapAction
			);
			// @formatter:on
		}

		/**
		 * The <c>SOAPAction</c> HTTP header value to set on responses.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <js>"http://www.w3.org/2003/05/soap-envelope"</js>.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder soapAction(String value) {
			soapAction = assertArgNotNull(ARG_value, value);
			return this;
		}


	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final String soapAction;

	private final Map<ClassMeta<?>,SoapXmlClassMeta> soapXmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,SoapXmlBeanPropertyMeta> soapXmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public SoapXmlSerializer(Builder builder) {
		super(builder);
		soapAction = builder.soapAction;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public SoapXmlSerializerSession.Builder createSession() {
		return SoapXmlSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public SoapXmlSerializerSession getSession() { return createSession().build(); }

	/**
	 * The SOAPAction HTTP header value to set on responses.
	 *
	 * @see Builder#soapAction(String)
	 * @return
	 * 	The SOAPAction HTTP header value to set on responses.
	 */
	public String getSoapAction() { return soapAction; }

	@Override /* Overridden from SoapXmlMetaProvider */
	public SoapXmlBeanPropertyMeta getSoapXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return SoapXmlBeanPropertyMeta.DEFAULT;
		return soapXmlBeanPropertyMetas.computeIfAbsent(bpm, k -> new SoapXmlBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from SoapXmlMetaProvider */
	public SoapXmlClassMeta getSoapXmlClassMeta(ClassMeta<?> cm) {
		return soapXmlClassMetas.computeIfAbsent(cm, k -> new SoapXmlClassMeta(k, this));
	}

	@Override /* Overridden from XmlSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_soapAction, soapAction);
	}
}