/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.jena;

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.jena.Constants.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.*;

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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RdfParserSession extends ReaderParserSession {
	/**
	 * Builder class.
	 */
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

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public RdfParserSession build() {
			return new RdfParserSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(RdfParser ctx) {
		return new Builder(ctx);
	}

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
			ctx.getJenaSettings().forEach((k, v) -> rdfReader.setProperty(k, v));
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
		var s = o.toString();
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
		var l = new LinkedList<Resource>();

		// First try to find the root using the "http://www.apache.org/juneau/root" property.
		var root = m.createProperty(getJuneauNs().getUri(), RDF_juneauNs_ROOT);
		for (ResIterator i = m.listResourcesWithProperty(root); i.hasNext();)
			l.add(i.next());

		if (! l.isEmpty())
			return l;

		// Otherwise, we need to find all resources that aren't objects.
		// We want to explicitly ignore statements where the subject
		// and object are the same node.
		var objects = new HashSet<RDFNode>();
		for (StmtIterator i = m.listStatements(); i.hasNext();) {
			var st = i.next();
			var subject = st.getSubject();
			var object = st.getObject();
			if (object.isResource() && ! object.equals(subject))
				objects.add(object);
		}
		for (ResIterator i = m.listSubjects(); i.hasNext();) {
			var r = i.next();
			if (! objects.contains(r))
				l.add(r);
		}
		return l;
	}

	private Object getValue(RDFNode n, Object outer) throws ParseException {
		if (n.isLiteral())
			return n.asLiteral().getValue();
		if (n.isResource()) {
			var st = n.asResource().getProperty(pValue);
			if (nn(st)) {
				n = st.getObject();
				if (n.isLiteral())
					return n.asLiteral().getValue();
				return parseAnything(object(), st.getObject(), outer, null);
			}
		}
		throw new ParseException(this, "Unknown value type for node ''{0}''", n);
	}

	private boolean isBag(RDFNode n) {
		if (n.isResource()) {
			var st = n.asResource().getProperty(pRdfType);
			if (nn(st))
				return RDF_BAG.equals(st.getResource().getURI());
		}
		return false;
	}

	private boolean isMultiValuedCollections(BeanPropertyMeta pMeta) {
		var bpRdf = (pMeta == null ? RdfBeanPropertyMeta.DEFAULT : getRdfBeanPropertyMeta(pMeta));

		if (bpRdf.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
			return bpRdf.getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;

		return getCollectionFormat() == RdfCollectionFormat.MULTI_VALUED;
	}

	private boolean isSeq(RDFNode n) {
		if (n.isResource()) {
			var st = n.asResource().getProperty(pRdfType);
			if (nn(st))
				return RDF_SEQ.equals(st.getResource().getURI());
		}
		return false;
	}

	private <T> T parseAnything(ClassMeta<?> eType, RDFNode n, Object outer, BeanPropertyMeta pMeta) throws ParseException {

		if (eType == null)
			eType = object();
		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)opt(parseAnything(eType.getElementType(), n, outer, pMeta));

		setCurrentClass(sType);

		if (! sType.canCreateNewInstance(outer)) {
			if (n.isResource()) {
				var st = n.asResource().getProperty(pType);
				if (nn(st)) {
					var c = st.getLiteral().getString();
					var tcm = getClassMeta(c, pMeta, eType);
					if (nn(tcm))
						sType = eType = tcm;
				}
			}
		}

		Object o = null;
		if (n.isResource() && nn(n.asResource().getURI()) && n.asResource().getURI().equals(RDF_NIL)) {
			// Do nothing.  Leave o == null.
		} else if (sType.isObject()) {
			if (n.isLiteral()) {
				o = n.asLiteral().getValue();
				if (o instanceof String) {
					o = decodeString(o);
				}
			} else if (n.isResource()) {
				var r = n.asResource();
				if (! urisVisited.add(r))
					o = r.getURI();
				else if (nn(r.getProperty(pValue))) {
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
					var uri = r.getURI();
					if (nn(uri) && ! r.listProperties().hasNext()) {
						o = r.getURI();
					} else {
						var m2 = new JsonMap(this);
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
			var r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			var m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
			o = parseIntoMap(r, m, eType.getKeyType(), eType.getValueType(), pMeta);
		} else if (sType.isCollectionOrArray() || sType.isArgs()) {
			if (sType.isArray() || sType.isArgs())
				o = list();
			else
				o = (sType.canCreateNewInstance(outer) ? (Collection<?>)sType.newInstance(outer) : new JsonList(this));
			var r = n.asResource();
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
		} else if (nn(builder)) {
			var r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			var bm = toBeanMap(builder.create(this, eType));
			o = builder.build(this, parseIntoBeanMap(r, bm).getBean(), eType);
		} else if (sType.canCreateNewBean(outer)) {
			var r = n.asResource();
			if (! urisVisited.add(r))
				return null;
			var bm = newBeanMap(outer, sType.getInnerClass());
			o = parseIntoBeanMap(r, bm).getBean();
		} else if (sType.isUri() && n.isResource()) {
			o = sType.newInstanceFromString(outer, decodeString(n.asResource().getURI()));
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, decodeString(getValue(n, outer)));
		} else if (n.isResource()) {
			var r = n.asResource();
			var m = newGenericMap(sType);
			parseIntoMap(r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (m.containsKey(getBeanTypePropertyName(eType)))
				o = cast((JsonMap)m, pMeta, eType);
			else if (nn(sType.getProxyInvocationHandler()))
				o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
			else
				throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		} else {
			throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	private <T> BeanMap<T> parseIntoBeanMap(Resource r2, BeanMap<T> m) throws ParseException {
		var bm = m.getMeta();
		var rbm = getRdfBeanMeta(bm);
		if (rbm.hasBeanUri() && nn(r2.getURI()))
			rbm.getBeanUriProperty().set(m, null, r2.getURI());
		for (StmtIterator i = r2.listProperties(); i.hasNext();) {
			var st = i.next();
			var p = st.getPredicate();
			var key = decodeString(p.getLocalName());
			var pMeta = m.getPropertyMeta(key);
			setCurrentProperty(pMeta);
			if (nn(pMeta)) {
				var o = st.getObject();
				var cm = pMeta.getClassMeta();
				if (cm.isCollectionOrArray() && isMultiValuedCollections(pMeta)) {
					var et = cm.getElementType();
					var value = parseAnything(et, o, m.getBean(false), pMeta);
					setName(et, value, key);
					try {
						pMeta.add(m, key, value);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(pMeta, e);
						throw e;
					}
				} else {
					var value = parseAnything(cm, o, m.getBean(false), pMeta);
					setName(cm, value, key);
					try {
						pMeta.set(m, key, value);
					} catch (BeanRuntimeException e) {
						onBeanSetterException(pMeta, e);
						throw e;
					}
				}
			} else if (! (p.equals(pRoot) || p.equals(pType))) {
				var o = st.getObject();
				var value = parseAnything(object(), o, m.getBean(false), null);
				onUnknownProperty(key, m, value);
			}
			setCurrentProperty(null);
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

	private <K,V> Map<K,V> parseIntoMap(Resource r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws ParseException {
		// Add URI as "uri" to generic maps.
		if (nn(r.getURI())) {
			var uri = convertAttrToType(m, "uri", keyType);
			var value = convertAttrToType(m, r.getURI(), valueType);
			m.put(uri, value);
		}
		for (StmtIterator i = r.listProperties(); i.hasNext();) {
			var st = i.next();
			var p = st.getPredicate();
			var key = p.getLocalName();
			if (! (key.equals("root") && p.getURI().equals(getJuneauNs().getUri()))) {
				key = decodeString(key);
				var o = st.getObject();
				var key2 = convertAttrToType(m, key, keyType);
				V value = parseAnything(valueType, o, m, pMeta);
				setName(valueType, value, key);
				m.put(key2, value);
			}

		}
		return m;
	}

	@SuppressWarnings("resource")
	@Override /* Overridden from ReaderParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {

		var r = rdfReader;
		r.read(model, pipe.getBufferedReader(), null);

		var roots = getRoots(model);

		// Special case where we're parsing a loose collection of resources.
		if (isLooseCollections() && type.isCollectionOrArray()) {
			Collection c = null;
			if (type.isArray() || type.isArgs())
				c = list();
			else
				c = (type.canCreateNewInstance(getOuter()) ? (Collection<?>)type.newInstance(getOuter()) : new JsonList(this));

			var argIndex = new AtomicInteger(0);
			var c2 = c;
			roots.forEach(x -> c2.add(parseAnything(type.isArgs() ? type.getArg(argIndex.getAndIncrement()) : type.getElementType(), x, getOuter(), null)));

			if (type.isArray() || type.isArgs())
				return (T)toArray(type, c);
			return (T)c;
		}

		if (roots.isEmpty())
			return type.isOptional() ? (T)opte() : null;

		if (roots.size() > 1)
			throw new ParseException(this, "Too many root nodes found in model:  {0}", roots.size());
		var resource = roots.get(0);

		return parseAnything(type, resource, getOuter(), null);
	}

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see RdfParser.Builder#collectionFormat(RdfCollectionFormat)
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() { return ctx.getCollectionFormat(); }

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() { return ctx.getJenaSettings(); }

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see RdfParser.Builder#juneauBpNs(Namespace)
	 * @return
	 * 	Default XML namespace for bean properties.
	 */
	protected final Namespace getJuneauBpNs() { return ctx.getJuneauBpNs(); }

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see RdfParser.Builder#juneauNs(Namespace)
	 * @return
	 * 	XML namespace for Juneau properties.
	 */
	protected final Namespace getJuneauNs() { return ctx.getJuneauNs(); }

	/**
	 * RDF language.
	 *
	 * @see RdfParser.Builder#language(String)
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() { return ctx.getLanguage(); }

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
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return ctx.getRdfClassMeta(cm);
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

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see RdfParser.Builder#looseCollections()
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() { return ctx.isLooseCollections(); }

	/**
	 * Trim whitespace from text elements.
	 *
	 * @see RdfParser.Builder#trimWhitespace()
	 * @return
	 * 	<jk>true</jk> if whitespace in text elements will be automatically trimmed.
	 */
	protected final boolean isTrimWhitespace() { return ctx.isTrimWhitespace(); }
}