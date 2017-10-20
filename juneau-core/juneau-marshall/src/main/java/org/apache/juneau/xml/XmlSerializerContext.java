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

import static org.apache.juneau.xml.XmlSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link XmlSerializer} class.
 */
public class XmlSerializerContext extends SerializerContext {

	final boolean
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot,
		addBeanTypeProperties;

	final String defaultNamespace;

	final Namespace
		xsNamespace;

	final Namespace[] namespaces;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public XmlSerializerContext(PropertyStore ps) {
		super(ps);
		autoDetectNamespaces = ps.getProperty(XML_autoDetectNamespaces, boolean.class, true);
		enableNamespaces = ps.getProperty(XML_enableNamespaces, boolean.class, false);
		addNamespaceUrlsToRoot = ps.getProperty(XML_addNamespaceUrisToRoot, boolean.class, false);
		defaultNamespace = ps.getProperty(XML_defaultNamespace, String.class, "{juneau:'http://www.apache.org/2013/Juneau'}");
		xsNamespace = ps.getProperty(XML_xsNamespace, Namespace.class, new Namespace("xs", "http://www.w3.org/2001/XMLSchema"));
		namespaces = ps.getProperty(XML_namespaces, Namespace[].class, new Namespace[0]);
		addBeanTypeProperties = ps.getProperty(XML_addBeanTypeProperties, boolean.class, ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("XmlSerializerContext", new ObjectMap()
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