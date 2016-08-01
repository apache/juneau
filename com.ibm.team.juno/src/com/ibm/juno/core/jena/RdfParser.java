/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import static com.ibm.juno.core.jena.Constants.*;
import static com.ibm.juno.core.jena.RdfProperties.*;
import static com.ibm.juno.core.utils.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

/**
 * Parses RDF into POJOs.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#ParserConfigurableProperties'>Configurable Properties</a>
 * 		for the entire list of configurable properties.
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for language-specific parsers:
 * <ul>
 * 	<li>{@link RdfParser.Xml} - RDF/XML and RDF/XML-ABBREV.
 * 	<li>{@link RdfParser.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfParser.Turtle} - TURTLE.
 * 	<li>{@link RdfParser.N3} - N3.
 * </ul>
 *
 *
 * <h6 class='topic'>Additional Information</h6>
 * <p>
 * 	See <a class='doclink' href='package-summary.html#TOC'>RDF Overview</a> for an overview of RDF support in Juno.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
			setProperty(RDF_language, LANG_RDF_XML);
		}
	}

	/** Consumes N-Triple input */
	@Consumes(value="text/n-triple")
	public static class NTriple extends RdfParser {
		/** Constructor */
		public NTriple() {
			setProperty(RDF_language, LANG_NTRIPLE);
		}
	}

	/** Consumes Turtle input */
	@Consumes(value="text/turtle")
	public static class Turtle extends RdfParser {
		/** Constructor */
		public Turtle() {
			setProperty(RDF_language, LANG_TURTLE);
		}
	}

	/** Consumes N3 input */
	@Consumes(value="text/n3")
	public static class N3 extends RdfParser {
		/** Constructor */
		public N3() {
			setProperty(RDF_language, LANG_N3);
		}
	}


	/** Jena parser properties currently set on this parser. */
	protected transient RdfParserProperties rpp = new RdfParserProperties();


	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override /* ReaderParser */
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {

		in = IOUtils.getBufferedReader(in, estimatedSize);

		RdfParserContext rctx = (RdfParserContext)ctx;

		type = ctx.getBeanContext().normalizeClassMeta(type);

		Model model = rctx.getModel();
		RDFReader r = rctx.getRdfReader();
		r.read(model, in, null);
		BeanContext bc = ctx.getBeanContext();

		List<Resource> roots = getRoots(model, rctx);

		try {
			// Special case where we're parsing a loose collection of resources.
			if (rctx.isLooseCollection() && (type.isCollection() || type.isArray())) {
				Collection c = null;
				if (type.isArray())
					c = new ArrayList();
				else
					c = (type.canCreateNewInstance(ctx.getOuter()) ? (Collection<?>)type.newInstance(ctx.getOuter()) : new ObjectList(bc));
				for (Resource resource : roots)
					c.add(parseAnything(type.getElementType(), resource, null, rctx, ctx.getOuter(), null));

				if (type.isArray())
					return (T)bc.toArray(type, c);
				return (T)c;
			}

			if (roots.isEmpty())
				return null;
			if (roots.size() > 1)
				throw new ParseException("Too many root nodes found in model:  {0}", roots.size());
			Resource resource = roots.get(0);

			return parseAnything(type, resource, null, rctx, ctx.getOuter(), null);

		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	/*
	 * Finds the roots in the model using either the "root" property to identify it,
	 * 	or by resorting to scanning the model for all nodes with no incoming predicates.
	 */
	private List<Resource> getRoots(Model m, RdfParserContext ctx) {
		List<Resource> l = new LinkedList<Resource>();

		// First try to find the root using the "http://www.ibm.com/juno/root" property.
		Property root = m.createProperty(ctx.getJunoNsUri(), RDF_junoNs_ROOT);
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

	private <T> BeanMap<T> parseIntoBeanMap(Resource r2, BeanMap<T> m, RdfParserContext ctx) throws ParseException {
		BeanMeta<T> bm = m.getMeta();
		if (bm.hasBeanUriProperty() && r2.getURI() != null)
			m.putBeanUri(r2.getURI());
		Property subTypeIdProperty = null;
		BeanPropertyMeta<T> stp = bm.getSubTypeIdProperty();
		if (stp != null) {
			subTypeIdProperty = ctx.getProperty(stp.getName());
			Statement st = r2.getProperty(subTypeIdProperty);
			if (st == null)
				throw new ParseException("Could not find subtype ID property for bean of type ''{0}''", m.getClassMeta());
			String subTypeId = st.getLiteral().getString();
			stp.set(m, subTypeId);
		}
		for (StmtIterator i = r2.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
			if (p.equals(subTypeIdProperty))
				continue;
			String key = ctx.decodeString(p.getLocalName());
			BeanPropertyMeta<T> pMeta = m.getPropertyMeta(key);
			if (pMeta != null) {
				RDFNode o = st.getObject();
				ClassMeta<?> cm = pMeta.getClassMeta();
				if ((cm.isArray() || cm.isCollection()) && isMultiValuedCollections(ctx, pMeta)) {
					Object val = parseAnything(cm.getElementType(), o, pMeta, ctx, m.getBean(false), key);
					pMeta.add(m, val);
				} else {
					Object val = parseAnything(cm, o, pMeta, ctx, m.getBean(false), key);
					pMeta.set(m, val);
				}
			} else if (! (p.equals(ctx.getRootProperty()) || p.equals(ctx.getClassProperty()) || p.equals(subTypeIdProperty))) {
				if (bm.isSubTyped()) {
					RDFNode o = st.getObject();
					Object val = parseAnything(object(), o, null, ctx, m.getBean(false), key);
					m.put(key, val);
				} else {
					onUnknownProperty(ctx, key, m, -1, -1);
				}
			}
		}
		return m;
	}

	private boolean isMultiValuedCollections(RdfParserContext ctx, BeanPropertyMeta<?> pMeta) {
		if (pMeta != null && pMeta.getRdfMeta().getCollectionFormat() != RdfCollectionFormat.DEFAULT)
			return pMeta.getRdfMeta().getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
		return ctx.getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T parseAnything(ClassMeta<T> nt, RDFNode n, BeanPropertyMeta<?> p, RdfParserContext ctx, Object outer, Object name) throws ParseException {

		BeanContext bc = ctx.getBeanContext();

		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoFilter<T,Object> filter = (PojoFilter<T,Object>)nt.getPojoFilter();
		ClassMeta<?> ft = nt.getFilteredClassMeta();

		if (! ft.canCreateNewInstance(outer)) {
			if (n.isResource()) {
				Statement st = n.asResource().getProperty(ctx.getClassProperty());
				if (st != null) {
 					String c = st.getLiteral().getString();
					ft = nt = (ClassMeta<T>)bc.getClassMetaFromString(c);
				}
			}
		}

		try {

			Object o = null;
			if (n.isResource() && n.asResource().getURI() != null && n.asResource().getURI().equals(RDF_NIL)) {
				// Do nothing.  Leave o == null.
			} else if (ft.isObject()) {
				if (n.isLiteral()) {
					o = n.asLiteral().getValue();
					if (o instanceof String) {
						o = ctx.decodeString(o);
					}
				}
				else if (n.isResource()) {
					Resource r = n.asResource();
					if (ctx.wasAlreadyProcessed(r))
						o = r.getURI();
					else if (r.getProperty(ctx.getValueProperty()) != null) {
						o = parseAnything(object(), n.asResource().getProperty(ctx.getValueProperty()).getObject(), null, ctx, outer, null);
					} else if (isSeq(r, ctx)) {
						o = new ObjectList(bc);
						parseIntoCollection(r.as(Seq.class), (Collection)o, ft.getElementType(), ctx);
					} else if (isBag(r, ctx)) {
						o = new ObjectList(bc);
						parseIntoCollection(r.as(Bag.class), (Collection)o, ft.getElementType(), ctx);
					} else if (r.canAs(RDFList.class)) {
						o = new ObjectList(bc);
						parseIntoCollection(r.as(RDFList.class), (Collection)o, ft.getElementType(), ctx);
					} else {
						// If it has a URI and no child properties, we interpret this as an
						// external resource, and convert it to just a URL.
						String uri = r.getURI();
						if (uri != null && ! r.listProperties().hasNext()) {
							o = r.getURI();
						} else {
							o = new ObjectMap(bc);
							parseIntoMap(r, (Map)o, null, null, ctx);
						}
					}
				} else {
					throw new ParseException("Unrecognized node type ''{0}'' for object", n);
				}
			} else if (ft.isBoolean()) {
				o = bc.convertToType(getValue(n, ctx, outer), boolean.class);
			} else if (ft.isCharSequence()) {
				o = ctx.decodeString(getValue(n, ctx, outer));
			} else if (ft.isChar()) {
				o = ctx.decodeString(getValue(n, ctx, outer)).charAt(0);
			} else if (ft.isNumber()) {
				o = parseNumber(getValue(n, ctx, outer).toString(), (Class<? extends Number>)ft.getInnerClass());
			} else if (ft.isMap()) {
				Resource r = n.asResource();
				if (ctx.wasAlreadyProcessed(r))
					return null;
				Map m = (ft.canCreateNewInstance(outer) ? (Map)ft.newInstance(outer) : new ObjectMap(bc));
				o = parseIntoMap(r, m, nt.getKeyType(), nt.getValueType(), ctx);
			} else if (ft.isCollection() || ft.isArray()) {
				if (ft.isArray())
					o = new ArrayList();
				else
					o = (ft.canCreateNewInstance(outer) ? (Collection<?>)ft.newInstance(outer) : new ObjectList(bc));
				Resource r = n.asResource();
				if (ctx.wasAlreadyProcessed(r))
					return null;
				if (isSeq(r, ctx)) {
					parseIntoCollection(r.as(Seq.class), (Collection)o, ft.getElementType(), ctx);
				} else if (isBag(r, ctx)) {
					parseIntoCollection(r.as(Bag.class), (Collection)o, ft.getElementType(), ctx);
				} else if (r.canAs(RDFList.class)) {
					parseIntoCollection(r.as(RDFList.class), (Collection)o, ft.getElementType(), ctx);
				} else {
					throw new ParseException("Unrecognized node type ''{0}'' for collection", n);
				}
				if (ft.isArray())
					o = bc.toArray(ft, (Collection)o);
			} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
				Resource r = n.asResource();
				if (ctx.wasAlreadyProcessed(r))
					return null;
				Map m = new ObjectMap(bc);
				parseIntoMap(r, m, nt.getKeyType(), nt.getValueType(), ctx);
				o = ft.newInstanceFromObjectMap(outer, (ObjectMap)m);
			} else if (ft.canCreateNewBean(outer)) {
				Resource r = n.asResource();
				if (ctx.wasAlreadyProcessed(r))
					return null;
				BeanMap<?> bm = bc.newBeanMap(outer, ft.getInnerClass());
				o = parseIntoBeanMap(r, bm, ctx).getBean();
			} else if (ft.isUri() && n.isResource()) {
				o = ft.newInstanceFromString(outer, ctx.decodeString(n.asResource().getURI()));
			} else if (ft.canCreateNewInstanceFromString(outer)) {
				o = ft.newInstanceFromString(outer, ctx.decodeString(getValue(n, ctx, outer)));
			} else {
				throw new ParseException("Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
			}

			if (filter != null && o != null)
				o = filter.unfilter(o, nt);

			if (outer != null)
				setParent(nt, o, outer);

			if (name != null)
				setName(nt, o, name);

			return (T)o;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			if (p == null)
				throw new ParseException("Error occurred trying to parse into class ''{0}''", ft).initCause(e);
			throw new ParseException("Error occurred trying to parse value for bean property ''{0}'' on class ''{1}''",
				p.getName(), p.getBeanMeta().getClassMeta()
			).initCause(e);
		}
	}

	private boolean isSeq(RDFNode n, RdfParserContext ctx) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(ctx.getTypeProperty());
			if (st != null)
				return RDF_SEQ.equals(st.getResource().getURI());
		}
		return false;
	}

	private boolean isBag(RDFNode n, RdfParserContext ctx) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(ctx.getTypeProperty());
			if (st != null)
				return RDF_BAG.equals(st.getResource().getURI());
		}
		return false;
	}

	private Object getValue(RDFNode n, RdfParserContext ctx, Object outer) throws ParseException {
		if (n.isLiteral())
			return n.asLiteral().getValue();
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(ctx.getValueProperty());
			if (st != null) {
				n = st.getObject();
				if (n.isLiteral())
					return n.asLiteral().getValue();
				return parseAnything(object(), st.getObject(), null, ctx, outer, null);
			}
		}
		throw new ParseException("Unknown value type for node ''{0}''", n);
	}

	private <K,V> Map<K,V> parseIntoMap(Resource r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, RdfParserContext ctx) throws ParseException {
		// Add URI as "uri" to generic maps.
		if (r.getURI() != null) {
			K uri = convertAttrToType(m, "uri", keyType);
			V value = convertAttrToType(m, r.getURI(), valueType);
			m.put(uri, value);
		}
		for (StmtIterator i = r.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
			String key = p.getLocalName();
			if (! (key.equals("root") && p.getURI().equals(ctx.getJunoNsUri()))) {
				key = ctx.decodeString(key);
				RDFNode o = st.getObject();
				K key2 = convertAttrToType(m, key, keyType);
				V value = parseAnything(valueType, o, null, ctx, m, key);
				m.put(key2, value);
			}

		}
		// TODO Auto-generated method stub
		return m;
	}

	private <E> Collection<E> parseIntoCollection(Container c, Collection<E> l, ClassMeta<E> et, RdfParserContext ctx) throws ParseException {
		for (NodeIterator ni = c.iterator(); ni.hasNext();) {
			E e = parseAnything(et, ni.next(), null, ctx, l, null);
			l.add(e);
		}
		return l;
	}

	private <E> Collection<E> parseIntoCollection(RDFList list, Collection<E> l, ClassMeta<E> et, RdfParserContext ctx) throws ParseException {
		for (ExtendedIterator<RDFNode> ni = list.iterator(); ni.hasNext();) {
			E e = parseAnything(et, ni.next(), null, ctx, l, null);
			l.add(e);
		}
		return l;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public RdfParserContext createContext(ObjectMap op, Method javaMethod, Object outer) {
		return new RdfParserContext(getBeanContext(), pp, rpp, op, javaMethod, outer);
	}

	@Override /* CoreApi */
	public RdfParser setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! rpp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public RdfParser addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> RdfParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

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
			c.rpp = rpp.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
