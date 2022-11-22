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
package org.apache.juneau.swap;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Used to swap out non-serializable objects with serializable replacements during serialization, and vis-versa during
 * parsing.
 *
 * <h5 class='topic'>Description</h5>
 *
 * <p>
 * <c>ObjectSwaps</c> are used to extend the functionality of the serializers and parsers to be able to handle
 * objects that aren't automatically handled by the serializers or parsers.
 * <br>For example, JSON does not have a standard representation for rendering dates.
 * By defining a special {@code Date} swap and associating it with a serializer and parser, you can convert a
 * {@code Date} object to a {@code String} during serialization, and convert that {@code String} object back into a
 * {@code Date} object during parsing.
 *
 * <p>
 * Swaps MUST declare a public no-arg constructor so that the bean context can instantiate them.
 *
 * <p>
 * <c>ObjectSwaps</c> are associated with serializers and parsers through the following:
 * <ul class='javatree'>
 * 	<li class='ja'>{@link Swap @Swap}
 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#swaps(Class...)}
 * </ul>
 *
 * <p>
 * <c>ObjectSwaps</c> have two parameters:
 * <ol>
 * 	<li>{@code <T>} - The normal representation of an object.
 * 	<li>{@code <S>} - The swapped representation of an object.
 * </ol>
 * <br>{@link Serializer Serializers} use swaps to convert objects of type T into objects of type S, and on calls to
 * {@link BeanMap#get(Object)}.
 * <br>{@link Parser Parsers} use swaps to convert objects of type S into objects of type T, and on calls to
 * {@link BeanMap#put(String,Object)}.
 *
 * <h5 class='topic'>Swap Class Type {@code <S>}</h5>
 *
 * <p>
 * For normal serialization, the swapped object representation of an object must be an object type that the serializers can natively convert to
 * JSON (or language-specific equivalent).
 * <br>The list of valid transformed types are as follows...
 * <ul>
 * 	<li>
 * 		{@link String}
 * 	<li>
 * 		{@link Number}
 * 	<li>
 * 		{@link Boolean}
 * 	<li>
 * 		{@link Collection} containing anything on this list.
 * 	<li>
 * 		{@link Map} containing anything on this list.
 * 	<li>
 * 		A java bean with properties of anything on this list.
 * 	<li>
 * 		An array of anything on this list.
 * </ul>
 *
 * <p>
 * For OpenAPI serialization, the valid swapped types also include <code><jk>byte</jk>[]</code> and <c>Calendar</c>.
 *
 * <h5 class='topic'>Normal Class Type {@code <T>}</h5>
 *
 * <p>
 * The normal object representation of an object.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 *
 * @param <T> The normal form of the class.
 * @param <S> The swapped form of the class.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class ObjectSwap<T,S> {

	/**
	 * Represents a non-existent object swap.
	 */
	public static final ObjectSwap NULL = new ObjectSwap((Class)null, (Class)null) {};

	private final Class<T> normalClass;
	private final Class<?> swapClass;
	private final ClassInfo normalClassInfo, swapClassInfo;
	private ClassMeta<?> swapClassMeta;

	// Unfortunately these cannot be made final because we want to allow for ObjectSwaps with no-arg constructors
	// which simplifies declarations.
	private MediaType[] forMediaTypes;
	private String template;

	/**
	 * Constructor.
	 */
	protected ObjectSwap() {
		ClassInfo ci = ClassInfo.of(this.getClass());
		normalClass = (Class<T>)ci.getParameterType(0, ObjectSwap.class);
		swapClass = ci.getParameterType(1, ObjectSwap.class);
		normalClassInfo = ClassInfo.of(normalClass);
		swapClassInfo = ClassInfo.of(swapClass);
		forMediaTypes = forMediaTypes();
		template = withTemplate();
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param swapClass The transformed class (serializable).
	 */
	protected ObjectSwap(Class<T> normalClass, Class<?> swapClass) {
		this.normalClass = normalClass;
		this.swapClass = swapClass;
		normalClassInfo = ClassInfo.of(normalClass);
		swapClassInfo = ClassInfo.of(swapClass);
		this.forMediaTypes = forMediaTypes();
		this.template = withTemplate();
	}

	/**
	 * Returns the media types that this swap is applicable to.
	 *
	 * <p>
	 * This method can be overridden to programmatically specify what media types it applies to.
	 *
	 * <p>
	 * This method is the programmatic equivalent to the {@link Swap#mediaTypes() @Swap(mediaTypes)} annotation.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.PerMediaTypeSwaps">Per-media-type Swaps</a>
	 * </ul>
	 * @return The media types that this swap is applicable to, or <jk>null</jk> if it's applicable for all media types.
	 */
	public MediaType[] forMediaTypes() {
		return null;
	}

	/**
	 * Returns additional context information for this swap.
	 *
	 * <p>
	 * Typically this is going to be used to specify a template name, such as a FreeMarker template file name.
	 *
	 * <p>
	 * This method can be overridden to programmatically specify a template value.
	 *
	 * <p>
	 * This method is the programmatic equivalent to the {@link Swap#template() @Swap(template)} annotation.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.TemplatedSwaps">Templated Swaps</a>
	 * </ul>
	 *
	 * @return Additional context information, or <jk>null</jk> if not specified.
	 */
	public String withTemplate() {
		return null;
	}

	/**
	 * Sets the media types that this swap is associated with.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.PerMediaTypeSwaps">Per-media-type Swaps</a>
	 * </ul>
	 *
	 * @param mediaTypes The media types that this swap is associated with.
	 * @return This object.
	 */
	public ObjectSwap<T,?> forMediaTypes(MediaType[] mediaTypes) {
		this.forMediaTypes = mediaTypes;
		return this;
	}

	/**
	 * Sets the template string on this swap.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.TemplatedSwaps">Templated Swaps</a>
	 * </ul>
	 *
	 * @param template The template string on this swap.
	 * @return This object.
	 */
	public ObjectSwap<T,?> withTemplate(String template) {
		this.template = template;
		return this;
	}

	/**
	 * Returns a number indicating how well this swap matches the specified session.
	 *
	 * <p>
	 * Uses the {@link MediaType#match(MediaType, boolean)} method algorithm to produce a number whereby a
	 * larger value indicates a "better match".
	 * The idea being that if multiple swaps are associated with a given object, we want to pick the best one.
	 *
	 * <p>
	 * For example, if the session media type is <js>"text/json"</js>, then the match values are shown below:
	 *
	 * <ul>
	 * 	<li><js>"text/json"</js> = <c>100,000</c>
	 * 	<li><js>"&#42;/json"</js> = <c>5,100</c>
	 * 	<li><js>"&#42;/&#42;"</js> = <c>5,000</c>
	 * 	<li>No media types specified on swap = <c>1</c>
	 * 	<li><js>"text/xml"</js> = <c>0</c>
	 * </ul>
	 *
	 * @param session The bean session.
	 * @return Zero if swap doesn't match the session, or a positive number if it does.
	 */
	public int match(BeanSession session) {
		if (forMediaTypes == null)
			return 1;
		int i = 0;
		MediaType mt = session.getMediaType();
		if (mt == null)
			return 0;
		if (forMediaTypes != null)
			for (MediaType mt2 : forMediaTypes)
				i = Math.max(i, mt2.match(mt, false));
		return i;
	}

	/**
	 * If this transform is to be used to serialize non-serializable objects, it must implement this method.
	 *
	 * <p>
	 * The object must be converted into one of the following serializable types:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@link String}
	 * 	<li>
	 * 		{@link Number}
	 * 	<li>
	 * 		{@link Boolean}
	 * 	<li>
	 * 		{@link Collection} containing anything on this list.
	 * 	<li>
	 * 		{@link Map} containing anything on this list.
	 * 	<li>
	 * 		A java bean with properties of anything on this list.
	 * 	<li>
	 * 		An array of anything on this list.
	 * </ul>
	 *
	 * @param session
	 * 	The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param o The object to be transformed.
	 * @return The transformed object.
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	public S swap(BeanSession session, T o) throws Exception {
		return swap(session, o, template);
	}

	/**
	 * Same as {@link #swap(BeanSession, Object)}, but can be used if your swap has a template associated with it.
	 *
	 * @param session
	 * 	The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param o The object to be transformed.
	 * @param template
	 * 	The template string associated with this swap.
	 * @return The transformed object.
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	public S swap(BeanSession session, T o, String template) throws Exception {
		throw new SerializeException("Swap method not implemented on ObjectSwap ''{0}''", className(this));
	}

	/**
	 * If this transform is to be used to reconstitute objects that aren't true Java beans, it must implement this method.
	 *
	 * @param session
	 * 	The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param f The transformed object.
	 * @param hint
	 * 	If possible, the parser will try to tell you the object type being created.
	 * 	For example, on a serialized date, this may tell you that the object being created must be of type
	 * 	{@code GregorianCalendar}.
	 * 	<br>This may be <jk>null</jk> if the parser cannot make this determination.
	 * @return The narrowed object.
	 * @throws Exception If this method is not implemented.
	 */
	public T unswap(BeanSession session, S f, ClassMeta<?> hint) throws Exception {
		return unswap(session, f, hint, template);
	}

	/**
	 * Same as {@link #unswap(BeanSession, Object, ClassMeta)}, but can be used if your swap has a template associated with it.
	 *
	 * @param session
	 * 	The bean session to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @param f The transformed object.
	 * @param hint
	 * 	If possible, the parser will try to tell you the object type being created.
	 * 	For example, on a serialized date, this may tell you that the object being created must be of type
	 * 	{@code GregorianCalendar}.
	 * 	<br>This may be <jk>null</jk> if the parser cannot make this determination.
	 * @param template
	 * 	The template string associated with this swap.
	 * @return The transformed object.
	 * @throws Exception If a problem occurred trying to convert the output.
	 */
	public T unswap(BeanSession session, S f, ClassMeta<?> hint, String template) throws Exception {
		throw new ParseException("Unswap method not implemented on ObjectSwap ''{0}''", className(this));
	}

	/**
	 * Returns the T class, the normalized form of the class.
	 *
	 * @return The normal form of this class.
	 */
	public ClassInfo getNormalClass() {
		return normalClassInfo;
	}

	/**
	 * Returns the G class, the generalized form of the class.
	 *
	 * <p>
	 * Subclasses must override this method if the generalized class is {@code Object}, meaning it can produce multiple
	 * generalized forms.
	 *
	 * @return The transformed form of this class.
	 */
	public ClassInfo getSwapClass() {
		return swapClassInfo;
	}

	/**
	 * Returns the {@link ClassMeta} of the transformed class type.
	 *
	 * <p>
	 * This value is cached for quick lookup.
	 *
	 * @param session
	 * 	The bean context to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @return The {@link ClassMeta} of the transformed class type.
	 */
	public ClassMeta<?> getSwapClassMeta(BeanSession session) {
		if (swapClassMeta == null)
			swapClassMeta = session.getClassMeta(swapClass);
		return swapClassMeta;
	}

	/**
	 * Checks if the specified object is an instance of the normal class defined on this swap.
	 *
	 * @param o The object to check.
	 * @return
	 * 	<jk>true</jk> if the specified object is a subclass of the normal class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isNormalObject(Object o) {
		if (o == null)
			return false;
		return normalClassInfo.isParentOf(o.getClass());
	}

	/**
	 * Checks if the specified object is an instance of the swap class defined on this swap.
	 *
	 * @param o The object to check.
	 * @return
	 * 	<jk>true</jk> if the specified object is a subclass of the transformed class defined on this transform.
	 * 	<jk>null</jk> always return <jk>false</jk>.
	 */
	public boolean isSwappedObject(Object o) {
		if (o == null)
			return false;
		return swapClassInfo.isParentOf(o.getClass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return getClass().getSimpleName() + '<' + getNormalClass().getSimpleName() + "," + getSwapClass().getSimpleName() + '>';
	}
}
