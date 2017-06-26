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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.*;

/**
 * Parses RDF into POJOs.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#ParserConfigurableProperties">Configurable Properties</a>
 * for the entire list of configurable properties.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for language-specific parsers:
 * <ul class='spaced-list'>
 * 	<li>{@link RdfParser.Xml} - RDF/XML and RDF/XML-ABBREV.
 * 	<li>{@link RdfParser.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfParser.Turtle} - TURTLE.
 * 	<li>{@link RdfParser.N3} - N3.
 * </ul>
 *
 * <h5 class='section'>Additional information:</h5>
 * <p>
 * See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
 */
@Consumes(value="text/xml+rdf")
public class RdfParser extends ReaderParser {

	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new RdfParser(PropertyStore.create());

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new Turtle(PropertyStore.create());

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new NTriple(PropertyStore.create());

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new N3(PropertyStore.create());


	/** Consumes RDF/XML input */
	@Consumes("text/xml+rdf")
	public static class Xml extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_RDF_XML);
		}
	}

	/** Consumes N-Triple input */
	@Consumes(value="text/n-triple")
	public static class NTriple extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_NTRIPLE);
		}
	}

	/** Consumes Turtle input */
	@Consumes(value="text/turtle")
	public static class Turtle extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_TURTLE);
		}
	}

	/** Consumes N3 input */
	@Consumes(value="text/n3")
	public static class N3 extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public N3(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_N3);
		}
	}


	private final RdfParserContext ctx;

	/**
	 * Constructor.
	 * 
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public RdfParser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(RdfParserContext.class);
	}

	@Override /* CoreObject */
	public RdfParserBuilder builder() {
		return new RdfParserBuilder(propertyStore);
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
			if (type.isArray() || type.isArgs())
				c = new ArrayList();
			else
				c = (
					type.canCreateNewInstance(session.getOuter()) 
					? (Collection<?>)type.newInstance(session.getOuter()) 
					: new ObjectList(session)
				);

			int argIndex = 0;
			for (Resource resource : roots)
				c.add(parseAnything(s, type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), resource, 
					session.getOuter(), null));

			if (type.isArray() || type.isArgs())
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
	private static List<Resource> getRoots(RdfParserSession session, Model m) {
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
			rbm.getBeanUriProperty().set(m, null, r2.getURI());
		for (StmtIterator i = r2.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
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
					pMeta.add(m, key, value);
				} else {
					Object value = parseAnything(session, cm, o, m.getBean(false), pMeta);
					setName(cm, value, key);
					pMeta.set(m, key, value);
				}
			} else if (! (p.equals(session.getRootProperty()) || p.equals(session.getTypeProperty()))) {
				session.onUnknownProperty(key, m, -1, -1);
			}
			session.setCurrentProperty(null);
		}
		return m;
	}

	private static boolean isMultiValuedCollections(RdfParserSession session, BeanPropertyMeta pMeta) {
		if (pMeta != null && pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() != RdfCollectionFormat.DEFAULT)
			return pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
		return session.getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T parseAnything(RdfParserSession session, ClassMeta<T> eType, RDFNode n, Object outer, 
			BeanPropertyMeta pMeta) throws Exception {

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
					parseIntoCollection(session, r.as(Seq.class), (Collection)o, sType, pMeta);
				} else if (isBag(session, r)) {
					o = new ObjectList(session);
					parseIntoCollection(session, r.as(Bag.class), (Collection)o, sType, pMeta);
				} else if (r.canAs(RDFList.class)) {
					o = new ObjectList(session);
					parseIntoCollection(session, r.as(RDFList.class), (Collection)o, sType, pMeta);
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
		} else if (sType.isCollectionOrArray() || sType.isArgs()) {
			if (sType.isArray() || sType.isArgs())
				o = new ArrayList();
			else
				o = (sType.canCreateNewInstance(outer) ? (Collection<?>)sType.newInstance(outer) : new ObjectList(session));
			Resource r = n.asResource();
			if (session.wasAlreadyProcessed(r))
				return null;
			if (isSeq(session, r)) {
				parseIntoCollection(session, r.as(Seq.class), (Collection)o, sType, pMeta);
			} else if (isBag(session, r)) {
				parseIntoCollection(session, r.as(Bag.class), (Collection)o, sType, pMeta);
			} else if (r.canAs(RDFList.class)) {
				parseIntoCollection(session, r.as(RDFList.class), (Collection)o, sType, pMeta);
			} else {
				throw new ParseException("Unrecognized node type ''{0}'' for collection", n);
			}
			if (sType.isArray() || sType.isArgs())
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
			if (m.containsKey(session.getBeanTypePropertyName(eType)))
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

	private static boolean isSeq(RdfParserSession session, RDFNode n) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(session.getRdfTypeProperty());
			if (st != null)
				return RDF_SEQ.equals(st.getResource().getURI());
		}
		return false;
	}

	private static boolean isBag(RdfParserSession session, RDFNode n) {
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

	private <K,V> Map<K,V> parseIntoMap(RdfParserSession session, Resource r, Map<K,V> m, ClassMeta<K> keyType, 
			ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {
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
		return m;
	}

	@SuppressWarnings("unchecked")
	private <E> Collection<E> parseIntoCollection(RdfParserSession session, Container c, Collection<E> l, 
			ClassMeta<?> type, BeanPropertyMeta pMeta) throws Exception {
		int argIndex = 0;
		for (NodeIterator ni = c.iterator(); ni.hasNext();) {
			E e = (E)parseAnything(session, type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}

	@SuppressWarnings("unchecked")
	private <E> Collection<E> parseIntoCollection(RdfParserSession session, RDFList list, Collection<E> l, 
			ClassMeta<?> type, BeanPropertyMeta pMeta) throws Exception {
		int argIndex = 0;
		for (ExtendedIterator<RDFNode> ni = list.iterator(); ni.hasNext();) {
			E e = (E)parseAnything(session, type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public RdfParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, 
			TimeZone timeZone, MediaType mediaType) {
		return new RdfParserSession(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}
}
