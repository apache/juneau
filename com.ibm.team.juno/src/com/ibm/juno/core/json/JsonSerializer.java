/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import static com.ibm.juno.core.json.JsonSerializerProperties.*;
import static com.ibm.juno.core.serializer.SerializerProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;

/**
 * Serializes POJO models to JSON.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/json, text/json</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	The conversion is as follows...
 * 	<ul>
 * 		<li>Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to JSON objects.
 * 		<li>Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to JSON arrays.
 * 		<li>{@link String Strings} are converted to JSON strings.
 * 		<li>{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to JSON numbers.
 * 		<li>{@link Boolean Booleans} are converted to JSON booleans.
 * 		<li>{@code nulls} are converted to JSON nulls.
 * 		<li>{@code arrays} are converted to JSON arrays.
 * 		<li>{@code beans} are converted to JSON objects.
 * 	</ul>
 * <p>
 * 	The types above are considered "JSON-primitive" object types.  Any non-JSON-primitive object types are transformed
 * 		into JSON-primitive object types through {@link com.ibm.juno.core.filter.Filter Filters} associated through the {@link BeanContextFactory#addFilters(Class...)}
 * 		method.  Several default filters are provided for transforming Dates, Enums, Iterators, etc...
 * <p>
 * 	This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
 * 	However, custom serializers can be constructed to fine-tune behavior.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link JsonSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * 	The following direct subclasses are provided for convenience:
 * <ul>
 * 	<li>{@link Simple} - Default serializer, single quotes, simple mode.
 * 	<li>{@link SimpleReadable} - Default serializer, single quotes, simple mode, with whitespace.
 * </ul>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(someObject);
 *
 * 	<jc>// Create a custom serializer for lax syntax using single quote characters</jc>
 * 	JsonSerializer serializer = <jk>new</jk> JsonSerializer()
 * 		.setProperty(JsonSerializerProperties.<jsf>JSON_simpleMode</jsf>, <jk>true</jk>)
 * 		.setProperty(SerializerProperties.<jsf>SERIALIZER_quoteChar</jsf>, <js>'\''</js>);
 *
 * 	<jc>// Clone an existing serializer and modify it to use single-quotes</jc>
 * 	JsonSerializer serializer = JsonSerializer.<jsf>DEFAULT</jsf>.clone()
 * 		.setProperty(SerializerProperties.<jsf>SERIALIZER_quoteChar</jsf>, <js>'\''</js>);
 *
 * 	<jc>// Serialize a POJO to JSON</jc>
 * 	String json = serializer.serialize(someObject);
 * </p>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces({"application/json","text/json"})
public class JsonSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT = new JsonSerializer().lock();

	/** Default serializer, all default settings.*/
	public static final JsonSerializer DEFAULT_READABLE = new Readable().lock();

	/** Default serializer, single quotes, simple mode. */
	public static final JsonSerializer DEFAULT_LAX = new Simple().lock();

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final JsonSerializer DEFAULT_LAX_READABLE = new SimpleReadable().lock();

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static final JsonSerializer DEFAULT_LAX_READABLE_SAFE = new SimpleReadableSafe().lock();

	/** Default serializer, with whitespace. */
	public static class Readable extends JsonSerializer {
		/** Constructor */
		public Readable() {
			setProperty(JSON_useWhitespace, true);
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/** Default serializer, single quotes, simple mode. */
	@Produces(value={"application/json+simple","text/json+simple"},contentType="application/json")
	public static class Simple extends JsonSerializer {
		/** Constructor */
		public Simple() {
			setProperty(JSON_simpleMode, true);
			setProperty(SERIALIZER_quoteChar, '\'');
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends Simple {
		/** Constructor */
		public SimpleReadable() {
			setProperty(JSON_useWhitespace, true);
			setProperty(SERIALIZER_useIndentation, true);
		}
	}

	/**
	 * Default serializer, single quotes, simple mode, with whitespace and recursion detection.
	 * Note that recursion detection introduces a small performance penalty.
	 */
	public static class SimpleReadableSafe extends SimpleReadable {
		/** Constructor */
		public SimpleReadableSafe() {
			setProperty(SERIALIZER_detectRecursions, true);
		}
	}

	/** JSON serializer properties currently set on this serializer. */
	protected transient JsonSerializerProperties jsp = new JsonSerializerProperties();


	/**
	 * Workhorse method. Determines the type of object, and then calls the
	 * appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	SerializerWriter serializeAnything(JsonSerializerWriter out, Object o, ClassMeta<?> eType, JsonSerializerContext ctx, String attrName, BeanPropertyMeta pMeta) throws SerializeException {
		try {
			BeanContext bc = ctx.getBeanContext();

			if (o == null) {
				out.append("null");
				return out;
			}

			if (eType == null)
				eType = object();

			boolean addClassAttr;		// Add "_class" attribute to element?
			ClassMeta<?> aType;			// The actual type
			ClassMeta<?> gType;			// The generic type

			aType = ctx.push(attrName, o, eType);
			boolean isRecursion = aType == null;

			// Handle recursion
			if (aType == null) {
				o = null;
				aType = object();
			}

			gType = aType.getFilteredClassMeta();
			addClassAttr = (ctx.isAddClassAttrs() && ! eType.equals(aType));

			// Filter if necessary
			PojoFilter filter = aType.getPojoFilter();				// The filter
			if (filter != null) {
				o = filter.filter(o);

				// If the filter's getFilteredClass() method returns Object, we need to figure out
				// the actual type now.
				if (gType.isObject())
					gType = bc.getClassMetaForObject(o);
			}

			String wrapperAttr = gType.getJsonMeta().getWrapperAttr();
			if (wrapperAttr != null) {
				out.append('{').cr(ctx.indent).attr(wrapperAttr).append(':').s();
				ctx.indent++;
			}

			// '\0' characters are considered null.
			if (o == null || (gType.isChar() && ((Character)o).charValue() == 0))
				out.append("null");
			else if (gType.isNumber() || gType.isBoolean())
				out.append(o);
			else if (gType.hasToObjectMapMethod())
				serializeMap(out, gType.toObjectMap(o), gType, ctx);
			else if (gType.isBean())
				serializeBeanMap(out, bc.forBean(o), addClassAttr, ctx);
			else if (gType.isUri() || (pMeta != null && (pMeta.isUri() || pMeta.isBeanUri())))
				out.q().appendUri(o).q();
			else if (gType.isMap()) {
				if (o instanceof BeanMap)
					serializeBeanMap(out, (BeanMap)o, addClassAttr, ctx);
				else
					serializeMap(out, (Map)o, eType, ctx);
			}
			else if (gType.isCollection()) {
				if (addClassAttr)
					serializeCollectionMap(out, (Collection)o, gType, ctx);
				else
					serializeCollection(out, (Collection) o, eType, ctx);
			}
			else if (gType.isArray()) {
				if (addClassAttr)
					serializeCollectionMap(out, toList(gType.getInnerClass(), o), gType, ctx);
				else
					serializeCollection(out, toList(gType.getInnerClass(), o), eType, ctx);
			}
			else
				out.stringValue(o);

			if (wrapperAttr != null) {
				ctx.indent--;
				out.cr(ctx.indent-1).append('}');
			}

			if (! isRecursion)
				ctx.pop();
			return out;
		} catch (SerializeException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw e;
		} catch (Throwable e) {
			throw new SerializeException("Exception occured trying to process object of type ''{0}''", (o == null ? null : o.getClass().getName())).initCause(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonSerializerWriter out, Map m, ClassMeta<?> type, JsonSerializerContext ctx) throws IOException, SerializeException {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = ctx.sort(m);

		int depth = ctx.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		while (mapEntries.hasNext()) {
			Map.Entry e = (Map.Entry) mapEntries.next();
			Object value = e.getValue();

			Object key = ctx.generalize(e.getKey(), keyType);

			out.cr(depth).attr(key).append(':').s();

			serializeAnything(out, value, valueType, ctx, (key == null ? null : key.toString()), null);

			if (mapEntries.hasNext())
				out.append(',').s();
		}

		out.cr(depth-1).append('}');

		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeCollectionMap(JsonSerializerWriter out, Collection o, ClassMeta<?> type, JsonSerializerContext ctx) throws IOException, SerializeException {
		int i = ctx.getIndent();
		out.append('{');
		out.cr(i).attr("_class").append(':').s().q().append(type.getInnerClass().getName()).q().append(',').s();
		out.cr(i).attr("items").append(':').s();
		ctx.indent++;
		serializeCollection(out, o, type, ctx);
		ctx.indent--;
		out.cr(i-1).append('}');
		return out;
	}

	@SuppressWarnings({ "rawtypes" })
	private SerializerWriter serializeBeanMap(JsonSerializerWriter out, BeanMap m, boolean addClassAttr, JsonSerializerContext ctx) throws IOException, SerializeException {
		int depth = ctx.getIndent();
		out.append('{');

		Iterator mapEntries = m.entrySet().iterator();

		// Print out "_class" attribute on this bean if required.
		if (addClassAttr) {
			String attr = "_class";
			out.cr(depth).attr(attr).append(':').s().q().append(m.getClassMeta().getInnerClass().getName()).q();
			if (mapEntries.hasNext())
				out.append(',').s();
		}

		boolean addComma = false;

		while (mapEntries.hasNext()) {
			BeanMapEntry p = (BeanMapEntry)mapEntries.next();
			BeanPropertyMeta pMeta = p.getMeta();

			String key = p.getKey();
			Object value = null;
			try {
				value = p.getValue();
			} catch (StackOverflowError e) {
				throw e;
			} catch (Throwable t) {
				ctx.addBeanGetterWarning(pMeta, t);
			}

			if (ctx.canIgnoreValue(pMeta.getClassMeta(), key, value))
				continue;

			if (addComma)
				out.append(',').s();

			out.cr(depth).attr(key).append(':').s();

			serializeAnything(out, value, pMeta.getClassMeta(), ctx, key, pMeta);

			addComma = true;
		}
		out.cr(depth-1).append('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonSerializerWriter out, Collection c, ClassMeta<?> type, JsonSerializerContext ctx) throws IOException, SerializeException {

		ClassMeta<?> elementType = type.getElementType();

		c = ctx.sort(c);

		out.append('[');
		int depth = ctx.getIndent();

		for (Iterator i = c.iterator(); i.hasNext();) {

			Object value = i.next();

			out.cr(depth);

			serializeAnything(out, value, elementType, ctx, "<iterator>", null);

			if (i.hasNext())
				out.append(',').s();
		}
		out.cr(depth-1).append(']');
		return out;
	}

	/**
	 * Returns the schema serializer based on the settings of this serializer.
	 * @return The schema serializer.
	 */
	public JsonSchemaSerializer getSchemaSerializer() {
		JsonSchemaSerializer s = new JsonSchemaSerializer();
		s.beanContextFactory = this.beanContextFactory;
		s.sp = this.sp;
		s.jsp = this.jsp;
		return s;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public JsonSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		return new JsonSerializerContext(getBeanContext(), sp, jsp, properties, javaMethod);
	}

	@Override /* Serializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		JsonSerializerContext jctx = (JsonSerializerContext)ctx;
		serializeAnything(jctx.getWriter(out), o, null, jctx, "root", null);
	}

	@Override /* CoreApi */
	public JsonSerializer setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! jsp.setProperty(property, value))
			super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> JsonSerializer addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public JsonSerializer setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public JsonSerializer clone() {
		try {
			JsonSerializer c = (JsonSerializer)super.clone();
			c.jsp = jsp.clone();
			return c;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
