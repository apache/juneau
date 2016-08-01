/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import static com.ibm.juno.core.serializer.SerializerProperties.*;
import static com.ibm.juno.core.utils.ClassUtils.*;

import java.io.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.serializer.*;

/**
 * Serializes POJO metadata to HTTP responses as JSON.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/json+schema, text/json+schema</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Produces the JSON-schema for the JSON produced by the {@link JsonSerializer} class with the same properties.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces(value={"application/json+schema","text/json+schema"},contentType="application/json")
public final class JsonSchemaSerializer extends JsonSerializer {

	/**
	 * Constructor.
	 */
	public JsonSchemaSerializer() {
		setProperty(SERIALIZER_detectRecursions, true);
		setProperty(SERIALIZER_ignoreRecursions, true);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* JsonSerializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		JsonSerializerContext jctx = (JsonSerializerContext)ctx;
		ObjectMap schema = getSchema(ctx.getBeanContext().getClassMetaForObject(o), jctx, "root", null);
		serializeAnything(jctx.getWriter(out), schema, null, jctx, "root", null);
	}

	/*
	 * Creates a schema representation of the specified class type.
	 *
	 * @param eType The class type to get the schema of.
	 * @param ctx Serialize context used to prevent infinite loops.
	 * @param attrName The name of the current attribute.
	 * @return A schema representation of the specified class.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ObjectMap getSchema(ClassMeta<?> eType, JsonSerializerContext ctx, String attrName, String[] pNames) throws SerializeException {
		try {

			ObjectMap out = new ObjectMap();

			if (eType == null)
				eType = object();

			ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
			ClassMeta<?> gType;			// The generic type

			aType = ctx.push(attrName, eType, null);

			gType = eType.getFilteredClassMeta();
			String type = null;

			if (gType.isEnum() || gType.isCharSequence() || gType.isChar())
				type = "string";
			else if (gType.isNumber())
				type = "number";
			else if (gType.isBoolean())
				type = "boolean";
			else if (gType.isBean() || gType.isMap())
				type = "object";
			else if (gType.isCollection() || gType.isArray())
				type = "array";
			else
				type = "any";

			out.put("type", type);
			out.put("description", eType.toString());
			PojoFilter f = eType.getPojoFilter();
			if (f != null)
				out.put("filter", f);

			if (aType != null) {
				if (gType.isEnum())
					out.put("enum", getEnumStrings((Class<Enum<?>>)gType.getInnerClass()));
				else if (gType.isCollection() || gType.isArray()) {
					ClassMeta componentType = gType.getElementType();
					if (gType.isCollection() && isParentClass(Set.class, gType.getInnerClass()))
						out.put("uniqueItems", true);
					out.put("items", getSchema(componentType, ctx, "items", pNames));
				} else if (gType.isBean()) {
					ObjectMap properties = new ObjectMap();
					BeanMeta bm = ctx.getBeanContext().getBeanMeta(gType.getInnerClass());
					if (pNames != null)
						bm = new BeanMetaFiltered(bm, pNames);
					for (Iterator<BeanPropertyMeta<?>> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
						BeanPropertyMeta p = i.next();
						properties.put(p.getName(), getSchema(p.getClassMeta(), ctx, p.getName(), p.getProperties()));
					}
					out.put("properties", properties);
				}
			}
			ctx.pop();
			return out;
		} catch (StackOverflowError e) {
			throw e;
		} catch (Throwable e) {
			throw new SerializeException("Exception occured trying to process object of type ''{0}''", eType).initCause(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> getEnumStrings(Class<? extends Enum> c) {
		List<String> l = new LinkedList<String>();
		try {
			for (Object e : EnumSet.allOf(c))
				l.add(e.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}


	@Override /* Lockable */
	public JsonSchemaSerializer lock() {
		super.lock();
		return this;
	}
}
