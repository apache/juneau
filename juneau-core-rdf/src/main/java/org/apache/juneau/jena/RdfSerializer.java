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
package org.apache.juneau.jena;

import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Serializes POJOs to RDF.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	Refer to <a class="doclink" href="package-summary.html#SerializerConfigurableProperties">Configurable Properties</a>
 * 		for the entire list of configurable properties.
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
 * <h5 class='section'>Additional information:</h5>
 * <p>
 * 	See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
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
			setLanguage(LANG_RDF_XML);
		}
	}

	/** Produces Abbreviated RDF/XML output */
	@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
	public static class XmlAbbrev extends RdfSerializer {
		/** Constructor */
		public XmlAbbrev() {
			setLanguage(LANG_RDF_XML_ABBREV);
		}
	}

	/** Produces N-Triple output */
	@Produces("text/n-triple")
	public static class NTriple extends RdfSerializer {
		/** Constructor */
		public NTriple() {
			setLanguage(LANG_NTRIPLE);
		}
	}

	/** Produces Turtle output */
	@Produces("text/turtle")
	public static class Turtle extends RdfSerializer {
		/** Constructor */
		public Turtle() {
			setLanguage(LANG_TURTLE);
		}
	}

	/** Produces N3 output */
	@Produces("text/n3")
	public static class N3 extends RdfSerializer {
		/** Constructor */
		public N3() {
			setLanguage(LANG_N3);
		}
	}


	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {

		RdfSerializerSession s = (RdfSerializerSession)session;

		Model model = s.getModel();
		Resource r = null;

		ClassMeta<?> cm = session.getClassMetaForObject(o);
		if (s.isLooseCollections() && cm != null && cm.isCollectionOrArray()) {
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

			if (s.isAddRootProp())
				r.addProperty(s.getRootProp(), "true");
		}

		s.getRdfWriter().write(model, session.getWriter(), "http://unknown/");
	}

	private RDFNode serializeAnything(RdfSerializerSession session, Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		Model m = session.getModel();

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> sType = object();   // The serialized type

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

			sType = aType.getSerializedClassMeta();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(session, o);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = session.getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta();
		}

		String typeName = session.getBeanTypeName(eType, aType, bpm);

		RDFNode n = null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (! session.isTrimNulls()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (sType.isUri() || isURI) {
			n = m.createResource(getUri(session, o, null));

		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(session.encodeTextInvalidChars(o));

		} else if (sType.isNumber() || sType.isBoolean()) {
			if (! session.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (sType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				Object uri = null;
				RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(bm);
				String uri2 = getUri(session, uri, null);
				n = m.createResource(uri2);
				serializeBeanMap(session, bm, (Resource)n, typeName);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(session, m2, (Resource)n, sType);
			}

		} else if (sType.isBean()) {
			BeanMap bm = session.toBeanMap(o);
			Object uri = null;
			RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm);
			String uri2 = getUri(session, uri, null);
			n = m.createResource(uri2);
			serializeBeanMap(session, bm, (Resource)n, typeName);

		} else if (sType.isCollectionOrArray() || (wType != null && wType.isCollection())) {
			Collection c = session.sort(sType.isCollection() ? (Collection)o : toList(sType.getInnerClass(), o));
			RdfCollectionFormat f = session.getCollectionFormat();
			RdfClassMeta rcm = sType.getExtendedMeta(RdfClassMeta.class);
			if (rcm.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = rcm.getCollectionFormat();
			if (bpm != null && bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat();
			switch (f) {
				case BAG: n = serializeToContainer(session, c, eType, m.createBag()); break;
				case LIST: n = serializeToList(session, c, eType); break;
				case MULTI_VALUED: serializeToMultiProperties(session, c, eType, bpm, attrName, parentResource); break;
				default: n = serializeToContainer(session, c, eType, m.createSeq());
			}
		} else {
			n = m.createLiteral(session.encodeTextInvalidChars(session.toString(o)));
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

	private void serializeBeanMap(RdfSerializerSession session, BeanMap<?> m, Resource r, String typeName) throws SerializeException {
		List<BeanPropertyValue> l = m.getValues(session.isTrimNulls(), typeName != null ? session.createBeanTypeNameProperty(m, typeName) : null);
		Collections.reverse(l);
		for (BeanPropertyValue bpv : l) {
			BeanPropertyMeta pMeta = bpv.getMeta();
			ClassMeta<?> cMeta = pMeta.getClassMeta();

			if (pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).isBeanUri())
				continue;

			String key = bpv.getName();
			Object value = bpv.getValue();
			Throwable t = bpv.getThrown();
			if (t != null)
				session.addBeanGetterWarning(pMeta, t);

			if (session.canIgnoreValue(cMeta, key, value))
				continue;

			BeanPropertyMeta bpm = bpv.getMeta();
			Namespace ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
			if (ns == null && session.isUseXmlNamespaces())
				ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
			if (ns == null)
				ns = session.getJuneauBpNs();
			else if (session.isAutoDetectNamespaces())
				session.addModelPrefix(ns);

			Property p = session.getModel().createProperty(ns.getUri(), session.encodeElementName(key));
			RDFNode n = serializeAnything(session, value, pMeta.isUri(), cMeta, key, pMeta, r);
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

	private void serializeToMultiProperties(RdfSerializerSession session, Collection c, ClassMeta<?> sType, BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {
		ClassMeta<?> elementType = sType.getElementType();
		for (Object e : c) {
			Namespace ns = null;
			if (bpm != null) {
				ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
				if (ns == null && session.isUseXmlNamespaces())
					ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
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
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public RdfSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new RdfSerializerSession(getContext(RdfSerializerContext.class), op, output, javaMethod, locale, timeZone, mediaType);
	}

	
	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  RDF language.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.language"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"RDF/XML-ABBREV"</js>
	 * </ul>
	 * <p>
	 * 	Can be any of the following:
	 * <ul class='spaced-list'>
	 * 	<li><js>"RDF/XML"</js>
	 * 	<li><js>"RDF/XML-ABBREV"</js>
	 * 	<li><js>"N-TRIPLE"</js>
	 * 	<li><js>"N3"</js> - General name for the N3 writer.
	 * 		Will make a decision on exactly which writer to use (pretty writer, plain writer or simple writer) when created.
	 * 		Default is the pretty writer but can be overridden with system property	<code>com.hp.hpl.jena.n3.N3JenaWriter.writer</code>.
	 * 	<li><js>"N3-PP"</js> - Name of the N3 pretty writer.
	 * 		The pretty writer uses a frame-like layout, with prefixing, clustering like properties and embedding one-referenced bNodes.
	 * 	<li><js>"N3-PLAIN"</js> - Name of the N3 plain writer.
	 * 		The plain writer writes records by subject.
	 * 	<li><js>"N3-TRIPLES"</js> - Name of the N3 triples writer.
	 * 		This writer writes one line per statement, like N-Triples, but does N3-style prefixing.
	 * 	<li><js>"TURTLE"</js> -  Turtle writer.
	 * 		http://www.dajobe.org/2004/01/turtle/
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_language</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfCommonContext#RDF_language
	 */
	public RdfSerializer setLanguage(String value) throws LockedException {
		return setProperty(RDF_language, value);
	}

	/**
	 * <b>Configuration property:</b>  XML namespace for Juneau properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.juneauNs"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{j:<js>'http://www.apache.org/juneau/'</js>}</code>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_juneauNs</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_juneauNs
	 */
	public RdfSerializer setJuneauNs(Namespace value) throws LockedException {
		return setProperty(RDF_juneauNs, value);
	}

	/**
	 * <b>Configuration property:</b>  Default XML namespace for bean properties.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.juneauBpNs"</js>
	 * 	<li><b>Data type:</b> {@link Namespace}
	 * 	<li><b>Default:</b> <code>{j:<js>'http://www.apache.org/juneaubp/'</js>}</code>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_juneauBpNs</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_juneauBpNs
	 */
	public RdfSerializer setJuneauBpNs(Namespace value) throws LockedException {
		return setProperty(RDF_juneauBpNs, value);
	}

	/**
	 * <b>Configuration property:</b>  Reuse XML namespaces when RDF namespaces not specified.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.useXmlNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * 	When specified, namespaces defined using {@link XmlNs} and {@link Xml} will be inherited by the RDF serializers.
	 * 	Otherwise, namespaces will be defined using {@link RdfNs} and {@link Rdf}.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_useXmlNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see SerializerContext#SERIALIZER_sortMaps
	 */
	public RdfSerializer setUseXmlNamespaces(boolean value) throws LockedException {
		return setProperty(RDF_useXmlNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Add XSI data types to non-<code>String</code> literals.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addLiteralTypes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_addLiteralTypes</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_addLiteralTypes
	 */
	public RdfSerializer setAddLiteralTypes(boolean value) throws LockedException {
		return setProperty(RDF_addLiteralTypes, value);
	}

	/**
	 * <b>Configuration property:</b>  Add RDF root identifier property to root node.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addRootProperty"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without
	 * 	incoming predicates to identify root notes, which can introduce a considerable performance
	 * 	degradation.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_addRootProperty</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_addRootProperty
	 */
	public RdfSerializer setAddRootProperty(boolean value) throws LockedException {
		return setProperty(RDF_addRootProperty, value);
	}

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_autoDetectNamespaces</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_autoDetectNamespaces
	 */
	public RdfSerializer setAutoDetectNamespaces(boolean value) throws LockedException {
		return setProperty(RDF_autoDetectNamespaces, value);
	}

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.namespaces.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_namespaces</jsf>, values)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param values The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfSerializerContext#RDF_namespaces
	 */
	public RdfSerializer setNamespaces(Namespace...values) throws LockedException {
		return setProperty(RDF_namespaces, values);
	}

	/**
	 * <b>Configuration property:</b>  RDF format for representing collections and arrays.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.collectionFormat"</js>
	 * 	<li><b>Data type:</b> <code>RdfCollectionFormat</code>
	 * 	<li><b>Default:</b> <js>"DEFAULT"</js>
	 * </ul>
	 * <p>
	 * 	Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"DEFAULT"</js> - Default format.  The default is an RDF Sequence container.
	 * 	<li><js>"SEQ"</js> - RDF Sequence container.
	 * 	<li><js>"BAG"</js> - RDF Bag container.
	 * 	<li><js>"LIST"</js> - RDF List container.
	 * 	<li><js>"MULTI_VALUED"</js> - Multi-valued properties.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If you use <js>"BAG"</js> or <js>"MULTI_VALUED"</js>, the order of the elements in the collection will get lost.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_collectionFormat</jsf>, value)</code>.
	 * 	<li>This introduces a slight performance penalty.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfCommonContext#RDF_collectionFormat
	 */
	public RdfSerializer setCollectionFormat(RdfCollectionFormat value) throws LockedException {
		return setProperty(RDF_collectionFormat, value);
	}

	/**
	 * <b>Configuration property:</b>  Collections should be serialized and parsed as loose collections.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Rdf.looseCollections"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When specified, collections of resources are handled as loose collections of resources in RDF instead of
	 * resources that are children of an RDF collection (e.g. Sequence, Bag).
	 * <p>
	 * Note that this setting is specialized for RDF syntax, and is incompatible with the concept of
	 * losslessly representing POJO models, since the tree structure of these POJO models are lost
	 * when serialized as loose collections.
	 *	<p>
	 *	This setting is typically only useful if the beans being parsed into do not have a bean property
	 *	annotated with {@link Rdf#beanUri @Rdf(beanUri=true)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	WriterSerializer s = <jk>new</jk> RdfSerializer.XmlAbbrev().setLooseCollections(<jk>true</jk>);
	 * 	ReaderParser p = <jk>new</jk> RdfParser.Xml().setLooseCollections(<jk>true</jk>);
	 *
	 * 	List&lt;MyBean&gt; l = createListOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(l);
	 *
	 *	<jc>// Parse back into a Java collection</jc>
	 * 	l = p.parse(rdfXml, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	MyBean[] b = createArrayOfMyBeans();
	 *
	 * 	<jc>// Serialize to RDF/XML as loose resources</jc>
	 * 	String rdfXml = s.serialize(b);
	 *
	 *	<jc>// Parse back into a bean array</jc>
	 * 	b = p.parse(rdfXml, MyBean[].<jk>class</jk>);
	 * </p>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_looseCollections</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfCommonContext#RDF_looseCollections
	 */
	public RdfSerializer setLooseCollections(boolean value) throws LockedException {
		return setProperty(RDF_looseCollections, value);
	}

	@Override /* Serializer */
	public RdfSerializer setMaxDepth(int value) throws LockedException {
		super.setMaxDepth(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setInitialDepth(int value) throws LockedException {
		super.setInitialDepth(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setDetectRecursions(boolean value) throws LockedException {
		super.setDetectRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setIgnoreRecursions(boolean value) throws LockedException {
		super.setIgnoreRecursions(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setUseWhitespace(boolean value) throws LockedException {
		super.setUseWhitespace(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setAddBeanTypeProperties(boolean value) throws LockedException {
		super.setAddBeanTypeProperties(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setQuoteChar(char value) throws LockedException {
		super.setQuoteChar(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setTrimNullProperties(boolean value) throws LockedException {
		super.setTrimNullProperties(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setTrimEmptyCollections(boolean value) throws LockedException {
		super.setTrimEmptyCollections(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setTrimEmptyMaps(boolean value) throws LockedException {
		super.setTrimEmptyMaps(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setRelativeUriBase(String value) throws LockedException {
		super.setRelativeUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setAbsolutePathUriBase(String value) throws LockedException {
		super.setAbsolutePathUriBase(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setSortCollections(boolean value) throws LockedException {
		super.setSortCollections(value);
		return this;
	}

	@Override /* Serializer */
	public RdfSerializer setSortMaps(boolean value) throws LockedException {
		super.setSortMaps(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfSerializer removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}
	

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
