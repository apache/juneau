/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.serializer;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.utils.ClassUtils.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.utils.log.*;

/**
 * Context object that lives for the duration of a single serialization of {@link Serializer} and its subclasses.
 * <p>
 *  	Used by serializers for the following purposes:
 * 	<ul>
 * 		<li>Keeping track of how deep it is in a model for indentation purposes.
 * 		<li>Ensuring infinite loops don't occur by setting a limit on how deep to traverse a model.
 * 		<li>Ensuring infinite loops don't occur from loops in the model (when detectRecursions is enabled.
 * 		<li>Allowing serializer properties to be overridden on method calls.
 * 	</ul>
 * </p>
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SerializerContext {

	private static JunoLogger logger = JunoLogger.getLogger(SerializerContext.class);

	private final int maxDepth, initialDepth;
	private final boolean
		debug,
		detectRecursions,
		ignoreRecursions,
		useIndentation,
		addClassAttrs,
		trimNulls,
		trimEmptyLists,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps;
	private final char quoteChar;
	private final String relativeUriBase, absolutePathUriBase;
	private final ObjectMap overrideProperties;

	/** The current indentation depth into the model. */
	public int indent;

	/** Contains the current objects in the current branch of the model. */
	private Map<Object,Object> set;

	/** Contains the current objects in the current branch of the model. */
	private LinkedList<StackElement> stack;

	/** If 'true', then we're at a leaf in the model (i.e. a String, Number, Boolean, or null). */
	private boolean isBottom;

	/** Any warnings encountered. */
	private final List<String> warnings = new LinkedList<String>();

	/** The bean context being used in this context. */
	private final BeanContext beanContext;

	/** Java method that invoked this serializer. */
	private final Method javaMethod;

	/**
	 * Create a new HashStack with the specified options.
	 *
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp The default serializer properties.
	 * @param op The override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	public SerializerContext(BeanContext beanContext, SerializerProperties sp, ObjectMap op, Method javaMethod) {
		this.beanContext = beanContext;
		this.javaMethod = javaMethod;
		if (op == null || op.isEmpty()) {
			overrideProperties = new ObjectMap();
			maxDepth = sp.maxDepth;
			initialDepth = sp.initialDepth;
			debug = sp.debug;
			detectRecursions = sp.detectRecursions;
			ignoreRecursions = sp.ignoreRecursions;
			useIndentation = sp.useIndentation;
			addClassAttrs = sp.addClassAttrs;
			trimNulls = sp.trimNulls;
			trimEmptyLists = sp.trimEmptyLists;
			trimEmptyMaps = sp.trimEmptyMaps;
			trimStrings = sp.trimStrings;
			quoteChar = sp.quoteChar;
			relativeUriBase = resolveRelativeUriBase(sp.relativeUriBase);
			absolutePathUriBase = resolveAbsolutePathUriBase(sp.absolutePathUriBase);
			sortCollections = sp.sortCollections;
			sortMaps = sp.sortMaps;
		} else {
			overrideProperties = op;
			maxDepth = op.getInt(SERIALIZER_maxDepth, sp.maxDepth);
			initialDepth = op.getInt(SERIALIZER_initialDepth, sp.initialDepth);
			debug = op.getBoolean(SERIALIZER_debug, sp.debug);
			detectRecursions = op.getBoolean(SERIALIZER_detectRecursions, sp.detectRecursions);
			ignoreRecursions = op.getBoolean(SERIALIZER_ignoreRecursions, sp.ignoreRecursions);
			useIndentation = op.getBoolean(SERIALIZER_useIndentation, sp.useIndentation);
			addClassAttrs = op.getBoolean(SERIALIZER_addClassAttrs, sp.addClassAttrs);
			trimNulls = op.getBoolean(SERIALIZER_trimNullProperties, sp.trimNulls);
			trimEmptyLists = op.getBoolean(SERIALIZER_trimEmptyLists, sp.trimEmptyLists);
			trimEmptyMaps = op.getBoolean(SERIALIZER_trimEmptyMaps, sp.trimEmptyMaps);
			trimStrings = op.getBoolean(SERIALIZER_trimStrings, sp.trimStrings);
			quoteChar = op.getString(SERIALIZER_quoteChar, ""+sp.quoteChar).charAt(0);
			relativeUriBase = resolveRelativeUriBase(op.getString(SERIALIZER_relativeUriBase, sp.relativeUriBase));
			absolutePathUriBase = resolveAbsolutePathUriBase(op.getString(SERIALIZER_absolutePathUriBase, sp.absolutePathUriBase));
			sortCollections = op.getBoolean(SERIALIZER_sortCollections, sp.sortMaps);
			sortMaps = op.getBoolean(SERIALIZER_sortMaps, sp.sortMaps);
		}

		this.indent = initialDepth;
		if (detectRecursions || debug) {
			set = new IdentityHashMap<Object,Object>();
			stack = new LinkedList<StackElement>();
		}
	}

	private String resolveRelativeUriBase(String s) {
		if (StringUtils.isEmpty(s))
			return null;
		if (s.equals("/"))
			return s;
		else if (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		return s;
	}

	private String resolveAbsolutePathUriBase(String s) {
		if (StringUtils.isEmpty(s))
			return null;
		if (StringUtils.endsWith(s, '/'))
			s = s.substring(0, s.length()-1);
		return s;
	}

	/**
	 * Returns the bean context associated with this context.
	 *
	 * @return The bean context associated with this context.
	 */
	public final BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the Java method that invoked this serializer.
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 *
	 * @return The Java method that invoked this serializer.
	*/
	public final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the runtime properties associated with this context.
	 *
	 * @return The runtime properties associated with this context.
	 */
	public final ObjectMap getProperties() {
		return overrideProperties;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_maxDepth} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_maxDepth} setting value in this context.
	 */
	public final int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_initialDepth} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_initialDepth} setting value in this context.
	 */
	public final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_debug} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_debug} setting value in this context.
	 */
	public final boolean isDebug() {
		return debug;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_detectRecursions} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_detectRecursions} setting value in this context.
	 */
	public final boolean isDetectRecursions() {
		return detectRecursions;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_ignoreRecursions} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_ignoreRecursions} setting value in this context.
	 */
	public final boolean isIgnoreRecursions() {
		return ignoreRecursions;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_useIndentation} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_useIndentation} setting value in this context.
	 */
	public final boolean isUseIndentation() {
		return useIndentation;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_addClassAttrs} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_addClassAttrs} setting value in this context.
	 */
	public final boolean isAddClassAttrs() {
		return addClassAttrs;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_quoteChar} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_quoteChar} setting value in this context.
	 */
	public final char getQuoteChar() {
		return quoteChar;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_trimNullProperties} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_trimNullProperties} setting value in this context.
	 */
	public final boolean isTrimNulls() {
		return trimNulls;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_trimEmptyLists} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_trimEmptyLists} setting value in this context.
	 */
	public final boolean isTrimEmptyLists() {
		return trimEmptyLists;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_trimEmptyMaps} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_trimEmptyMaps} setting value in this context.
	 */
	public final boolean isTrimEmptyMaps() {
		return trimEmptyMaps;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_trimStrings} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_trimStrings} setting value in this context.
	 */
	public final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_sortCollections} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_sortCollections} setting value in this context.
	 */
	public final boolean isSortCollections() {
		return sortCollections;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_sortMaps} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_sortMaps} setting value in this context.
	 */
	public final boolean isSortMaps() {
		return sortMaps;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_relativeUriBase} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_relativeUriBase} setting value in this context.
	 */
	public final String getRelativeUriBase() {
		return relativeUriBase;
	}

	/**
	 * Returns the {@link SerializerProperties#SERIALIZER_absolutePathUriBase} setting value in this context.
	 *
	 * @return The {@link SerializerProperties#SERIALIZER_absolutePathUriBase} setting value in this context.
	 */
	public final String getAbsolutePathUriBase() {
		return absolutePathUriBase;
	}

	/**
	 * Push the specified object onto the stack.
	 *
	 * @param attrName The attribute name.
	 * @param o The current object being serialized.
	 * @param eType The expected class type.
	 * @return The {@link ClassMeta} of the object so that <code>instanceof</code> operations
	 * 	only need to be performed once (since they can be expensive).<br>
	 * @throws SerializeException
	 */
	public ClassMeta<?> push(String attrName, Object o, ClassMeta<?> eType) throws SerializeException {
		indent++;
		isBottom = true;
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ClassMeta<?> cm = (eType != null && c == eType.getInnerClass()) ? eType : beanContext.getClassMeta(c);
		if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
			return cm;
		if (detectRecursions || debug) {
			if (stack.size() > maxDepth)
				return null;
			if (willRecurse(attrName, o, cm))
				return null;
			isBottom = false;
			stack.add(new StackElement(stack.size(), attrName, o, cm));
			if (debug)
				logger.info(getStack(false));
			set.put(o, o);
		}
		return cm;
	}

	/**
	 * Returns <jk>true</jk> if {@link SerializerProperties#SERIALIZER_detectRecursions} is enabled, and the specified
	 * 	object is already higher up in the serialization chain.
	 *
	 * @param attrName The bean property attribute name, or some other identifier.
	 * @param o The object to check for recursion.
	 * @param cm The metadata on the object class.
	 * @return <jk>true</jk> if recursion detected.
	 * @throws SerializeException
	 */
	public boolean willRecurse(String attrName, Object o, ClassMeta<?> cm) throws SerializeException {
		if (! (detectRecursions || debug))
			return false;
		if (! set.containsKey(o))
			return false;
		if (ignoreRecursions && ! debug)
			return true;

		stack.add(new StackElement(stack.size(), attrName, o, cm));
		throw new SerializeException("Recursion occurred, stack={0}", getStack(true));
	}

	/**
	 * Pop an object off the stack.
	 */
	public void pop() {
		indent--;
		if ((detectRecursions || debug) && ! isBottom)  {
			Object o = stack.removeLast().o;
			Object o2 = set.remove(o);
			if (o2 == null)
				addWarning("Couldn't remove object of type ''{0}'' on attribute ''{1}'' from object stack.", o.getClass().getName(), stack);
		}
		isBottom = false;
	}

	/**
	 * The current indentation depth.
	 *
	 * @return The current indentation depth.
	 */
	public int getIndent() {
		return indent;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional printf arguments to replace in the error message.
	 */
	public void addWarning(String msg, Object... args) {
		logger.warning(msg, args);
		msg = args.length == 0 ? msg : MessageFormat.format(msg, args);
		warnings.add((warnings.size() + 1) + ": " + msg);
	}

	/**
	 * Specialized warning when an exception is thrown while executing a bean getter.
	 *
	 * @param p The bean map entry representing the bean property.
	 * @param t The throwable that the bean getter threw.
	 */
	public void addBeanGetterWarning(BeanPropertyMeta<?> p, Throwable t) {
		String prefix = (debug ? getStack(false) + ": " : "");
		addWarning("{0}Could not call getValue() on property ''{1}'' of class ''{2}'', exception = {3}", prefix, p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage());
	}

	/**
	 * Trims the specified string if {@link SerializerContext#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String trim(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (trimStrings)
			s = s.trim();
		return s;
	}

	/**
	 * Generalize the specified object if a filter is associated with it.
	 *
	 * @param o The object to generalize.
	 * @param type The type of object.
	 * @return The generalized object, or <jk>null</jk> if the object is <jk>null</jk>.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final Object generalize(Object o, ClassMeta<?> type) throws SerializeException {
		if (o == null)
			return null;
		PojoFilter f = (type == null || type.isObject() ? getBeanContext().getClassMeta(o.getClass()).getPojoFilter() : type.getPojoFilter());
		if (f == null)
			return o;
		return f.filter(o);
	}

	/**
	 * Returns <jk>true</jk> if the specified value should not be serialized.
	 *
	 * @param cm The class type of the object being serialized.
	 * @param attrName The bean attribute name, or <jk>null</jk> if this isn't a bean attribute.
	 * @param value The object being serialized.
	 * @return <jk>true</jk> if the specified value should not be serialized.
	 * @throws SerializeException
	 */
	public final boolean canIgnoreValue(ClassMeta<?> cm, String attrName, Object value) throws SerializeException {

		if (trimNulls && value == null)
			return true;

		if (value == null)
			return false;

		if (cm == null)
			cm = getBeanContext().object();

		if (trimEmptyLists) {
			if (cm.isArray() || (cm.isObject() && value.getClass().isArray())) {
				if (((Object[])value).length == 0)
					return true;
			}
			if (cm.isCollection() || (cm.isObject() && isParentClass(Collection.class, value.getClass()))) {
				if (((Collection<?>)value).isEmpty())
					return true;
			}
		}

		if (trimEmptyMaps) {
			if (cm.isMap() || (cm.isObject() && isParentClass(Map.class, value.getClass()))) {
				if (((Map<?,?>)value).isEmpty())
					return true;
			}
		}

		if (trimNulls && willRecurse(attrName, value, cm))
			return true;

		return false;
	}

	/**
	 * Sorts the specified map if {@link SerializerContext#isSortMaps()} returns <jk>true</jk>.
	 *
	 * @param m The map being sorted.
	 * @return A new sorted {@link TreeMap}.
	 */
	public final <K,V> Map<K,V> sort(Map<K,V> m) {
		if (sortMaps && m != null && (! m.isEmpty()) && m.keySet().iterator().next() instanceof Comparable<?>)
			return new TreeMap<K,V>(m);
		return m;
	}

	/**
	 * Sorts the specified collection if {@link SerializerContext#isSortCollections()} returns <jk>true</jk>.
	 *
	 * @param c The collection being sorted.
	 * @return A new sorted {@link TreeSet}.
	 */
	public final <E> Collection<E> sort(Collection<E> c) {
		if (sortCollections && c != null && (! c.isEmpty()) && c.iterator().next() instanceof Comparable<?>)
			return new TreeSet<E>(c);
		return c;
	}

	/**
	 * Perform cleanup on this context object if necessary.
	 *
	 * @throws SerializeException
	 */
	public void close() throws SerializeException {
		if (debug && warnings.size() > 0)
			throw new SerializeException("Warnings occurred during serialization: \n" + StringUtils.join(warnings, "\n"));
	}

	private static class StackElement {
		private int depth;
		private String name;
		private Object o;
		private ClassMeta<?> aType;

		private StackElement(int depth, String name, Object o, ClassMeta<?> aType) {
			this.depth = depth;
			this.name = name;
			this.o = o;
			this.aType = aType;
		}

		private String toString(boolean simple) {
			StringBuilder sb = new StringBuilder().append('[').append(depth).append(']');
			sb.append(StringUtils.isEmpty(name) ? "<noname>" : name).append(':');
			sb.append(aType.toString(simple));
			if (aType != aType.getFilteredClassMeta())
				sb.append('/').append(aType.getFilteredClassMeta().toString(simple));
			return sb.toString();
		}
	}

	private String getStack(boolean full) {
		StringBuilder sb = new StringBuilder();
		for (StackElement e : stack) {
			if (full) {
				sb.append("\n\t");
				for (int i = 1; i < e.depth; i++)
					sb.append("  ");
				if (e.depth > 0)
					sb.append("->");
				sb.append(e.toString(false));
			} else {
				sb.append(" > ").append(e.toString(true));
			}
		}
		return sb.toString();
	}
}
