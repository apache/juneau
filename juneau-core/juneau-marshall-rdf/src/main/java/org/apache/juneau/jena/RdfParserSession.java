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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.jena.Constants.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link RdfParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RdfParserSession extends ReaderParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(RdfParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends ReaderParserSession.Builder {

		RdfParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(RdfParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public RdfParserSession build() {
			return new RdfParserSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ReaderParserSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final RdfParser ctx;
	private final Property pRoot, pValue, pType, pRdfType;
	private final Model model;
	private final RDFReader rdfReader;
	private final Set<Resource> urisVisited = new HashSet<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected RdfParserSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		model = ModelFactory.createDefaultModel();
		addModelPrefix(ctx.getJuneauNs());
		addModelPrefix(ctx.getJuneauBpNs());
		pRoot = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_ROOT);
		pValue = model.createProperty(ctx.getJuneauNs().getUri(), RDF_juneauNs_VALUE);
		pType = model.createProperty(ctx.getJuneauBpNs().getUri(), RDF_juneauNs_TYPE);
		pRdfType = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		rdfReader = model.getReader(ctx.getLanguage());

		// Note: NTripleReader throws an exception if you try to set any properties on it.
		if (! ctx.getLanguage().equals(LANG_NTRIPLE))
			ctx.getJenaSettings().forEach((k,v) -> rdfReader.setProperty(k, v));
	}

	@Override /* ReaderParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {

		RDFReader r = rdfReader;
		r.read(model, pipe.getBufferedReader(), null);

		List<Resource> roots = getRoots(model);

		// Special case where we're parsing a loose collection of resources.
		if (isLooseCollections() && type.isCollectionOrArray()) {
			Collection c = null;
			if (type.isArray() || type.isArgs())
				c = list();
			else
				c = (
					type.canCreateNewInstance(getOuter())
					? (Collection<?>)type.newInstance(getOuter())
					: new JsonList(this)
				);

			AtomicInteger argIndex = new AtomicInteger(0);
			Collection c2 = c;
			roots.forEach(x -> c2.add(parseAnything(type.isArgs() ? type.getArg(argIndex.getAndIncrement()) : type.getElementType(), x, getOuter(), null)));

			if (type.isArray() || type.isArgs())
				return (T)toArray(type, c);
			return (T)c;
		}

		if (roots.isEmpty())
			return type.isOptional() ? (T)empty() : null;

		if (roots.size() > 1)
			throw new ParseException(this, "Too many root nodes found in model:  {0}", roots.size());
		Resource resource = roots.get(0);

		return parseAnything(type, resource, getOuter(), null);
	}

	private final void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/*
	 * Decodes the specified string.
	 * If {@link RdfParser#RDF_trimWhitespace} is <jk>true</jk>, the resulting string is trimmed before decoding.
	 * If {@link #isTrimStrings()} is <jk>true</jk>, the resulting string is trimmed after decoding.
	 */
	private String decodeString(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (s.isEmpty())
			return s;
		if (isTrimWhitespace())
			s = s.trim();
		s = XmlUtils.decode(s, null);
		if (isTrimStrings())
			s = s.trim();
		return s;
	}

	/*
	 * Finds the roots in the model using either the "root" property to identify it,
	 * 	or by resorting to scanning the model for all nodes with no incoming predicates.
	 */
	private List<Resource> getRoots(Model m) {
		List<Resource> l = new LinkedList<>();

		// First try to find the root using the "http://www.apache.org/juneau/root" property.
		Property root = m.createProperty(getJuneauNs().getUri(), RDF_juneauNs_ROOT);
		for (ResIterator i  = m.listResourcesWithProperty(root); i.hasNext();)
			l.add(i.next());

		if (! l.isEmpty())
			return l;

		// Otherwise, we need to find all resources that aren't objects.
		// We want to explicitly ignore statements where the subject
		// and object are the same node.
		Set<RDFNode> objects = new HashSet<>();
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

	private <T> BeanMap<T> parseIntoBeanMap(Resource r2, BeanMap<T> m) throws ParseException {
		BeanMeta<T> bm = m.getMeta();
		RdfBeanMeta rbm = getRdfBeanMeta(bm);
		if (rbm.hasBeanUri() && r2.getURI() != null)
			rbm.getBeanUriProperty().set(m, null, r2.getURI());
		for (StmtIterator i = r2.listProperties(); i.hasNext();) {
			Statement st = i.next();
			Property p = st.getPredicate();
			String key = decodeString(p.getLocalName());
			BeanPropertyMeta pMeta = m.getPropertyMeta(key);
			setCurrentProperty(pMeta);
			if (pMeta != null) {
				RDFNode o = st.getObject();
				ClassMeta<?> cm = pMeta.getClassMeta();
				if (cm.isCollectionOrArray() && isMultiValuedCollections(pMeta)) {
					ClassMeta<?> et = cm.getElementType();
					Object value = parseAnything(et, o, m.getBean(false), pMeta);
					setName(et, value, key);
					try {
						pMeta.add(m, key, value);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(pMeta, e);
						throw e;
					}
				} else {
					Object value = parseAnything(cm, o, m.getBean(false), pMeta);
					setName(cm, value, key);
					try {
						pMeta.set(m, key, value);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(pMeta, e);
						throw e;
					}
				}
			} else if (! (p.equals(pRoot) || p.equals(pType))) {
				RDFNode o = st.getObject();
				Object value = parseAnything(object(), o, m.getBean(false), null);
				onUnknownProperty(key, m, value);
			}
			setCurrentProperty(null);
		}
		return m;
	}

	private boolean isMultiValuedCollections(BeanPropertyMeta pMeta) {
		RdfBeanPropertyMeta bpRdf = (pMeta == null ? RdfBeanPropertyMeta.DEFAULT : getRdfBeanPropertyMeta(pMeta));

		if (bpRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
			return bpRdf.getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;

		return getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
	}

	private <T> T parseAnything(ClassMeta<?> eType, RDFNode n, Object outer, BeanPropertyMeta pMeta) throws ParseException {

		if (eType == null)
			eType = object();
		ObjectSwap<T,Object> swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)optional(parseAnything(eType.getElementType(), n, outer, pMeta));

		setCurrentClass(sType);

		if (! sType.canCreateNewInstance(outer)) {
			if (n.isResource()) {
				Statement st = n.asResource().getProperty(pType);
				if (st != null) {
					String c = st.getLiteral().getString();
					ClassMeta tcm = getClassMeta(c, pMeta, eType);
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
					o = decodeString(o);
				}
			}
			else if (n.isResource()) {
				Resource r = n.asResource();
				if (! urisVisited.add(r))
					o = r.getURI();
				else if (r.getProperty(pValue) != null) {
					o = parseAnything(object(), n.asResource().getProperty(pValue).getObject(), outer, null);
				} else if (isSeq(r)) {
					o = new JsonList(this);
					parseIntoCollection(r.as(Seq.class), (Collection)o, sType, pMeta);
				} else if (isBag(r)) {
					o = new JsonList(this);
					parseIntoCollection(r.as(Bag.class), (Collection)o, sType, pMeta);
				} else if (r.canAs(RDFList.class)) {
					o = new JsonList(this);
					parseIntoCollection(r.as(RDFList.class), (Collection)o, sType, pMeta);
				} else {
					// If it has a URI and no child properties, we interpret this as an
					// external resource, and convert it to just a URL.
					String uri = r.getURI();
					if (uri != null && ! r.listProperties().hasNext()) {
						o = r.getURI();
					} else {
						JsonMap m2 = new JsonMap(this);
						parseIntoMap(r, m2, null, null, pMeta);
						o = cast(m2, pMeta, eType);
					}
				}
			} else {
				throw new ParseException(this, "Unrecognized node type ''{0}'' for object", n);
			}
		} else if (sType.isBoolean()) {
			o = convertToType(getValue(n, outer), boolean.class);
		} else if (sType.isCharSequence()) {
			o = decodeString(getValue(n, outer));
		} else if (sType.isChar()) {
			o = parseCharacter(decodeString(getValue(n, outer)));
		} else if (sType.isNumber()) {
			o = parseNumber(getValue(n, outer).toString(), (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.isMap()) {
			Resource r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
			o = parseIntoMap(r, m, eType.getKeyType(), eType.getValueType(), pMeta);
		} else if (sType.isCollectionOrArray() || sType.isArgs()) {
			if (sType.isArray() || sType.isArgs())
				o = list();
			else
				o = (sType.canCreateNewInstance(outer) ? (Collection<?>)sType.newInstance(outer) : new JsonList(this));
			Resource r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			if (isSeq(r)) {
				parseIntoCollection(r.as(Seq.class), (Collection)o, sType, pMeta);
			} else if (isBag(r)) {
				parseIntoCollection(r.as(Bag.class), (Collection)o, sType, pMeta);
			} else if (r.canAs(RDFList.class)) {
				parseIntoCollection(r.as(RDFList.class), (Collection)o, sType, pMeta);
			} else {
				throw new ParseException(this, "Unrecognized node type ''{0}'' for collection", n);
			}
			if (sType.isArray() || sType.isArgs())
				o = toArray(sType, (Collection)o);
		} else if (builder != null) {
			Resource r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			BeanMap<?> bm = toBeanMap(builder.create(this, eType));
			o = builder.build(this, parseIntoBeanMap(r, bm).getBean(), eType);
		} else if (sType.canCreateNewBean(outer)) {
			Resource r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			BeanMap<?> bm = newBeanMap(outer, sType.getInnerClass());
			o = parseIntoBeanMap(r, bm).getBean();
		} else if (sType.isUri() && n.isResource()) {
			o = sType.newInstanceFromString(outer, decodeString(n.asResource().getURI()));
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, decodeString(getValue(n, outer)));
		} else if (n.isResource()) {
			Resource r = n.asResource();
			Map m = newGenericMap(sType);
			parseIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (m.containsKey(getBeanTypePropertyName(eType)))
				o = cast((JsonMap)m, pMeta, eType);
			else if (sType.getProxyInvocationHandler() != null)
				o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
			else
				throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else {
			throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private boolean isSeq(RDFNode n) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(pRdfType);
			if (st != null)
				return RDF_SEQ.equals(st.getResource().getURI());
		}
		return false;
	}

	private boolean isBag(RDFNode n) {
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(pRdfType);
			if (st != null)
				return RDF_BAG.equals(st.getResource().getURI());
		}
		return false;
	}

	private Object getValue(RDFNode n, Object outer) throws ParseException {
		if (n.isLiteral())
			return n.asLiteral().getValue();
		if (n.isResource()) {
			Statement st = n.asResource().getProperty(pValue);
			if (st != null) {
				n = st.getObject();
				if (n.isLiteral())
					return n.asLiteral().getValue();
				return parseAnything(object(), st.getObject(), outer, null);
			}
		}
		throw new ParseException(this, "Unknown value type for node ''{0}''", n);
	}

	private <K,V> Map<K,V> parseIntoMap(Resource r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws ParseException {
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
			if (! (key.equals("root") && p.getURI().equals(getJuneauNs().getUri()))) {
				key = decodeString(key);
				RDFNode o = st.getObject();
				K key2 = convertAttrToType(m, key, keyType);
				V value = parseAnything(valueType, o, m, pMeta);
				setName(valueType, value, key);
				m.put(key2, value);
			}

		}
		return m;
	}

	private <E> Collection<E> parseIntoCollection(Container c, Collection<E> l, ClassMeta<?> type, BeanPropertyMeta pMeta) throws ParseException {
		int argIndex = 0;
		for (NodeIterator ni = c.iterator(); ni.hasNext();) {
			E e = (E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}

	private <E> Collection<E> parseIntoCollection(RDFList list, Collection<E> l, ClassMeta<?> type, BeanPropertyMeta pMeta) throws ParseException {
		int argIndex = 0;
		for (ExtendedIterator<RDFNode> ni = list.iterator(); ni.hasNext();) {
			E e = (E)parseAnything(type.isArgs() ? type.getArg(argIndex++) : type.getElementType(), ni.next(), l, pMeta);
			l.add(e);
		}
		return l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Common properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see RdfParser.Builder#collectionFormat(RdfCollectionFormat)
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return ctx.getCollectionFormat();
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see RdfParser.Builder#juneauBpNs(Namespace)
	 * @return
	 * 	Default XML namespace for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return ctx.getJuneauBpNs();
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see RdfParser.Builder#juneauNs(Namespace)
	 * @return
	 * 	XML namespace for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return ctx.getJuneauNs();
	}

	/**
	 * RDF language.
	 *
	 * @see RdfParser.Builder#language(String)
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return ctx.getLanguage();
	}

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see RdfParser.Builder#looseCollections()
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() {
		return ctx.isLooseCollections();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Jena properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() {
		return ctx.getJenaSettings();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Trim whitespace from text elements.
	 *
	 * @see RdfParser.Builder#trimWhitespace()
	 * @return
	 * 	<jk>true</jk> if whitespace in text elements will be automatically trimmed.
	 */
	protected final boolean isTrimWhitespace() {
		return ctx.isTrimWhitespace();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return ctx.getRdfClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean.
	 *
	 * @param bm The bean to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		return ctx.getRdfBeanMeta(bm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getRdfBeanPropertyMeta(bpm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	protected XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getXmlBeanPropertyMeta(bpm);
	}
}
