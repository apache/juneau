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

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to XML.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/xml</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * See the {@link JsonSerializer} class for details on how Java models map to JSON.
 *
 * <p>
 * For example, the following JSON...
 * <p class='bcode'>
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
 * <p class='bcode'>
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
 * <p class='bcode'>
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
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 *
 * The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Sq} - Default serializer, single quotes.
 * 	<li>{@link SqReadable} - Default serializer, single quotes, whitespace added.
 * </ul>
 */
public class XmlSerializer extends WriterSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "XmlSerializer.";

	/**
	 * <b>Configuration property:</b>  Enable support for XML namespaces.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.enableNamespaces.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 */
	public static final String XML_enableNamespaces = PREFIX + "enableNamespaces.b";

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.autoDetectNamespaces.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Detect namespace usage before serialization.
	 *
	 * <p>
	 * Used in conjunction with {@link #XML_addNamespaceUrisToRoot} to reduce the list of namespace URLs appended to the
	 * root element to only those that will be used in the resulting document.
	 *
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 *
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Auto-detection of namespaces can be costly performance-wise.
	 * 		In high-performance environments, it's recommended that namespace detection be
	 * 		disabled, and that namespaces be manually defined through the {@link #XML_namespaces} property.
	 * </ul>
	 */
	public static final String XML_autoDetectNamespaces = PREFIX + "autoDetectNamespaces.b";

	/**
	 * <b>Configuration property:</b>  Add namespace URLs to the root element.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addNamespaceUrisToRoot.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root element for the default and all mapped namespaces.
	 *
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 */
	public static final String XML_addNamespaceUrisToRoot = PREFIX + "addNamespaceUrisToRoot.b";

	/**
	 * <b>Configuration property:</b>  Default namespace.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.defaultNamespace.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"juneau: http://www.apache.org/2013/Juneau"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Specifies the default namespace URI for this document.
	 */
	public static final String XML_defaultNamespace = PREFIX + "defaultNamespace.s";

	/**
	 * <b>Configuration property:</b>  XMLSchema namespace.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.xsNamespace.s"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{name:<js>'xs'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema'</js>}</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Specifies the namespace for the <code>XMLSchema</code> namespace, used by the schema generated by the
	 * {@link XmlSchemaSerializer} class.
	 */
	public static final String XML_xsNamespace = PREFIX + "xsNamespace.s";

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.namespaces.ls"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty set
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String XML_namespaces = PREFIX + "namespaces.ls";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String XML_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";


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
					.set(SERIALIZER_quoteChar, '\'')
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
					.set(SERIALIZER_quoteChar, '\'')
					.set(SERIALIZER_useWhitespace, true)
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
					.set(XML_enableNamespaces, true)
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
					.set(XML_enableNamespaces, true)
					.set(SERIALIZER_quoteChar, '\'')
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
					.set(XML_enableNamespaces, true)
					.set(SERIALIZER_quoteChar, '\'')
					.set(SERIALIZER_useWhitespace, true)
					.build()
				);
		}
	}

	private static final Namespace 
		DEFAULT_JUNEAU_NAMESPACE = Namespace.create("juneau", "http://www.apache.org/2013/Juneau"),
		DEFAULT_XS_NAMESPACE = Namespace.create("xs", "http://www.w3.org/2001/XMLSchema");
	
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot,
		addBeanTypeProperties;
	
	final Namespace defaultNamespace;
	
	final Namespace
		xsNamespace;
	
	final Namespace[] namespaces;

	private volatile XmlSchemaSerializer schemaSerializer;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public XmlSerializer(PropertyStore ps) {
		this(ps, "text/xml");
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
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	public XmlSerializer(PropertyStore ps, String produces, String...accept) {
		super(ps, produces, accept);
		autoDetectNamespaces = getProperty(XML_autoDetectNamespaces, boolean.class, true);
		enableNamespaces = getProperty(XML_enableNamespaces, boolean.class, false);
		addNamespaceUrlsToRoot = getProperty(XML_addNamespaceUrisToRoot, boolean.class, false);
		defaultNamespace = getInstanceProperty(XML_defaultNamespace, Namespace.class, DEFAULT_JUNEAU_NAMESPACE);
		addBeanTypeProperties = getProperty(XML_addBeanTypeProperties, boolean.class, getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
		xsNamespace = getInstanceProperty(XML_xsNamespace, Namespace.class, DEFAULT_XS_NAMESPACE);
		namespaces = getInstanceArrayProperty(XML_namespaces, Namespace.class, new Namespace[0]);
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

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public XmlSerializer getSchemaSerializer() {
		if (schemaSerializer == null)
			schemaSerializer = builder().build(XmlSchemaSerializer.class);
		return schemaSerializer;
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new XmlSerializerSession(this, args);
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("XmlSerializer", new ObjectMap()
				.append("autoDetectNamespaces", autoDetectNamespaces)
				.append("enableNamespaces", enableNamespaces)
				.append("addNamespaceUrlsToRoot", addNamespaceUrlsToRoot)
				.append("defaultNamespace", defaultNamespace)
				.append("xsNamespace", xsNamespace)
				.append("namespaces", namespaces)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
