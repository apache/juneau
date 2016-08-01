/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import java.util.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.annotation.*;


/**
 * Metadata on classes specific to the XML serializers and parsers pulled from the {@link Xml @Xml} annotation on the class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class XmlClassMeta {

	private final Namespace namespace;
	private final Xml xml;
	private final XmlFormat format;
	private final String elementName;
	private final String childName;

	/**
	 * Constructor.
	 *
	 * @param c The class that this annotation is defined on.
	 */
	public XmlClassMeta(Class<?> c) {
		this.namespace = findNamespace(c);
		this.xml =  ReflectionUtils.getAnnotation(Xml.class, c);
		if (xml != null) {
			this.format = xml.format();
			this.elementName = StringUtils.nullIfEmpty(xml.name());
			this.childName = StringUtils.nullIfEmpty(xml.childName());

		} else {
			this.format = XmlFormat.NORMAL;
			this.elementName = null;
			this.childName = null;
		}
	}

	/**
	 * Returns the {@link Xml} annotation defined on the class.
	 *
	 * @return The value of the {@link Xml} annotation defined on the class, or <jk>null</jk> if annotation is not specified.
	 */
	protected Xml getAnnotation() {
		return xml;
	}

	/**
	 * Returns the {@link Xml#format()} annotation defined on the class.
	 *
	 * @return The value of the {@link Xml#format()} annotation, or {@link XmlFormat#NORMAL} if not specified.
	 */
	protected XmlFormat getFormat() {
		return format;
	}

	/**
	 * Returns the {@link Xml#name()} annotation defined on the class.
	 *
	 * @return The value of the {@link Xml#name()} annotation, or <jk>null</jk> if not specified.
	 */
	protected String getElementName() {
		return elementName;
	}

	/**
	 * Returns the {@link Xml#childName()} annotation defined on the class.
	 *
	 * @return The value of the {@link Xml#childName()} annotation, or <jk>null</jk> if not specified.
	 */
	protected String getChildName() {
		return childName;
	}

	/**
	 * Returns the XML namespace associated with this class.
	 * <p>
	 * 	Namespace is determined in the following order:
	 * <ol>
	 * 	<li>{@link Xml#prefix()} annotation defined on class.
	 * 	<li>{@link Xml#prefix()} annotation defined on package.
	 * 	<li>{@link Xml#prefix()} annotation defined on superclasses.
	 * 	<li>{@link Xml#prefix()} annotation defined on superclass packages.
	 * 	<li>{@link Xml#prefix()} annotation defined on interfaces.
	 * 	<li>{@link Xml#prefix()} annotation defined on interface packages.
	 * </ol>
	 *
	 * @return The namespace associated with this class, or <jk>null</jk> if no namespace is
	 * 	associated with it.
	 */
	protected Namespace getNamespace() {
		return namespace;
	}

	private Namespace findNamespace(Class<?> c) {
		if (c == null)
			return null;

		List<Xml> xmls = ReflectionUtils.findAnnotations(Xml.class, c);
		List<XmlSchema> schemas = ReflectionUtils.findAnnotations(XmlSchema.class, c);
		return XmlUtils.findNamespace(xmls, schemas);
	}
}
