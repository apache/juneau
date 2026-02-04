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
package org.apache.juneau.xml.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.xml.*;

/**
 * Utility classes and methods for the {@link XmlConfig @XmlConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>
 * </ul>
 */
public class XmlConfigAnnotation {

	/**
	 * Applies {@link XmlConfig} annotations to a {@link org.apache.juneau.xml.XmlParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<XmlConfig,XmlParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(XmlConfig.class, XmlParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<XmlConfig> ai, XmlParser.Builder b) {
			XmlConfig a = ai.inner();

			type(a.eventAllocator()).ifPresent(b::eventAllocator);
			bool(a.preserveRootElement()).ifPresent(b::preserveRootElement);
			type(a.reporter()).ifPresent(b::reporter);
			type(a.resolver()).ifPresent(b::resolver);
			bool(a.validating()).ifPresent(b::validating);
		}
	}

	/**
	 * Applies {@link XmlConfig} annotations to a {@link org.apache.juneau.xml.XmlSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<XmlConfig,XmlSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(XmlConfig.class, XmlSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<XmlConfig> ai, XmlSerializer.Builder b) {
			XmlConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypesXml);
			bool(a.addNamespaceUrisToRoot()).ifPresent(b::addNamespaceUrisToRoot);
			bool(a.disableAutoDetectNamespaces()).ifPresent(b::disableAutoDetectNamespaces);
			string(a.defaultNamespace()).map(Namespace::create).ifPresent(b::defaultNamespace);
			bool(a.enableNamespaces()).ifPresent(b::enableNamespaces);
			strings(a.namespaces()).map(Namespace::createArray).ifPresent(b::namespaces);
		}
	}
}