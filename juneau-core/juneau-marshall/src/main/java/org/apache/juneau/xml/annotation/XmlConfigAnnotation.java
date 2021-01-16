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

			psb.setIfNotEmpty(XML_addBeanTypes, bool(a.addBeanTypes()));
			psb.setIfNotEmpty(XML_addNamespaceUrisToRoot, bool(a.addNamespaceUrisToRoot()));
			psb.setIfNotEmpty(XML_disableAutoDetectNamespaces, bool(a.disableAutoDetectNamespaces()));
			psb.setIfNotEmpty(XML_defaultNamespace, string(a.defaultNamespace()));
			psb.setIfNotEmpty(XML_enableNamespaces, bool(a.enableNamespaces()));
			psb.setIf(a.namespaces().length > 0, XML_namespaces, Namespace.createArray(stringList(a.namespaces())));
			psb.setIf(a.eventAllocator() != XmlEventAllocator.Null.class, XML_eventAllocator, a.eventAllocator());
			psb.setIfNotEmpty(XML_preserveRootElement, bool(a.preserveRootElement()));
			psb.setIf(a.reporter() != XmlReporter.Null.class, XML_reporter, a.reporter());
			psb.setIf(a.resolver() != XmlResolver.Null.class, XML_resolver, a.resolver());
			psb.setIfNotEmpty(XML_validating, bool(a.validating()));
		}
	}
}