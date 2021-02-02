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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to XML.
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
@ConfigurableContext
public class XmlSerializer extends WriterSerializer implements XmlMetaProvider, XmlCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "XmlSerializer";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_addBeanTypes XML_addBeanTypes}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>XmlSerializer.addBeanTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_ADDBEANTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#addBeanTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#addBeanTypes()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String XML_addBeanTypes = PREFIX + ".addBeanTypes.b";

	/**
	 * Configuration property:  Add namespace URLs to the root element.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_addNamespaceUrisToRoot XML_addNamespaceUrisToRoot}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.addNamespaceUrisToRoot.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>XmlSerializer.addNamespaceUrisToRoot</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_ADDNAMESPACEURISTOROOT</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#addNamespaceUrisToRoot()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#addNamespaceUrisToRoot()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root element for the default and all mapped namespaces.
	 *
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc XmlNamespaces}
	 * </ul>
	 */
	public static final String XML_addNamespaceUrisToRoot = PREFIX + ".addNamespaceUrisToRoot.b";

	/**
	 * Configuration property:  Default namespace.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_defaultNamespace XML_defaultNamespace}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.defaultNamespace.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.xml.Namespace}
	 * 	<li><b>System property:</b>  <c>XmlSerializer.defaultNamespace</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_DEFAULTNAMESPACE</c>
	 * 	<li><b>Default:</b>  <js>"juneau: http://www.apache.org/2013/Juneau"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#defaultNamespace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#defaultNamespace(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the default namespace URI for this document.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc XmlNamespaces}
	 * </ul>
	 */
	public static final String XML_defaultNamespace = PREFIX + ".defaultNamespace.s";

	/**
	 * Configuration property:  Don't auto-detect namespace usage.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_disableAutoDetectNamespaces XML_disableAutoDetectNamespaces}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.disableAutoDetectNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>XmlSerializer.disableAutoDetectNamespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_DONTAUTODETECTNAMESPACES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#disableAutoDetectNamespaces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#disableAutoDetectNamespaces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Don't detect namespace usage before serialization.
	 *
	 * <p>
	 * Used in conjunction with {@link #XML_addNamespaceUrisToRoot} to reduce the list of namespace URLs appended to the
	 * root element to only those that will be used in the resulting document.
	 *
	 * <p>
	 * If disabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Auto-detection of namespaces can be costly performance-wise.
	 * 		<br>In high-performance environments, it's recommended that namespace detection be
	 * 		disabled, and that namespaces be manually defined through the {@link #XML_namespaces} property.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc XmlNamespaces}
	 * </ul>
	 */
	public static final String XML_disableAutoDetectNamespaces = PREFIX + ".disableAutoDetectNamespaces.b";

	/**
	 * Configuration property:  Enable support for XML namespaces.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_enableNamespaces XML_enableNamespaces}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.enableNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>XmlSerializer.enableNamespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_ENABLENAMESPACES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#enableNamespaces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#enableNamespaces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc XmlNamespaces}
	 * </ul>
	 */
	public static final String XML_enableNamespaces = PREFIX + ".enableNamespaces.b";

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.xml.XmlSerializer#XML_namespaces XML_namespaces}
	 * 	<li><b>Name:</b>  <js>"XmlSerializer.namespaces.ls"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;{@link org.apache.juneau.xml.Namespace}&gt;</c>
	 * 	<li><b>System property:</b>  <c>XmlSerializer.namespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>XMLSERIALIZER_NAMESPACES</c>
	 * 	<li><b>Default:</b>  empty set
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.xml.annotation.XmlConfig#defaultNamespace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.xml.XmlSerializerBuilder#defaultNamespace(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc XmlNamespaces}
	 * </ul>
	 */
	public static final String XML_namespaces = PREFIX + ".namespaces.ls";

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer without namespaces. */
	public static final XmlSerializer DEFAULT = new XmlSerializer(PropertyStore.DEFAULT);

	/** Default serializer without namespaces, with single quotes. */
	public static final XmlSerializer DEFAULT_SQ = new Sq(PropertyStore.DEFAULT);

	/** Default serializer without namespaces, with single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_SQ_READABLE = new SqReadable(PropertyStore.DEFAULT);

	/** Default serializer, all default settings. */
	public static final XmlSerializer DEFAULT_NS = new Ns(PropertyStore.DEFAULT);

	/** Default serializer, single quotes. */
	public static final XmlSerializer DEFAULT_NS_SQ = new NsSq(PropertyStore.DEFAULT);

	/** Default serializer, single quotes, whitespace added. */
	public static final XmlSerializer DEFAULT_NS_SQ_READABLE = new NsSqReadable(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes. */
	public static class Sq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Sq(PropertyStore ps) {
			super(
				ps.builder()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.build()
				);
		}
	}

	/** Default serializer, single quotes, whitespace added. */
	public static class SqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public SqReadable(PropertyStore ps) {
			super(
				ps.builder()
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.setDefault(WSERIALIZER_useWhitespace, true)
					.build()
				);
		}
	}

	/** Default serializer without namespaces. */
	public static class Ns extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Ns(PropertyStore ps) {
			super(
				ps.builder()
					.setDefault(XML_enableNamespaces, true)
					.build(),
				"text/xml",
				"text/xml+simple"
			);
		}
	}

	/** Default serializer without namespaces, single quotes. */
	public static class NsSq extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public NsSq(PropertyStore ps) {
			super(
				ps.builder()
					.setDefault(XML_enableNamespaces, true)
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.build()
				);
		}
	}

	/** Default serializer without namespaces, single quotes, with whitespace. */
	public static class NsSqReadable extends XmlSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public NsSqReadable(PropertyStore ps) {
			super(
				ps.builder()
					.setDefault(XML_enableNamespaces, true)
					.setDefault(WSERIALIZER_quoteChar, '\'')
					.setDefault(WSERIALIZER_useWhitespace, true)
					.build()
				);
		}
	}

	@SuppressWarnings("javadoc")
	protected static final Namespace
		DEFAULT_JUNEAU_NAMESPACE = Namespace.create("juneau", "http://www.apache.org/2013/Juneau"),
		DEFAULT_XS_NAMESPACE = Namespace.create("xs", "http://www.w3.org/2001/XMLSchema");

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot,
		addBeanTypes;
	private final Namespace defaultNamespace;
	private final Namespace[] namespaces;
	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public XmlSerializer(PropertyStore ps) {
		this(ps, "text/xml", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc ExtRFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public XmlSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
		autoDetectNamespaces = ! getBooleanProperty(XML_disableAutoDetectNamespaces);
		enableNamespaces = getBooleanProperty(XML_enableNamespaces);
		addNamespaceUrlsToRoot = getBooleanProperty(XML_addNamespaceUrisToRoot);
		defaultNamespace = getInstanceProperty(XML_defaultNamespace, Namespace.class, DEFAULT_JUNEAU_NAMESPACE);
		addBeanTypes = getBooleanProperty(XML_addBeanTypes, getBooleanProperty(SERIALIZER_addBeanTypes));
		namespaces = getInstanceArrayProperty(XML_namespaces, Namespace.class);
	}

	@Override /* Context */
	public XmlSerializerBuilder builder() {
		return new XmlSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link XmlSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> XmlSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link XmlSerializerBuilder} object.
	 */
	public static XmlSerializerBuilder create() {
		return new XmlSerializerBuilder();
	}

	@Override /* Serializer */
	public XmlSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
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
	 * @see #XML_addBeanTypes
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
	 * @see #XML_addNamespaceUrisToRoot
	 * @return
	 * 	<jk>true</jk> if {@code xmlns:x} attributes are added to the root element for the default and all mapped namespaces.
	 */
	protected final boolean isAddNamespaceUrlsToRoot() {
		return addNamespaceUrlsToRoot;
	}

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see #XML_disableAutoDetectNamespaces
	 * @return
	 * 	<jk>true</jk> if namespace usage is detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Default namespace.
	 *
	 * @see #XML_defaultNamespace
	 * @return
	 * 	The default namespace URI for this document.
	 */
	protected final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Enable support for XML namespaces.
	 *
	 * @see #XML_enableNamespaces
	 * @return
	 * 	<jk>false</jk> if XML output will not contain any namespaces regardless of any other settings.
	 */
	protected final boolean isEnableNamespaces() {
		return enableNamespaces;
	}

	/**
	 * Default namespaces.
	 *
	 * @see #XML_namespaces
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
