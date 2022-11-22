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
 * Metadata on bean properties specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation
 * on the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a> * </ul>
 */
public class XmlBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/**
	 * Default instance.
	 */
	public static final XmlBeanPropertyMeta DEFAULT = new XmlBeanPropertyMeta();

	private Namespace namespace = null;
	private XmlFormat xmlFormat = XmlFormat.DEFAULT;
	private String childName;
	private final XmlMetaProvider xmlMetaProvider;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp XML metadata provider (for finding information about other artifacts).
	 */
	public XmlBeanPropertyMeta(BeanPropertyMeta bpm, XmlMetaProvider mp) {
		super(bpm);
		this.xmlMetaProvider = mp;

		bpm.forEachAnnotation(Xml.class, x -> true, x -> findXmlInfo(x, mp));

		if (namespace == null)
			namespace = mp.getXmlClassMeta(bpm.getBeanMeta().getClassMeta()).getNamespace();
	}

	private XmlBeanPropertyMeta() {
		super(null);
		this.xmlMetaProvider = null;
	}

	/**
	 * Returns the XML namespace associated with this bean property.
	 *
	 * <p>
	 * Namespace is determined in the following order of {@link Xml#prefix() @Xml(prefix)} annotation:
	 * <ol>
	 * 	<li>Bean property field.
	 * 	<li>Bean getter.
	 * 	<li>Bean setter.
	 * 	<li>Bean class.
	 * 	<li>Bean package.
	 * 	<li>Bean superclasses.
	 * 	<li>Bean superclass packages.
	 * 	<li>Bean interfaces.
	 * 	<li>Bean interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this bean property, or <jk>null</jk> if no namespace is associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	/**
	 * Returns the XML format of this property from the {@link Xml#format} annotation on this bean property.
	 *
	 * @return The XML format, or {@link XmlFormat#DEFAULT} if annotation not specified.
	 */
	public XmlFormat getXmlFormat() {
		return xmlFormat;
	}

	/**
	 * Returns the child element of this property from the {@link Xml#childName} annotation on this bean property.
	 *
	 * @return The child element, or <jk>null</jk> if annotation not specified.
	 */
	public String getChildName() {
		return childName;
	}

	private void findXmlInfo(Xml xml, AnnotationProvider mp) {
		if (xml == null)
			return;
		BeanPropertyMeta bpm = getBeanPropertyMeta();
		ClassMeta<?> cmProperty = bpm.getClassMeta();
		ClassMeta<?> cmBean = bpm.getBeanMeta().getClassMeta();
		String name = bpm.getName();

		List<Xml> xmls = bpm.getAllAnnotationsParentFirst(Xml.class);
		List<XmlSchema> schemas = bpm.getAllAnnotationsParentFirst(XmlSchema.class);
		namespace = XmlUtils.findNamespace(xmls, schemas);

		if (xmlFormat == XmlFormat.DEFAULT)
			xmlFormat = xml.format();

		boolean isCollection = cmProperty.isCollectionOrArray();

		String cen = xml.childName();
		if ((! cen.isEmpty()) && (! isCollection))
			throw new BeanRuntimeException(cmProperty.getInnerClass(),
				"Annotation error on property ''{0}''.  @Xml.childName can only be specified on collections and arrays.", name);

		if (xmlFormat == XmlFormat.COLLAPSED) {
			if (isCollection) {
				if (cen.isEmpty() && xmlMetaProvider != null)
					cen = xmlMetaProvider.getXmlClassMeta(cmProperty).getChildName();
				if (cen == null || cen.isEmpty())
					cen = cmProperty.getElementType().getDictionaryName();
				if (cen == null || cen.isEmpty())
					cen = name;
			} else {
				throw new BeanRuntimeException(cmBean.getInnerClass(),
					"Annotation error on property ''{0}''.  @Xml.format=COLLAPSED can only be specified on collections and arrays.", name);
			}
			if (cen.isEmpty() && isCollection)
				cen = cmProperty.getDictionaryName();
		}

		if (! cen.isEmpty())
			childName = cen;
	}
}
