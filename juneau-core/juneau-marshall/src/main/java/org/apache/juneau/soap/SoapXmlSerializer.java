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
package org.apache.juneau.soap;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

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
 */
@ConfigurableContext
public final class SoapXmlSerializer extends XmlSerializer implements SoapXmlMetaProvider,SoapXmlCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "SoapXmlSerializer";

	/**
	 * Configuration property:  The <c>SOAPAction</c> HTTP header value to set on responses.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.soap.SoapXmlSerializer#SOAPXML_SOAPAction SOAPXML_SOAPAction}
	 * 	<li><b>Name:</b>  <js>"SoapXmlSerializer.SOAPAction.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>SoapXmlSerializer.SOAPAction</c>
	 * 	<li><b>Environment variable:</b>  <c>SOAPXMLSERIALIZER_SOAPACTION</c>
	 * 	<li><b>Default:</b>  <js>"http://www.w3.org/2003/05/soap-envelope"</js>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.soap.annotation.SoapXmlConfig#soapAction()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.soap.SoapXmlSerializerBuilder#soapAction(String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String SOAPXML_SOAPAction = PREFIX + ".SOAPAction.s";


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String soapAction;
	private final Map<ClassMeta<?>,SoapXmlClassMeta> soapXmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,SoapXmlBeanPropertyMeta> soapXmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public SoapXmlSerializer(PropertyStore ps) {
		super(ps, "text/xml", "text/xml+soap");
		soapAction = ps.getString(SOAPXML_SOAPAction, "http://www.w3.org/2003/05/soap-envelope");
	}

	@Override /* Context */
	public SoapXmlSerializerBuilder builder() {
		return new SoapXmlSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link SoapXmlSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> SoapXmlSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link SoapXmlSerializerBuilder} object.
	 */
	public static SoapXmlSerializerBuilder create() {
		return new SoapXmlSerializerBuilder();
	}

	@Override /* Serializer */
	public SoapXmlSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public SoapXmlSerializerSession createSession(SerializerSessionArgs args) {
		return new SoapXmlSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SoapXmlMetaProvider */
	public SoapXmlClassMeta getSoapXmlClassMeta(ClassMeta<?> cm) {
		SoapXmlClassMeta m = soapXmlClassMetas.get(cm);
		if (m == null) {
			m = new SoapXmlClassMeta(cm, this);
			soapXmlClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* SoapXmlMetaProvider */
	public SoapXmlBeanPropertyMeta getSoapXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return SoapXmlBeanPropertyMeta.DEFAULT;
		SoapXmlBeanPropertyMeta m = soapXmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new SoapXmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			soapXmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The SOAPAction HTTP header value to set on responses.
	 *
	 * @see #SOAPXML_SOAPAction
	 * @return
	 * 	The SOAPAction HTTP header value to set on responses.
	 */
	public String getSoapAction() {
		return soapAction;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"SoapXmlSerializer",
				OMap
					.create()
					.filtered()
					.a("soapAction", soapAction)
			);
	}
}
