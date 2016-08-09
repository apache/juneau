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
package org.apache.juneau.jena;

import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommonContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Serializes POJOs to RDF.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#SerializerConfigurableProperties'>Configurable Properties</a>
 * 		for the entire list of configurable properties.
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for language-specific serializers:
 * <ul>
 * 	<li>{@link RdfSerializer.Xml} - RDF/XML.
 * 	<li>{@link RdfSerializer.XmlAbbrev} - RDF/XML-ABBREV.
 * 	<li>{@link RdfSerializer.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfSerializer.Turtle} - TURTLE.
 * 	<li>{@link RdfSerializer.N3} - N3.
 * </ul>
 *
 *
 * <h6 class='topic'>Additional Information</h6>
 * <p>
 * 	See <a class='doclink' href='package-summary.html#TOC'>RDF Overview</a> for an overview of RDF support in Juneau.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
public class RdfSerializer extends WriterSerializer {

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XML = new RdfSerializer.Xml().lock();

	/** Default Abbreviated RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XMLABBREV = new RdfSerializer.XmlAbbrev().lock();

	/** Default Turtle serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_TURTLE = new RdfSerializer.Turtle().lock();

	/** Default N-Triple serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_NTRIPLE = new RdfSerializer.NTriple().lock();

	/** Default N3 serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_N3 = new RdfSerializer.N3().lock();


	/** Produces RDF/XML output */
	@Produces("text/xml+rdf")
	public static class Xml extends RdfSerializer {
		/** Constructor */
		public Xml() {
			setProperty(RDF_language, LANG_RDF_XML);
		}
	}

	/** Produces Abbreviated RDF/XML output */
	@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
	public static class XmlAbbrev extends RdfSerializer {
		/** Constructor */
		public XmlAbbrev() {
			setProperty(RDF_language, LANG_RDF_XML_ABBREV);
		}
	}

	/** Produces N-Triple output */
	@Produces("text/n-triple")
	public static class NTriple extends RdfSerializer {
		/** Constructor */
		public NTriple() {
			setProperty(RDF_language, LANG_NTRIPLE);
		}
	}

	/** Produces Turtle output */
	@Produces("text/turtle")
	public static class Turtle extends RdfSerializer {
		/** Constructor */
		public Turtle() {
			setProperty(RDF_language, LANG_TURTLE);
		}
	}

	/** Produces N3 output */
	@Produces("text/n3")
	public static class N3 extends RdfSerializer {
		/** Constructor */
		public N3() {
			setProperty(RDF_language, LANG_N3);
		}
	}


	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {

		RdfSerializerSession s = (RdfSerializerSession)session;

		Model model = s.getModel();
		Resource r = null;

		ClassMeta<?> cm = s.getBeanContext().getClassMetaForObject(o);
		if (s.isLooseCollection() && cm != null && (cm.isCollection() || cm.isArray())) {
			Collection c = s.sort(cm.isCollection() ? (Collection)o : toList(cm.getInnerClass(), o));
			for (Object o2 : c)
				serializeAnything(s, o2, false, object(), "root", null, null);
		} else {
			RDFNode n = serializeAnything(s, o, false, object(), "root", null, null);
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(s.getValueProperty(), n);
			} else {
				r = n.asResource();
			}

			if (s.isAddRootProperty())
				r.addProperty(s.getRootProperty(), "true");
		}

		s.getRdfWriter().write(model, session.getWriter(), "http://unknown/");
	}

	private RDFNode serializeAnything(RdfSerializerSession session, Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		Model m = session.getModel();
		BeanContext bc = session.getBeanContext();

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> gType = object();   // The generic type

		aType = session.push(attrName, o, eType);

		if (eType == null)
			eType = object();

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			gType = aType.getTransformedClassMeta();

			// Transform if necessary
			PojoTransform transform = aType.getPojoTransform();
			if (transform != null) {
				o = transform.transform(o);

				// If the transforms getTransformedClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}
		} else {
			gType = eType.getTransformedClassMeta();
		}

		RDFNode n = null;

		if (o == null || gType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (! session.isTrimNulls()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (gType.isUri() || isURI) {
			n = m.createResource(getUri(session, o, null));

		} else if (gType.isCharSequence() || gType.isChar()) {
			n = m.createLiteral(session.encodeTextInvalidChars(o));

		} else if (gType.isNumber() || gType.isBoolean()) {
			if (! session.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (gType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				String uri = getUri(session, bm.getBeanUri(), null);
				n = m.createResource(uri);
				serializeBeanMap(session, bm, (Resource)n);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(session, m2, (Resource)n, gType);
			}

		} else if (gType.hasToObjectMapMethod()) {
			Map m2 = gType.toObjectMap(o);
			n = m.createResource();
			serializeMap(session, m2, (Resource)n, gType);

		} else if (gType.isBean()) {
			BeanMap bm = bc.forBean(o);
			String uri = getUri(session, bm.getBeanUri(), null);
			n = m.createResource(uri);
			serializeBeanMap(session, bm, (Resource)n);

		} else if (gType.isCollection() || gType.isArray() || (wType != null && wType.isCollection())) {
			Collection c = session.sort(gType.isCollection() ? (Collection)o : toList(gType.getInnerClass(), o));
			RdfCollectionFormat f = session.getCollectionFormat();
			if (gType.getRdfMeta().getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = gType.getRdfMeta().getCollectionFormat();
			if (bpm != null && bpm.getRdfMeta().getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpm.getRdfMeta().getCollectionFormat();
			switch (f) {
				case BAG: n = serializeToContainer(session, c, gType, m.createBag()); break;
				case LIST: n = serializeToList(session, c, gType); break;
				case MULTI_VALUED: serializeToMultiProperties(session, c, gType, bpm, attrName, parentResource); break;
				default: n = serializeToContainer(session, c, gType, m.createSeq());
			}
		} else {
			n = m.createLiteral(session.encodeTextInvalidChars(session.toString(o)));
		}

		if (session.isAddClassAttrs() && n != null && n.isResource()) {
			if (o != null && ! eType.equals(aType))
				n.asResource().addProperty(session.getClassProperty(), aType.toString());
			else if (o == null)
				n.asResource().addProperty(session.getClassProperty(), eType.toString());
		}

		session.pop();

		return n;
	}

	private String getUri(RdfSerializerSession session, Object uri, Object uri2) {
		String s = null;
		if (uri != null)
			s = uri.toString();
		if ((s == null || s.isEmpty()) && uri2 != null)
			s = uri2.toString();
		if (s == null)
			return null;
		if (s.indexOf("://") == -1) {
			String aUri = session.getAbsolutePathUriBase();
			String rUri = session.getRelativeUriBase();
			if (StringUtils.startsWith(s, '/')) {
				if (aUri != null)
					return aUri + s;
			} else {
				if (rUri != null) {
					if (rUri.equals("/"))
						return '/' + s;
					return rUri + '/' + s;
				}
			}
		}
		return s;
	}

	private void serializeMap(RdfSerializerSession session, Map m, Resource r, ClassMeta<?> type) throws SerializeException {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		ArrayList<Map.Entry<Object,Object>> l = new ArrayList<Map.Entry<Object,Object>>(m.entrySet());
		Collections.reverse(l);
		for (Map.Entry<Object,Object> me : l) {
			Object value = me.getValue();

			Object key = session.generalize(me.getKey(), keyType);

			Namespace ns = session.getJuneauBpNs();
			Model model = session.getModel();
			Property p = model.createProperty(ns.getUri(), session.encodeElementName(session.toString(key)));
			RDFNode n = serializeAnything(session, value, false, valueType, key == null ? null : session.toString(key), null, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}

	private void serializeBeanMap(RdfSerializerSession session, BeanMap<?> m, Resource r) throws SerializeException {
		List<BeanPropertyValue> l = m.getValues(false, session.isTrimNulls());
		Collections.reverse(l);
		for (BeanPropertyValue bpv : l) {
			BeanPropertyMeta pMeta = bpv.getMeta();
			ClassMeta<?> cm = pMeta.getClassMeta();

			if (pMeta.isBeanUri())
				continue;

			String key = bpv.getName();
			Object value = bpv.getValue();
			Throwable t = bpv.getThrown();
			if (t != null)
				session.addBeanGetterWarning(pMeta, t);

			if (session.canIgnoreValue(cm, key, value))
				continue;

			BeanPropertyMeta bpm = bpv.getMeta();
			Namespace ns = bpm.getRdfMeta().getNamespace();
			if (ns == null && session.isUseXmlNamespaces())
				ns = bpm.getXmlMeta().getNamespace();
			if (ns == null)
				ns = session.getJuneauBpNs();
			else if (session.isAutoDetectNamespaces())
				session.addModelPrefix(ns);

			Property p = session.getModel().createProperty(ns.getUri(), session.encodeElementName(key));
			RDFNode n = serializeAnything(session, value, pMeta.isUri(), cm, key, pMeta, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}


	private Container serializeToContainer(RdfSerializerSession session, Collection c, ClassMeta<?> type, Container list) throws SerializeException {

		ClassMeta<?> elementType = type.getElementType();
		for (Object e : c) {
			RDFNode n = serializeAnything(session, e, false, elementType, null, null, null);
			list = list.add(n);
		}
		return list;
	}

	private RDFList serializeToList(RdfSerializerSession session, Collection c, ClassMeta<?> type) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		List<RDFNode> l = new ArrayList<RDFNode>(c.size());
		for (Object e : c) {
			l.add(serializeAnything(session, e, false, elementType, null, null, null));
		}
		return session.getModel().createList(l.iterator());
	}

	private void serializeToMultiProperties(RdfSerializerSession session, Collection c, ClassMeta<?> gType, BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {
		ClassMeta<?> elementType = gType.getElementType();
		for (Object e : c) {
			Namespace ns = null;
			if (bpm != null) {
				ns = bpm.getRdfMeta().getNamespace();
				if (ns == null && session.isUseXmlNamespaces())
					ns = bpm.getXmlMeta().getNamespace();
			}
			if (ns == null)
				ns = session.getJuneauBpNs();
			else if (session.isAutoDetectNamespaces())
				session.addModelPrefix(ns);
			RDFNode n2 = serializeAnything(session, e, false, elementType, null, null, null);
			Property p = session.getModel().createProperty(ns.getUri(), session.encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		}

	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public RdfSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new RdfSerializerSession(getContext(RdfSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* CoreApi */
	public RdfSerializer setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addTransforms(Class<?>...classes) throws LockedException {
		super.addTransforms(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> RdfSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public RdfSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public RdfSerializer clone() {
		try {
			return (RdfSerializer)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
