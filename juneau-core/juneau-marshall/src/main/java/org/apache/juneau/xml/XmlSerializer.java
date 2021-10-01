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
package org.apache.juneau.xml;

import static java.util.Optional.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to XML.
 * {@review}
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/xml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/xml</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * See the {@link JsonSerializer} class for details on how Java models map to JSON.
 *
 * <p>
 * For example, the following JSON...
 * <p class='bcode w800'>
 * 	{
 * 		name:<js>'John Smith'</js>,
 * 		address: {
 * 			streetAddress: <js>'21 2nd Street'</js>,
 * 			city: <js>'New York'</js>,
 * 			state: <js>'NY'</js>,
 * 			postalCode: <js>10021</js>
 * 		},
 * 		phoneNumbers: [
 * 			<js>'212 555-1111'</js>,
 * 			<js>'212 555-2222'</js>
 * 		],
 * 		additionalInfo: <jk>null</jk>,
 * 		remote: <jk>false</jk>,
 * 		height: <js>62.4</js>,
 * 		<js>'fico score'</js>:  <js>' &gt; 640'</js>
 * 	}
 * <p>
 * 	...maps to the following XML using the default serializer...
 * <p class='bcode w800'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address&gt;</xt>
 * 			<xt>&lt;streetAddress&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 *
 * <p>
 * An additional "add-json-properties" mode is also provided to prevent loss of JSON data types...
 * <p class='bcode w800'>
 * 	<xt>&lt;object&gt;</xt>
 * 		<xt>&lt;name</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;</xt>
 * 		<xt>&lt;address</xt> <xa>_type</xa>=<xs>'object'</xs><xt>&gt;</xt>
 * 			<xt>&lt;streetAddress</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>21 2nd Street<xt>&lt;/streetAddress&gt;</xt>
 * 			<xt>&lt;city</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>New York<xt>&lt;/city&gt;</xt>
 * 			<xt>&lt;state</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt>NY<xt>&lt;/state&gt;</xt>
 * 			<xt>&lt;postalCode</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>10021<xt>&lt;/postalCode&gt;</xt>
 * 		<xt>&lt;/address&gt;</xt>
 * 		<xt>&lt;phoneNumbers</xt> <xa>_type</xa>=<xs>'array'</xs><xt>&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-1111<xt>&lt;/string&gt;</xt>
 * 			<xt>&lt;string&gt;</xt>212 555-2222<xt>&lt;/string&gt;</xt>
 * 		<xt>&lt;/phoneNumbers&gt;</xt>
 * 		<xt>&lt;additionalInfo</xt> <xa>_type</xa>=<xs>'null'</xs><xt>&gt;&lt;/additionalInfo&gt;</xt>
 * 		<xt>&lt;remote</xt> <xa>_type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>false<xt>&lt;/remote&gt;</xt>
 * 		<xt>&lt;height</xt> <xa>_type</xa>=<xs>'number'</xs><xt>&gt;</xt>62.4<xt>&lt;/height&gt;</xt>
 * 		<xt>&lt;fico_x0020_score</xt> <xa>_type</xa>=<xs>'string'</xs><xt>&gt;</xt> &amp;gt; 640<xt>&lt;/fico_x0020_score&gt;</xt>
 * 	<xt>&lt;/object&gt;</xt>
 * </p>
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined <jsf>DEFAULT</jsf> serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * If an attribute name contains any non-valid XML element characters, they will be escaped using standard
 * {@code _x####_} notation.
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 *
 * The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 */
public class XmlSerializer extends WriterSerializer implements XmlMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// c
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT = new XmlSerializer(create());

	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new Sq(create());

	/** Default serializer without namespaces, with single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new SqReadable(create());

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT_NS = new Ns(create());

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_NS_SQ = new NsSq(create());

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_NS_SQ_READABLE = new NsSqReadable(create());

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Sq(XmlSerializerBuilder builder) {
			super(builder.quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected SqReadable(XmlSerializerBuilder builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
	}

	/** Default serializer without namespaces. */
	public static class Ns extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected Ns(XmlSerializerBuilder builder) {
			super(builder.enableNamespaces());
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class NsSq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected NsSq(XmlSerializerBuilder builder) {
			super(builder.enableNamespaces().quoteChar('\''));
		}
	}

	/** Default serializer without namespaces, single quotes, with whitespace. */
	public static class NsSqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		protected NsSqReadable(XmlSerializerBuilder builder) {
			super(builder.enableNamespaces().quoteChar('\'').useWhitespace());
		}
	}

	@SuppressWarnings("javadoc")
	protected static final Namespace
		DEFAULT_JUNEAU_NAMESPACE = Namespace.of("juneau", "http://www.apache.org/2013/Juneau"),
		DEFAULT_XS_NAMESPACE = Namespace.of("xs", "http://www.w3.org/2001/XMLSchema");

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot,
		addBeanTypesXml;

	final Namespace defaultNamespace;
	final Namespace[] namespaces;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected XmlSerializer(XmlSerializerBuilder builder) {
		super(builder);
		autoDetectNamespaces = ! builder.disableAutoDetectNamespaces;
		enableNamespaces = builder.enableNamespaces;
		addNamespaceUrlsToRoot = builder.addNamespaceUrisToRoot;
		addBeanTypesXml = builder.addBeanTypesXml;
		defaultNamespace = ofNullable(builder.defaultNamespace).orElse(DEFAULT_JUNEAU_NAMESPACE);
		namespaces = ofNullable(builder.namespaces).map(x -> x.toArray(new Namespace[0])).orElse(new Namespace[0]);

		addBeanTypes = addBeanTypesXml || super.isAddBeanTypes();
	}

	@Override /* Context */
	public XmlSerializerBuilder copy() {
		return new XmlSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link XmlSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> XmlSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link XmlSerializerBuilder} object.
	 */
	public static XmlSerializerBuilder create() {
		return new XmlSerializerBuilder();
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession() {
		return createSession(defaultArgs());
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession(SerializerSessionArgs args) {
		return new XmlSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* XmlMetaProvider */
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		XmlClassMeta m = xmlClassMetas.get(cm);
		if (m == null) {
			m = new XmlClassMeta(cm, this);
			xmlClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		XmlBeanMeta m = xmlBeanMetas.get(bm);
		if (m == null) {
			m = new XmlBeanMeta(bm, this);
			xmlBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		XmlBeanPropertyMeta m = xmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new XmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			xmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see XmlSerializerBuilder#addBeanTypesXml()
	 * @return
	 * 	<jk>true</jk> if<js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add namespace URLs to the root element.
	 *
	 * @see XmlSerializerBuilder#addNamespaceUrisToRoot()
	 * @return
	 * 	<jk>true</jk> if {@code xmlns:x} attributes are added to the root element for the default and all mapped namespaces.
	 */
	protected final boolean isAddNamespaceUrlsToRoot() {
		return addNamespaceUrlsToRoot;
	}

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see XmlSerializerBuilder#disableAutoDetectNamespaces()
	 * @return
	 * 	<jk>true</jk> if namespace usage is detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Default namespace.
	 *
	 * @see XmlSerializerBuilder#defaultNamespace(Namespace)
	 * @return
	 * 	The default namespace URI for this document.
	 */
	protected final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Enable support for XML namespaces.
	 *
	 * @see XmlSerializerBuilder#enableNamespaces()
	 * @return
	 * 	<jk>false</jk> if XML output will not contain any namespaces regardless of any other settings.
	 */
	protected final boolean isEnableNamespaces() {
		return enableNamespaces;
	}

	/**
	 * Default namespaces.
	 *
	 * @see XmlSerializerBuilder#namespaces(Namespace...)
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	protected final Namespace[] getNamespaces() {
		return namespaces;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"XmlSerializer",
				OMap
					.create()
					.filtered()
					.a("autoDetectNamespaces", autoDetectNamespaces)
					.a("enableNamespaces", enableNamespaces)
					.a("addNamespaceUrlsToRoot", addNamespaceUrlsToRoot)
					.a("defaultNamespace", defaultNamespace)
					.a("namespaces", namespaces)
					.a("addBeanTypes", addBeanTypes)
			);
	}
}
