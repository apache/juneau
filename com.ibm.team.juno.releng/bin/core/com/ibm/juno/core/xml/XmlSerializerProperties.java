/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;

/**
 * Configurable properties on the {@link XmlSerializer} class.
 * <p>
 * 	Use the {@link XmlSerializer#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link XmlSerializer}.
 * <ul>
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class XmlSerializerProperties implements Cloneable {

	/**
	 * Add JSON type attributes to output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <js>true</jk>, {@code type} attributes will be added to elements in the XML for number/boolean/null nodes.
	 */
	public static final String XML_addJsonTypeAttrs = "XmlSerializer.addJsonTypeAttrs";

	/**
	 * Add JSON type attributes for strings to output ({@link Boolean}, default=<jk>false</jk>).
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
	 * Enable support for XML namespaces ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If not enabled, XML output will not contain any namespaces regardless of any other settings.
	 */
	public static final String XML_enableNamespaces = "XmlSerializer.enableNamespaces";

	/**
	 * Auto-detect namespace usage ({@link Boolean}, default=<jk>true</jk>).
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
	 * Add namespace URLs to the root element ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * Use this setting to add {@code xmlns:x} attributes to the root
	 * element for the default and all mapped namespaces.
	 * <p>
	 * This setting is ignored if {@link #XML_enableNamespaces} is not enabled.
	 */
	public static final String XML_addNamespaceUrisToRoot = "XmlSerializer.addNamespaceUrisToRoot";

	/**
	 * Default namespace URI ({@link String}, default=<jk>null</jk>).
	 * <p>
	 * Specifies the default namespace URI for this document.
	 */
	public static final String XML_defaultNamespaceUri = "XmlSerializer.defaultNamespaceUri";

	/**
	 * XMLSchema namespace ({@link Namespace}, default=<code>{name:<js>'xs'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema'</js>}</code>).
	 * <p>
	 * Specifies the namespace for the <code>XMLSchema</code> namespace, used by the schema generated
	 * by the {@link XmlSchemaSerializer} class.
	 */
	public static final String XML_xsNamespace = "XmlSerializer.xsNamespace";

	/**
	 * XMLSchema-Instance namespace ({@link Namespace}, default=<code>{name:<js>'xsi'</js>,uri:<js>'http://www.w3.org/2001/XMLSchema-instance'</js>}</code>).
	 * <p>
	 * Specifies the namespace of the <code>XMLSchema-instance</code> namespace used for<code>nil=<jk>true</jk></code> attributes.
	 */
	public static final String XML_xsiNamespace = "XmlSerializer.xsiNamespace";

	/**
	 * Default namespaces (<code>Set&lt;Namespace&gt;</code>, default=empty set).
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String XML_namespaces = "XmlSerializer.namespaces";

	boolean
		addJsonTypeAttrs = false,
		addJsonStringTypeAttrs = false,
		autoDetectNamespaces = true,
		enableNamespaces = true,
		addNamespaceUrlsToRoot = true;

	String defaultNamespace = "{juno:'http://www.ibm.com/2013/Juno'}";

	Namespace
		xsiNamespace = NamespaceFactory.get("xsi", "http://www.w3.org/2001/XMLSchema-instance"),
		xsNamespace = NamespaceFactory.get("xs", "http://www.w3.org/2001/XMLSchema");

	Namespace[] namespaces = new Namespace[0];

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 * @throws LockedException If the bean context is locked.
	 */
	public boolean setProperty(String property, Object value) throws LockedException {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(XML_addJsonTypeAttrs))
			addJsonTypeAttrs = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_addJsonStringTypeAttrs))
			addJsonStringTypeAttrs = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_enableNamespaces))
			enableNamespaces = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_autoDetectNamespaces))
			autoDetectNamespaces = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_addNamespaceUrisToRoot))
			addNamespaceUrlsToRoot = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_defaultNamespaceUri))
			defaultNamespace = (String)value;
		else if (property.equals(XML_xsiNamespace))
			xsiNamespace = NamespaceFactory.parseNamespace(value);
		else if (property.equals(XML_xsNamespace))
			xsNamespace = NamespaceFactory.parseNamespace(value);
		else if (property.equals(XML_namespaces))
			namespaces = NamespaceFactory.parseNamespaces(value);
		else
			return false;
		return true;
	}

	@Override /* Cloneable */
	public XmlSerializerProperties clone() {
		try {
			return (XmlSerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}