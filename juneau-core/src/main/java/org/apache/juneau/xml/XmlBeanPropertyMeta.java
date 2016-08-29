/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.xml;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Metadata on bean properties specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the bean property.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class XmlBeanPropertyMeta extends BeanPropertyMetaExtended {

	private Namespace namespace = null;
	private XmlFormat xmlFormat = XmlFormat.NORMAL;
	private XmlContentHandler<?> xmlContentHandler = null;
	private String childName;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 */
	public XmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		super(bpm);

		if (bpm.getField() != null)
			findXmlInfo(bpm.getField().getAnnotation(Xml.class));
		if (bpm.getGetter() != null)
			findXmlInfo(bpm.getGetter().getAnnotation(Xml.class));
		if (bpm.getSetter() != null)
			findXmlInfo(bpm.getSetter().getAnnotation(Xml.class));

		if (namespace == null)
			namespace = bpm.getBeanMeta().getClassMeta().getExtendedMeta(XmlClassMeta.class).getNamespace();
	}

	/**
	 * Returns the XML namespace associated with this bean property.
	 * <p>
	 * 	Namespace is determined in the following order:
	 * <ol>
	 * 	<li>{@link Xml#prefix()} annotation defined on bean property field.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean getter.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean setter.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean package.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean superclasses.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean superclass packages.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean interfaces.
	 * 	<li>{@link Xml#prefix()} annotation defined on bean interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this bean property, or <jk>null</jk> if no namespace is
	 * 	associated with it.
	 */
	public Namespace getNamespace() {
		return namespace;
	}

	/**
	 * Returns the XML format of this property from the {@link Xml#format} annotation on this bean property.
	 *
	 * @return The XML format, or {@link XmlFormat#NORMAL} if annotation not specified.
	 */
	protected XmlFormat getXmlFormat() {
		return xmlFormat;
	}

	/**
	 * Returns the XML content handler of this property from the {@link Xml#contentHandler} annotation on this bean property.
	 *
	 * @return The XML content handler, or <jk>null</jk> if annotation not specified.
	 */
	protected XmlContentHandler<?> getXmlContentHandler() {
		return xmlContentHandler;
	}

	/**
	 * Returns the child element of this property from the {@link Xml#childName} annotation on this bean property.
	 *
	 * @return The child element, or <jk>null</jk> if annotation not specified.
	 */
	protected String getChildName() {
		return childName;
	}

	private void findXmlInfo(Xml xml) {
		if (xml == null)
			return;
		BeanPropertyMeta bpm = getBeanPropertyMeta();
		ClassMeta<?> cmProperty = bpm.getClassMeta();
		ClassMeta<?> cmBean = bpm.getBeanMeta().getClassMeta();
		String name = bpm.getName();
		if (! xml.name().isEmpty())
			throw new BeanRuntimeException(cmBean.getInnerClass(), "Annotation error on property ''{0}''.  Found @Xml.name annotation can only be specified on types.", name);

		List<Xml> xmls = bpm.findAnnotations(Xml.class);
		List<XmlSchema> schemas = bpm.findAnnotations(XmlSchema.class);
		namespace = XmlUtils.findNamespace(xmls, schemas);

		if (xmlFormat == XmlFormat.NORMAL)
			xmlFormat = xml.format();

		boolean isCollection = cmProperty.isCollection() || cmProperty.isArray();

		String cen = xml.childName();
		if ((! cen.isEmpty()) && (! isCollection))
			throw new BeanRuntimeException(cmProperty.getInnerClass(), "Annotation error on property ''{0}''.  @Xml.childName can only be specified on collections and arrays.", name);

		if (xmlFormat == XmlFormat.COLLAPSED) {
			if (isCollection) {
				if (cen.isEmpty())
					cen = cmProperty.getExtendedMeta(XmlClassMeta.class).getChildName();
				if (cen == null || cen.isEmpty())
					cen = cmProperty.getElementType().getExtendedMeta(XmlClassMeta.class).getElementName();
				if (cen == null || cen.isEmpty())
					cen = name;
			} else {
				throw new BeanRuntimeException(cmBean.getInnerClass(), "Annotation error on property ''{0}''.  @Xml.format=COLLAPSED can only be specified on collections and arrays.", name);
			}
			if (cen.isEmpty() && isCollection)
				cen = cmProperty.getExtendedMeta(XmlClassMeta.class).getElementName();
		}

		try {
			if (xmlFormat == XmlFormat.CONTENT && xml.contentHandler() != XmlContentHandler.NULL.class)
				xmlContentHandler = xml.contentHandler().newInstance();
		} catch (Exception e) {
			throw new BeanRuntimeException(cmBean.getInnerClass(), "Could not instantiate content handler ''{0}''", xml.contentHandler().getName()).initCause(e);
		}

		if (! cen.isEmpty())
			childName = cen;
	}
}
