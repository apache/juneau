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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfParserContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.*;

/**
 * Parses RDF into POJOs.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * 	Refer to <a class="doclink" href="package-summary.html#ParserConfigurableProperties">Configurable Properties</a>
 * 		for the entire list of configurable properties.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for language-specific parsers:
 * <ul class='spaced-list'>
 * 	<li>{@link RdfParser.Xml} - RDF/XML and RDF/XML-ABBREV.
 * 	<li>{@link RdfParser.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfParser.Turtle} - TURTLE.
 * 	<li>{@link RdfParser.N3} - N3.
 * </ul>
 *
 * <h5 class='section'>Additional information:</h5>
 * <p>
 * 	See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
 */
@Consumes(value="text/xml+rdf")
public class RdfParser extends ReaderParser {

	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new RdfParser.Xml().lock();

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new RdfParser.Turtle().lock();

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new RdfParser.NTriple().lock();

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new RdfParser.N3().lock();


	/** Consumes RDF/XML input */
	@Consumes("text/xml+rdf")
	public static class Xml extends RdfParser {
		/** Constructor */
		public Xml() {
			setLanguage(LANG_RDF_XML);
		}
	}

	/** Consumes N-Triple input */
	@Consumes(value="text/n-triple")
	public static class NTriple extends RdfParser {
		/** Constructor */
		public NTriple() {
			setLanguage(LANG_NTRIPLE);
		}
	}

	/** Consumes Turtle input */
	@Consumes(value="text/turtle")
	public static class Turtle extends RdfParser {
		/** Constructor */
		public Turtle() {
			setLanguage(LANG_TURTLE);
		}
	}

	/** Consumes N3 input */
	@Consumes(value="text/n3")
	public static class N3 extends RdfParser {
		/** Constructor */
		public N3() {
			setLanguage(LANG_N3);
		}
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override /* ReaderParser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {

		RdfParserSession s = (RdfParserSession)session;

		Model model = s.getModel();
		RDFReader r = s.getRdfReader();
		r.read(model, session.getReader(), null);

		List<Resource> roots = getRoots(s, model);

		// Special case where we're parsing a loose collection of resources.
		if (s.isLooseCollections() && type.isCollectionOrArray()) {
			Collection c = null;
			if (type.isArray())
				c = new ArrayList();
			else
				c = (type.canCreateNewInstance(session.getOuter()) ? (Collection<?>)type.newInstance(session.getOuter()) : new ObjectList(session));
			for (Resource resource : roots)
				c.add(parseAnything(s, type.getElementType(), resource, session.getOuter(), null));

			if (type.isArray())
				return (T)session.toArray(type, c);
			return (T)c;
		}

		if (roots.isEmpty())
			return null;
		if (roots.size() > 1)
			throw new ParseException(session, "Too many root nodes found in model:  {0}", roots.size());
		Resource resource = roots.get(0);

		return parseAnything(s, type, resource, session.getOuter(), null);
	}

	/*
	 * Finds the roots in the model using either the "root" property to identify it,
	 * 	or by resorting to scanning the model for all nodes with no incoming predicates.
	 */
	private List<Resource> getRoots(RdfParserSession session, Model m) {
		List<Resource> l = new LinkedList<Resource>();

		// First try to find the root using the "http://www.apache.org/juneau/root" property.
		Property root = m.createProperty(session.getJuneauNsUri(), RDF_juneauNs_ROOT);
		for (ResIterator i  = m.listResourcesWithProperty(root); i.hasNext();)
			l.add(i.next());

		if (! l.isEmpty())
			return l;

		// Otherwise, we need to find all resources that aren't objects.
		// We want to explicitly ignore statements where the subject
		// and object are the same node.
		Set<RDFNode> objects = new HashSet<RDFNode>();
		for (StmtIterator i = m.listStatements(); i.hasNext();) {
			Statement st = i.next();
			RDFNode subject = st.getSubject();
			RDFNode object = st.getObject();
			if (object.isResource() && ! object.equals(subject))
				objects.add(object);
		}
		for (ResIterator i = m.listSubjects(); i.hasNext();) {
			Resource r = i.next();
			if (! objects.contains(r))
				l.add(r);
		}
		return l;
	}

	private <T> BeanMap<T> parseIntoBeanMap(RdfParserSession session, Resource r2, BeanMap<T> m) throws Exception {
		BeanMeta<T> bm = m.getMeta();
		RdfBeanMeta rbm = bm.getExtendedMeta(RdfBeanMeta.class);
		if (rbm.hasBeanUri() && r2.getURI() != null)
			rbm.getBeanUriProperty().set(m, r2.getURI());
		Property subTypeIdProperty = null;
		for (StmtIterator i = r2.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
			if (subTypeIdProperty != null && p.equals(subTypeIdProperty))
				continue;
			String key = session.decodeString(p.getLocalName());
			BeanPropertyMeta pMeta = m.getPropertyMeta(key);
			session.setCurrentProperty(pMeta);
			if (pMeta != null) {
				RDFNode o = st.getObject();
				ClassMeta<?> cm = pMeta.getClassMeta();
				if (cm.isCollectionOrArray() && isMultiValuedCollections(session, pMeta)) {
					ClassMeta<?> et = cm.getElementType();
					Object value = parseAnything(session, et, o, m.getBean(false), pMeta);
					setName(et, value, key);
					pMeta.add(m, value);
				} else {
					Object value = parseAnything(session, cm, o, m.getBean(false), pMeta);
					setName(cm, value, key);
					pMeta.set(m, value);
				}
			} else if (! (p.equals(session.getRootProperty()) || p.equals(session.getTypeProperty()) || (subTypeIdProperty != null && p.equals(subTypeIdProperty)))) {
				onUnknownProperty(session, key, m, -1, -1);
			}
			session.setCurrentProperty(null);
		}
		return m;
	}

	private boolean isMultiValuedCollections(RdfParserSession session, BeanPropertyMeta pMeta) {
		if (pMeta != null && pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() != RdfCollectionFormat.DEFAULT)
			return pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
		return session.getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T parseAnything(RdfParserSession session, ClassMeta<T> eType, RDFNode n, Object outer, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();
		session.setCurrentClass(sType);

		if (! sType.canCreateNewInstance(outer)) {
			if (n.isResource()) {
				Statement st = n.asResource().getProperty(session.getTypeProperty());
				if (st != null) {
 					String c = st.getLiteral().getString();
 					ClassMeta tcm = session.getClassMeta(c, pMeta, eType);
 					if (tcm != null)
 						sType = eType = tcm;
				}
			}
		}

		Object o = null;
		if (n.isResource() && n.asResource().getURI() != null && n.asResource().getURI().equals(RDF_NIL)) {
			// Do nothing.  Leave o == null.
		} else if (sType.isObject()) {
			if (n.isLiteral()) {
				o = n.asLiteral().getValue();
				if (o instanceof String) {
					o = session.decodeString(o);
				}
			}
			else if (n.isResource()) {
				Resource r = n.asResource();
				if (session.wasAlreadyProcessed(r))
					o = r.getURI();
				else if (r.getProperty(session.getValueProperty()) != null) {
					o = parseAnything(session, object(), n.asResource().getProperty(session.getValueProperty()).getObject(), outer, null);
				} else if (isSeq(session, r)) {
					o = new ObjectList(session);
					parseIntoCollection(session, r.as(Seq.class), (Collection)o, sType.getElementType(), pMeta);
				} else if (isBag(session, r)) {
					o = new ObjectList(session);
					parseIntoCollection(session, r.as(Bag.class), (Collection)o, sType.getElementType(), pMeta);
				} else if (r.canAs(RDFList.class)) {
					o = new ObjectList(session);
					parseIntoCollection(session, r.as(RDFList.class), (Collection)o, sType.getElementType(), pMeta);
				} else {
					// If it has a URI and no child properties, we interpret this as an
					// external resource, and convert it to just a URL.
					String uri = r.getURI();
					if (uri != null && ! r.listProperties().hasNext()) {
						o = r.getURI();
					} else {
						ObjectMap m2 = new ObjectMap(session);
						parseIntoMap(session, r, m2, null, null, pMeta);
						o = session.cast(m2, pMeta, eType);
					}
				}
			} else {
				throw new ParseException(session, "Unrecognized node type ''{0}'' for object", n);
			}
		} else if (sType.isBoolean()) {
			o = session.convertToType(getValue(session, n, outer), boolean.class);
		} else if (sType.isCharSequence()) {
			o = session.decodeString(getValue(session, n, outer));
		} else if (sType.isChar()) {
			o = session.decodeString(getValue(session, n, outer)).charAt(0);
		} else if (sType.isNumber()) {
			o = parseNumber(getValue(session, n, outer).toString(), (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.isMap()) {
			Resource r = n.asResource();
			if (session.wasAlreadyProcessed(r))
				return null;
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(session));
			o = parseIntoMap(session, r, m, eType.getKeyType(), eType.getValueType(), pMeta);
		} else if (sType.isCollectionOrArray()) {
			if (sType.isArray())
				o = new ArrayList();
			else
				o = (sType.canCreateNewInstance(outer) ? (Collection<?>)sType.newInstance(outer) : new ObjectList(session));
			Resource r = n.asResource();
			if (session.wasAlreadyProcessed(r))
				return null;
			if (isSeq(session, r)) {
				parseIntoCollection(session, r.as(Seq.class), (Collection)o, sType.getElementType(), pMeta);
			} else if (isBag(session, r)) {
				parseIntoCollection(session, r.as(Bag.class), (Collection)o, sType.getElementType(), pMeta);
			} else if (r.canAs(RDFList.class)) {
				parseIntoCollection(session, r.as(RDFList.class), (Collection)o, sType.getElementType(), pMeta);
			} else {
				throw new ParseException("Unrecognized node type ''{0}'' for collection", n);
			}
			if (sType.isArray())
				o = session.toArray(sType, (Collection)o);
		} else if (sType.canCreateNewBean(outer)) {
			Resource r = n.asResource();
			if (session.wasAlreadyProcessed(r))
				return null;
			BeanMap<?> bm = session.newBeanMap(outer, sType.getInnerClass());
			o = parseIntoBeanMap(session, r, bm).getBean();
		} else if (sType.isUri() && n.isResource()) {
			o = sType.newInstanceFromString(outer, session.decodeString(n.asResource().getURI()));
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, session.decodeString(getValue(session, n, outer)));
		} else if (sType.canCreateNewInstanceFromNumber(outer)) {
			o = sType.newInstanceFromNumber(session, outer, parseNumber(getValue(session, n, outer).toString(), sType.getNewInstanceFromNumberClass()));
		} else if (n.isResource()) {
			Resource r = n.asResource();
			Map m = new ObjectMap(session);
			parseIntoMap(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (m.containsKey(session.getBeanTypePropertyName()))
				o = session.cast((ObjectMap)m, pMeta, eType);
			else
				throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else {
			throw new ParseException("Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private boolean isSeq(RdfParserSession session, RDFNode n) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(session.getRdfTypeProperty());
			if (st != null)
				return RDF_SEQ.equals(st.getResource().getURI());
		}
		return false;
	}

	private boolean isBag(RdfParserSession session, RDFNode n) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(session.getRdfTypeProperty());
			if (st != null)
				return RDF_BAG.equals(st.getResource().getURI());
		}
		return false;
	}

	private Object getValue(RdfParserSession session, RDFNode n, Object outer) throws Exception {
		if (n.isLiteral())
			return n.asLiteral().getValue();
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(session.getValueProperty());
			if (st != null) {
				n = st.getObject();
				if (n.isLiteral())
					return n.asLiteral().getValue();
				return parseAnything(session, object(), st.getObject(), outer, null);
			}
		}
		throw new ParseException(session, "Unknown value type for node ''{0}''", n);
	}

	private <K,V> Map<K,V> parseIntoMap(RdfParserSession session, Resource r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {
		// Add URI as "uri" to generic maps.
		if (r.getURI() != null) {
			K uri = convertAttrToType(session, m, "uri", keyType);
			V value = convertAttrToType(session, m, r.getURI(), valueType);
			m.put(uri, value);
		}
		for (StmtIterator i = r.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
			String key = p.getLocalName();
			if (! (key.equals("root") && p.getURI().equals(session.getJuneauNsUri()))) {
				key = session.decodeString(key);
				RDFNode o = st.getObject();
				K key2 = convertAttrToType(session, m, key, keyType);
				V value = parseAnything(session, valueType, o, m, pMeta);
				setName(valueType, value, key);
				m.put(key2, value);
			}

		}
		// TODO Auto-generated method stub
		return m;
	}

	private <E> Collection<E> parseIntoCollection(RdfParserSession session, Container c, Collection<E> l, ClassMeta<E> et, BeanPropertyMeta pMeta) throws Exception {
		for (NodeIterator ni = c.iterator(); ni.hasNext();) {
			E e = parseAnything(session, et, ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}

	private <E> Collection<E> parseIntoCollection(RdfParserSession session, RDFList list, Collection<E> l, ClassMeta<E> et, BeanPropertyMeta pMeta) throws Exception {
		for (ExtendedIterator<RDFNode> ni = list.iterator(); ni.hasNext();) {
			E e = parseAnything(session, et, ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}

	
	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public RdfParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new RdfParserSession(getContext(RdfParserContext.class), op, input, javaMethod, outer, locale, timeZone, mediaType);
	}
	

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * <b>Configuration property:</b>  Trim whitespace from text elements.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfParser.trimWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>setProperty(<jsf>RDF_trimWhitespace</jsf>, value)</code>.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfParserContext#RDF_trimWhitespace
	 */
	public RdfParser setTrimWhitespace(boolean value) throws LockedException {
		return setProperty(RDF_trimWhitespace, value);
	}

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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfParserContext#RDF_language
	 */
	public RdfParser setLanguage(String value) throws LockedException {
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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfParserContext#RDF_juneauNs
	 */
	public RdfParser setJuneauNs(Namespace value) throws LockedException {
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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfParserContext#RDF_juneauBpNs
	 */
	public RdfParser setJuneauBpNs(Namespace value) throws LockedException {
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
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 * @throws LockedException If {@link #lock()} was called on this class.
	 * @see RdfParserContext#RDF_useXmlNamespaces
	 */
	public RdfParser setUseXmlNamespaces(boolean value) throws LockedException {
		return setProperty(RDF_useXmlNamespaces, value);
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
	public RdfParser setCollectionFormat(RdfCollectionFormat value) throws LockedException {
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
	public RdfParser setLooseCollections(boolean value) throws LockedException {
		return setProperty(RDF_looseCollections, value);
	}

	@Override /* Parser */
	public RdfParser setTrimStrings(boolean value) throws LockedException {
		super.setTrimStrings(value);
		return this;
	}

	@Override /* Parser */
	public RdfParser setStrict(boolean value) throws LockedException {
		super.setStrict(value);
		return this;
	}

	@Override /* Parser */
	public RdfParser setInputStreamCharset(String value) throws LockedException {
		super.setInputStreamCharset(value);
		return this;
	}

	@Override /* Parser */
	public RdfParser setFileCharset(String value) throws LockedException {
		super.setFileCharset(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeansRequireDefaultConstructor(boolean value) throws LockedException {
		super.setBeansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeansRequireSerializable(boolean value) throws LockedException {
		super.setBeansRequireSerializable(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeansRequireSettersForGetters(boolean value) throws LockedException {
		super.setBeansRequireSettersForGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeansRequireSomeProperties(boolean value) throws LockedException {
		super.setBeansRequireSomeProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanMapPutReturnsOldValue(boolean value) throws LockedException {
		super.setBeanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanConstructorVisibility(Visibility value) throws LockedException {
		super.setBeanConstructorVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanClassVisibility(Visibility value) throws LockedException {
		super.setBeanClassVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanFieldVisibility(Visibility value) throws LockedException {
		super.setBeanFieldVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setMethodVisibility(Visibility value) throws LockedException {
		super.setMethodVisibility(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setUseJavaBeanIntrospector(boolean value) throws LockedException {
		super.setUseJavaBeanIntrospector(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setUseInterfaceProxies(boolean value) throws LockedException {
		super.setUseInterfaceProxies(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setIgnoreUnknownBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setIgnoreUnknownNullBeanProperties(boolean value) throws LockedException {
		super.setIgnoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setIgnorePropertiesWithoutSetters(boolean value) throws LockedException {
		super.setIgnorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setIgnoreInvocationExceptionsOnGetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setIgnoreInvocationExceptionsOnSetters(boolean value) throws LockedException {
		super.setIgnoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setSortProperties(boolean value) throws LockedException {
		super.setSortProperties(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setNotBeanPackages(String...values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setNotBeanPackages(Collection<String> values) throws LockedException {
		super.setNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addNotBeanPackages(String...values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addNotBeanPackages(Collection<String> values) throws LockedException {
		super.addNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeNotBeanPackages(String...values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeNotBeanPackages(Collection<String> values) throws LockedException {
		super.removeNotBeanPackages(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setNotBeanClasses(Class<?>...values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.setNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addNotBeanClasses(Class<?>...values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.addNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeNotBeanClasses(Class<?>...values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeNotBeanClasses(Collection<Class<?>> values) throws LockedException {
		super.removeNotBeanClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanFilters(Class<?>...values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.setBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addBeanFilters(Class<?>...values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.addBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeBeanFilters(Class<?>...values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeBeanFilters(Collection<Class<?>> values) throws LockedException {
		super.removeBeanFilters(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setPojoSwaps(Class<?>...values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.setPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addPojoSwaps(Class<?>...values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addPojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.addPojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removePojoSwaps(Class<?>...values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removePojoSwaps(Collection<Class<?>> values) throws LockedException {
		super.removePojoSwaps(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setImplClasses(Map<Class<?>,Class<?>> values) throws LockedException {
		super.setImplClasses(values);
		return this;
	}

	@Override /* CoreApi */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanDictionary(Class<?>...values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.setBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addToBeanDictionary(Class<?>...values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addToBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.addToBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeFromBeanDictionary(Class<?>...values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeFromBeanDictionary(Collection<Class<?>> values) throws LockedException {
		super.removeFromBeanDictionary(values);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setBeanTypePropertyName(String value) throws LockedException {
		super.setBeanTypePropertyName(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setDefaultParser(Class<?> value) throws LockedException {
		super.setDefaultParser(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setLocale(Locale value) throws LockedException {
		super.setLocale(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setTimeZone(TimeZone value) throws LockedException {
		super.setTimeZone(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setMediaType(MediaType value) throws LockedException {
		super.setMediaType(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setDebug(boolean value) throws LockedException {
		super.setDebug(value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setProperty(String name, Object value) throws LockedException {
		super.setProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addToProperty(String name, Object value) throws LockedException {
		super.addToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser putToProperty(String name, Object key, Object value) throws LockedException {
		super.putToProperty(name, key, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser putToProperty(String name, Object value) throws LockedException {
		super.putToProperty(name, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser removeFromProperty(String name, Object value) throws LockedException {
		super.removeFromProperty(name, value);
		return this;
	}

	
	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* CoreApi */
	public RdfParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public RdfParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public RdfParser clone() {
		try {
			RdfParser c = (RdfParser)super.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
