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
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link XmlSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link XmlSerializer#setProperty(String,Object)}
 * 	<li>{@link XmlSerializer#setProperties(ObjectMap)}
 * 	<li>{@link XmlSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link XmlSerializer#addBeanFilters(Class[])}
 * 	<li>{@link XmlSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link XmlSerializer#addToDictionary(Class[])}
 * 	<li>{@link XmlSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the XML serializer</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #XML_addJsonTypeAttrs}</td>
 * 		<td>Add JSON type attributes to output.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_addJsonStringTypeAttrs}</td>
 * 		<td>Add JSON type attributes for strings to output.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_enableNamespaces}</td>
 * 		<td>Enable support for XML namespaces.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_autoDetectNamespaces}</td>
 * 		<td>Auto-detect namespace usage.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_addNamespaceUrisToRoot}</td>
 * 		<td>Add namespace URLs to the root element.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_defaultNamespaceUri}</td>
 * 		<td>Default namespace URI.</td>
 * 		<td><code>String</code></td>
 * 		<td><jk>null</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_xsNamespace}</td>
 * 		<td>XMLSchema namespace.</td>
 * 		<td>{@link Namespace}</td>
 * 		<td><code>{name:<js>'xs'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema'</js>}</code></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_xsiNamespace}</td>
 * 		<td>XMLSchema-Instance namespace.</td>
 * 		<td>{@link Namespace}</td>
 * 		<td><code>{name:<js>'xsi'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema-instance'</js>}</code></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #XML_namespaces}</td>
 * 		<td>Default namespaces.</td>
 * 		<td><code>Set&lt;{@link Namespace}&gt;</code></td>
 * 		<td>empty set</td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
*
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class XmlSerializerContext extends SerializerContext {

	/**
	 * <b>Configuration property:</b>  Add JSON type attributes to output.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addJsonTypeAttrs"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <js>true</jk>, {@code type} attributes will be added to elements in the XML for number/boolean/null nodes.
	 */
	public static final String XML_addJsonTypeAttrs = "XmlSerializer.addJsonTypeAttrs";

	/**
	 * <b>Configuration property:</b>  Add JSON type attributes for strings to output.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addJsonStringTypeAttrs"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, {@code type} attributes will be added to elements in the XML for string nodes.
	 * <p>
	 * By default, these attributes are not added, and the parser will assume that the content type
	 * of the node is string by default.
	 * <p>
	 * This feature is disabled if {@link #XML_addJsonTypeAttrs} is disabled.
	 */
	public static final String XML_addJsonStringTypeAttrs = "XmlSerializer.addJsonStringTypeAttrs";

	/**
	 * <b>Configuration property:</b>  Enable support for XML namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.enableNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 */
	public static final String XML_enableNamespaces = "XmlSerializer.enableNamespaces";

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * Used in conjunction with {@link #XML_addNamespaceUrisToRoot} to reduce
	 * the list of namespace URLs appended to the root element to only those
	 * that will be used in the resulting document.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 * <p>
	 * <b>IMPORTANT NOTE:</b>
	 * Auto-detection of namespaces can be costly performance-wise.
	 * In high-performance environments, it's recommended that namespace detection be
	 * 	disabled, and that namespaces be manually defined through the {@link #XML_namespaces} property.
	 */
	public static final String XML_autoDetectNamespaces = "XmlSerializer.autoDetectNamespaces";

	/**
	 * <b>Configuration property:</b>  Add namespace URLs to the root element.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.addNamespaceUrisToRoot"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root
	 * element for the default and all mapped namespaces.
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 */
	public static final String XML_addNamespaceUrisToRoot = "XmlSerializer.addNamespaceUrisToRoot";

	/**
	 * <b>Configuration property:</b>  Default namespace URI.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.defaultNamespaceUri"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * </ul>
	 * <p>
	 * Specifies the default namespace URI for this document.
	 */
	public static final String XML_defaultNamespaceUri = "XmlSerializer.defaultNamespaceUri";

	/**
	 * <b>Configuration property:</b>  XMLSchema namespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.xsNamespace"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{name:<js>'xs'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema'</js>}</code>
	 * </ul>
	 * <p>
	 * Specifies the namespace for the <code>XMLSchema</code> namespace, used by the schema generated
	 * by the {@link XmlSchemaSerializer} class.
	 */
	public static final String XML_xsNamespace = "XmlSerializer.xsNamespace";

	/**
	 * <b>Configuration property:</b>  XMLSchema-Instance namespace.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.xsiNamespace"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{name:<js>'xsi'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema-instance'</js>}</code>
	 * </ul>
	 * <p>
	 * Specifies the namespace of the <code>XMLSchema-instance</code> namespace used for<code>nil=<jk>true</jk></code> attributes.
	 */
	public static final String XML_xsiNamespace = "XmlSerializer.xsiNamespace";

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"XmlSerializer.namespaces"</js>
	 * 	<li><b>Data type:</b> <code>Set&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty set
	 * </ul>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String XML_namespaces = "XmlSerializer.namespaces";


	final boolean
		addJsonTypeAttrs,
		addJsonStringTypeAttrs,
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot;

	final String defaultNamespace;

	final Namespace
		xsiNamespace,
		xsNamespace;

	final Namespace[] namespaces;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public XmlSerializerContext(ContextFactory cf) {
		super(cf);
		addJsonTypeAttrs = cf.getProperty(XML_addJsonTypeAttrs, boolean.class, false);
		addJsonStringTypeAttrs = cf.getProperty(XML_addJsonStringTypeAttrs, boolean.class, false);
		autoDetectNamespaces = cf.getProperty(XML_autoDetectNamespaces, boolean.class, true);
		enableNamespaces = cf.getProperty(XML_enableNamespaces, boolean.class, true);
		addNamespaceUrlsToRoot = cf.getProperty(XML_addNamespaceUrisToRoot, boolean.class, true);
		defaultNamespace = cf.getProperty(XML_defaultNamespaceUri, String.class, "{juneau:'http://www.ibm.com/2013/Juneau'}");
		xsNamespace = cf.getProperty(XML_xsNamespace, Namespace.class, new Namespace("xs", "http://www.w3.org/2001/XMLSchema"));
		xsiNamespace = cf.getProperty(XML_xsiNamespace, Namespace.class, new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
		namespaces = cf.getProperty(XML_namespaces, Namespace[].class, new Namespace[0]);
	}
}