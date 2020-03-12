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

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on classes specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the
 * class.
 */
public class XmlClassMeta extends ExtendedClassMeta {

	private final Namespace namespace;
	private final List<Xml> xmls;
	private final XmlFormat format;
	private final String childName;

	/**
	 * Constructor.
	 *
	 * @param cm The class that this annotation is defined on.
	 * @param mp XML metadata provider (for finding information about other artifacts).
	 */
	public XmlClassMeta(ClassMeta<?> cm, XmlMetaProvider mp) {
		super(cm);
		this.namespace = findNamespace(cm, mp);
		this.xmls = cm.getAnnotations(Xml.class);

		String _childName = null;
		XmlFormat _format = XmlFormat.DEFAULT;
		for (Xml a : xmls) {
			if (a.format() != XmlFormat.DEFAULT)
				_format = a.format();
			if (! a.childName().isEmpty())
				_childName = a.childName();
		}
		this.format = _format;
		this.childName = _childName;
	}

	/**
	 * Returns the {@link Xml @Xml} annotations defined on the class.
	 *
	 * @return An unmodifiable list of annotations ordered parent-to-child, or an empty list if not found.
	 */
	protected List<Xml> getAnnotations() {
		return xmls;
	}

	/**
	 * Returns the {@link Xml#format() @Xml(format)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or {@link XmlFormat#DEFAULT} if not specified.
	 */
	protected XmlFormat getFormat() {
		return format;
	}

	/**
	 * Returns the {@link Xml#childName() @Xml(childName)} annotation defined on the class.
	 *
	 * @return The value of the annotation, or <jk>null</jk> if not specified.
	 */
	protected String getChildName() {
		return childName;
	}

	/**
	 * Returns the XML namespace associated with this class.
	 *
	 * <p>
	 * Namespace is determined in the following order of {@link Xml#prefix() @Xml(prefix)} annotation:
	 * <ol>
	 * 	<li>Class.
	 * 	<li>Package.
	 * 	<li>Superclasses.
	 * 	<li>Superclass packages.
	 * 	<li>Interfaces.
	 * 	<li>Interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this class, or <jk>null</jk> if no namespace is associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	private static Namespace findNamespace(ClassMeta<?> cm, MetaProvider mp) {
		if (cm == null)
			return null;
		List<Xml> xmls = cm.getAnnotations(Xml.class);
		List<XmlSchema> schemas = cm.getAnnotations(XmlSchema.class);
		return XmlUtils.findNamespace(xmls, schemas);
	}
}
