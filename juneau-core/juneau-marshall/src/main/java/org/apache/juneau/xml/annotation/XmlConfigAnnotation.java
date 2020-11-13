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
package org.apache.juneau.xml.annotation;

import static org.apache.juneau.xml.XmlParser.*;
import static org.apache.juneau.xml.XmlSerializer.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Utility classes and methods for the {@link XmlConfig @XmlConfig} annotation.
 */
public class XmlConfigAnnotation {

	/**
	 * Applies {@link XmlConfig} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<XmlConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<XmlConfig> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<XmlConfig> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			XmlConfig a = ai.getAnnotation();
			if (! a.addBeanTypes().isEmpty())
				psb.set(XML_addBeanTypes, bool(a.addBeanTypes()));
			if (! a.addNamespaceUrisToRoot().isEmpty())
				psb.set(XML_addNamespaceUrisToRoot, bool(a.addNamespaceUrisToRoot()));
			if (! a.disableAutoDetectNamespaces().isEmpty())
				psb.set(XML_disableAutoDetectNamespaces, bool(a.disableAutoDetectNamespaces()));
			if (! a.defaultNamespace().isEmpty())
				psb.set(XML_defaultNamespace, string(a.defaultNamespace()));
			if (! a.enableNamespaces().isEmpty())
				psb.set(XML_enableNamespaces, bool(a.enableNamespaces()));
			if (a.namespaces().length > 0)
				psb.set(XML_namespaces, Namespace.createArray(strings(a.namespaces())));

			if (a.eventAllocator() != XmlEventAllocator.Null.class)
				psb.set(XML_eventAllocator, a.eventAllocator());
			if (! a.preserveRootElement().isEmpty())
				psb.set(XML_preserveRootElement, bool(a.preserveRootElement()));
			if (a.reporter() != XmlReporter.Null.class)
				psb.set(XML_reporter, a.reporter());
			if (a.resolver() != XmlResolver.Null.class)
				psb.set(XML_resolver, a.resolver());
			if (! a.validating().isEmpty())
				psb.set(XML_validating, bool(a.validating()));
		}
	}
}