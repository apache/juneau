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
package org.apache.juneau.transform;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Used to swap out non-serializable objects with serializable replacements during serialization, and vis-versa during
 * parsing.
 *
 *
 * <h6 class='section'>Description:</h6>
 *
 * <p>
 * <code>PojoSwaps</code> are used to extend the functionality of the serializers and parsers to be able to handle
 * POJOs that aren't automatically handled by the serializers or parsers.
 * <br>For example, JSON does not have a standard representation for rendering dates.
 * By defining a special {@code Date} swap and associating it with a serializer and parser, you can convert a
 * {@code Date} object to a {@code String} during serialization, and convert that {@code String} object back into a
 * {@code Date} object during parsing.
 *
 * <p>
 * Swaps MUST declare a public no-arg constructor so that the bean context can instantiate them.
 *
 * <p>
 * <code>PojoSwaps</code> are associated with instances of {@link BeanContext BeanContexts} by passing the swap
 * class to the {@link SerializerBuilder#pojoSwaps(Class...)} and {@link ParserBuilder#pojoSwaps(Class...)} methods.
 * <br>When associated with a bean context, fields of the specified type will automatically be converted when the
 * {@link BeanMap#get(Object)} or {@link BeanMap#put(String, Object)} methods are called.
 *
 * <p>
 * <code>PojoSwaps</code> have two parameters:
 * <ol>
 * 	<li>{@code <T>} - The normal representation of an object.
 * 	<li>{@code <S>} - The swapped representation of an object.
 * </ol>
 * <br>{@link Serializer Serializers} use swaps to convert objects of type T into objects of type S, and on calls to
 * {@link BeanMap#get(Object)}.
 * <br>{@link Parser Parsers} use swaps to convert objects of type S into objects of type T, and on calls to
 * {@link BeanMap#put(String,Object)}.
 *
 *
 * <h6 class='topic'>Subtypes</h6>
 *
 * The following abstract subclasses are provided for common swap types:
 * <ol>
 * 	<li>{@link StringSwap} - Objects swapped with strings.
 * 	<li>{@link MapSwap} - Objects swapped with {@link ObjectMap ObjectMaps}.
 * </ol>
 *
 *
 * <h6 class='topic'>Swap Class Type {@code <S>}</h6>
 *
 * The swapped object representation of an object must be an object type that the serializers can natively convert to
 * JSON (or language-specific equivalent).
 * The list of valid transformed types are as follows...
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
 *
 * <h6 class='topic'>Normal Class Type {@code <T>}</h6>
 *
 * The normal object representation of an object.
 *
 *
 * <h6 class='topic'>Overview</h6>
 *
 * The following is an example of a swap that replaces byte arrays with BASE-64 encoded strings:
 *
 * <p class='bcode'>
 * 	<jk>public class</jk> ByteArrayBase64Swap <jk>extends</jk> PojoSwap<<jk>byte</jk>[],String> {
 *
 * 		<jk>public</jk> String swap(BeanSession session, <jk>byte</jk>[] b) <jk>throws</jk> SerializeException {
 * 			<jk>return</jk> StringUtils.<jsm>base64Encode</jsm>(b);
 * 		}
 *
 * 		<jk>public byte</jk>[] unswap(BeanSession session, String s, ClassMeta&lt;?&gt; hint) <jk>throws</jk> ParseException {
 * 			<jk>return</jk> StringUtils.<jsm>base64Decode</jsm>(s);
 * 		}
 * 	}
 * </p>
 *
 * <p class='bcode'>
 * 	WriterSerializer s = JsonSerializer.<jsf>DEFAULT_LAX</jsf>.builder().pojoSwaps(ByteArrayBase64Swap.<jk>class</jk>).build();
 * 	String json = s.serialize(<jk>new byte</jk>[] {1,2,3});  <jc>// Produces "'AQID'"</jc>
 * </p>
 *
 *
 * <h6 class='topic'>Swap annotation</h6>
 *
 * <p>
 * Swap classes are often associated directly with POJOs using the {@link Swap @Swap} annotation.
 *
 * <p class='bcode'>
 * 	<jk>public class</jk> MyPojoSwap <jk>extends</jk> PojoSwap&lt;MyPojo,String&gt; { ... }
 *
 * 	<ja>@Swap</ja>(MyPojoSwap.<jk>class</jk>)
 * 	<jk>public class</jk> MyPojo { ... }
 * </p>
 *
 * <p>
 * The <ja>@Swap</ja> annotation is often simpler since you do not need to tell your serializers and parsers about them
 * leading to less code.
 *
 * <p>
 * Swaps can also be associated with getters and setters as well:
 *
 * <p class='bcode'>
 * 	<ja>@BeanProperty</ja>(swap=MyPojo.<jk>class</jk>)
 * 	<jk>public</jk> MyPojo getMyPojo();
 * </p>
 *
 *
 * <h6 class='topic'>One-way vs. Two-way Serialization</h6>
 *
 * Note that while there is a unified interface for handling swaps during both serialization and parsing,
 * in many cases only one of the {@link #swap(BeanSession, Object)} or {@link #unswap(BeanSession, Object, ClassMeta)}
 * methods will be defined because the swap is one-way.
 * For example, a swap may be defined to convert an {@code Iterator} to a {@code ObjectList}, but
 * it's not possible to unswap an {@code Iterator}.
 * In that case, the {@code swap(Object}} method would be implemented, but the {@code unswap(ObjectMap)} object would
 * not, and the swap would be associated on the serializer, but not the parser.
 * Also, you may choose to serialize objects like {@code Dates} to readable {@code Strings}, in which case it's not
 * possible to re-parse it back into a {@code Date}, since there is no way for the {@code Parser} to know it's a
 * {@code Date} from just the JSON or XML text.
 *
 *
 * <h6 class='topic'>Per media-type swaps</h6>
 * <p>
 * The {@link #forMediaTypes()} method can be overridden to provide a set of media types that the swap is invoked on.
 * It's also possible to define multiple swaps against the same POJO as long as they're differentiated by media type.
 * When multiple swaps are defined, the best-match media type is used.
 *
 * <p>
 * In the following example, we define 3 swaps against the same POJO.  One for JSON, one for XML, and one for all
 * other types.
 *
 * <p class='bcode'>
 * 	<jk>public class</jk> PojoSwapTest {
 *
 * 		<jk>public static class</jk> MyPojo {}
 *
 * 		<jk>public static class</jk> MyJsonSwap <jk>extends</jk> PojoSwap&lt;MyPojo,String&gt; {
 *
 * 			<jk>public</jk> MediaType[] forMediaTypes() {
 * 				<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/json"</js>);
 * 			}
 *
 * 			<jk>public</jk> String swap(BeanSession session, MyPojo o) <jk>throws</jk> Exception {
 * 				<jk>return</jk> <js>"It's JSON!"</js>;
 * 			}
 * 		}
 *
 * 		<jk>public static class</jk> MyXmlSwap <jk>extends</jk> PojoSwap&lt;MyPojo,String&gt; {
 *
 * 			<jk>public</jk> MediaType[] forMediaTypes() {
 * 				<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/xml"</js>);
 * 			}
 *
 * 			<jk>public</jk> String swap(BeanSession session, MyPojo o) <jk>throws</jk> Exception {
 * 				<jk>return</jk> <js>"It's XML!"</js>;
 * 			}
 * 		}
 *
 * 		<jk>public static class</jk> MyOtherSwap <jk>extends</jk> PojoSwap&lt;MyPojo,String&gt; {
 *
 * 			<jk>public</jk> MediaType[] forMediaTypes() {
 * 				<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/*"</js>);
 * 			}
 *
 * 			<jk>public</jk> String swap(BeanSession session, MyPojo o) <jk>throws</jk> Exception {
 * 				<jk>return</jk> <js>"It's something else!"</js>;
 * 			}
 * 		}
 *
 * 		<ja>@Test</ja>
 * 		<jk>public void</jk> doTest() <jk>throws</jk> Exception {
 *
 * 			SerializerGroup g = <jk>new</jk> SerializerGroupBuilder()
 * 				.append(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>, HtmlSerializer.<jk>class</jk>)
 * 				.sq()
 * 				.pojoSwaps(MyJsonSwap.<jk>class</jk>, MyXmlSwap.<jk>class</jk>, MyOtherSwap.<jk>class</jk>)
 * 				.build();
 *
 * 			MyPojo myPojo = <jk>new</jk> MyPojo();
 *
 * 			String json = g.getWriterSerializer(<js>"text/json"</js>).serialize(myPojo);
 * 			<jsm>assertEquals</jsm>(<js>"'It\\'s JSON!'"</js>, json);
 *
 * 			String xml = g.getWriterSerializer(<js>"text/xml"</js>).serialize(myPojo);
 * 			<jsm>assertEquals</jsm>(<js>"&lt;string&gt;It's XML!&lt;/string&gt;"</js>, xml);
 *
 * 			String html = g.getWriterSerializer(<js>"text/html"</js>).serialize(myPojo);
 * 			<jsm>assertEquals</jsm>(<js>"&lt;string&gt;It's something else!&lt;/string&gt;"</js>, html);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Multiple swaps can be associated with a POJO by using the {@link Swaps @Swaps} annotation:
 *
 * <p class='bcode'>
 * 	<ja>@Swaps</ja>(
 * 		{
 * 			<ja>@Swap</ja>(MyJsonSwap.<jk>class</jk>),
 * 			<ja>@Swap</ja>(MyXmlSwap.<jk>class</jk>),
 * 			<ja>@Swap</ja>(MyOtherSwap.<jk>class</jk>)
 * 		}
 * 	)
 * 	<jk>public class</jk> MyPojo {}
 * </p>
 *
 * <p>
 * Note that since <code>Readers</code> get serialized directly to the output of a serializer, it's possible to
 * implement a swap that provides fully-customized output.
 *
 * <p class='bcode'>
 * 	<jk>public class</jk> MyJsonSwap <jk>extends</jk> PojoSwap&lt;MyPojo,Reader&gt; {
 *
 * 		<jk>public</jk> MediaType[] forMediaTypes() {
 * 			<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/json"</js>);
 * 		}
 *
 * 		<jk>public</jk> Reader swap(BeanSession session, MyPojo o) <jk>throws</jk> Exception {
 * 			<jk>return new</jk> StringReader(<js>"{message:'Custom JSON!'}"</js>);
 * 		}
 * 	}
 * </p>
 *
 *
 * <h6 class='topic'>Templates</h6>
 *
 * <p>
 * Template strings are arbitrary strings associated with swaps that help provide additional context information
 * for the swap class.
 * They're called 'templates' because their primary purpose is for providing template names, such as Apache FreeMarker
 * template names.
 *
 * <p>
 * The following is an example of a templated swap class used to serialize POJOs to HTML using FreeMarker:
 *
 * <p class='bcode'>
 * 	<jc>// Our abstracted templated swap class.</jc>
 * 	<jk>public abstract class</jk> FreeMarkerSwap <jk>extends</jk> PojoSwap&lt;Object,Reader&gt; {
 *
 * 		<jk>public</jk> MediaType[] forMediaTypes() {
 * 			<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/html"</js>);
 * 		}
 *
 * 		<jk>public</jk> Reader swap(BeanSession session, Object o, String template) <jk>throws</jk> Exception {
 * 			<jk>return</jk> getFreeMarkerReader(template, o);  <jc>// Some method that creates raw HTML.</jc>
 * 		}
 * 	}
 *
 * 	<jc>// An implementation of our templated swap class.</jc>
 * 	<jk>public class</jk> MyPojoSwap <jk>extends</jk> FreeMarkerSwap {
 * 		<jk>public</jk> String withTemplate() {
 * 			<jk>return</jk> <js>"MyPojo.div.ftl"</js>;
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * In practice however, the template is usually going to be defined on a <ja>@Swap</ja> annotation like the following
 * example:
 *
 * <p class='bcode'>
 * 	<ja>@Swap</ja>(impl=FreeMarkerSwap.<jk>class</jk>, template=<js>"MyPojo.div.ftl"</js>)
 * 	<jk>public class</jk> MyPojo {}
 * </p>
 *
 *
 * <h6 class='topic'>Localization</h6>
 *
 * Swaps have access to the session locale and timezone through the {@link BeanSession#getLocale()} and
 * {@link BeanSession#getTimeZone()} methods.
 * This allows you to specify localized swap values when needed.
 * If using the REST server API, the locale and timezone are set based on the <code>Accept-Language</code> and
 * <code>Time-Zone</code> headers on the request.
 *
 *
 * <h6 class='section'>Additional information:</h6>
 *
 * See <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.transform</a> for more information.
 *
 * @param <T> The normal form of the class.
 * @param <S> The swapped form of the class.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class PojoSwap<T,S> {

	/**
	 * Represents a non-existent pojo swap.
	 */
	public final static PojoSwap NULL = new PojoSwap((Class)null, (Class)null) {};

	private final Class<T> normalClass;
	private final Class<?> swapClass;
	private ClassMeta<?> swapClassMeta;

	// Unfortunately these cannot be made final because we want to allow for PojoSwaps with no-arg constructors
	// which simplifies declarations.
	private MediaType[] forMediaTypes;
	private String template;

	/**
	 * Constructor.
	 */
	protected PojoSwap() {
		normalClass = (Class<T>)resolveParameterType(PojoSwap.class, 0, this.getClass());
		swapClass = resolveParameterType(PojoSwap.class, 1, this.getClass());
		forMediaTypes = forMediaTypes();
		template = withTemplate();
	}

	/**
	 * Constructor for when the normal and transformed classes are already known.
	 *
	 * @param normalClass The normal class (cannot be serialized).
	 * @param swapClass The transformed class (serializable).
	 */
	protected PojoSwap(Class<T> normalClass, Class<?> swapClass) {
		this.normalClass = normalClass;
		this.swapClass = swapClass;
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
	 * This method is the programmatic equivalent to the {@link Swap#mediaTypes()} annotation.
	 *
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
	 * This method is the programmatic equivalent to the {@link Swap#template()} annotation.
	 *
	 * @return Additional context information, or <jk>null</jk> if not specified.
	 */
	public String withTemplate() {
		return null;
	}

	/**
	 * Sets the media types that this swap is associated with.
	 *
	 * @param mediaTypes The media types that this swap is associated with.
	 * @return This object (for method chaining).
	 */
	public PojoSwap<T,?> forMediaTypes(MediaType[] mediaTypes) {
		this.forMediaTypes = mediaTypes;
		return this;
	}

	/**
	 * Sets the template string on this swap.
	 *
	 * @param template The template string on this swap.
	 * @return This object (for method chaining).
	 */
	public PojoSwap<T,?> withTemplate(String template) {
		this.template = template;
		return this;
	}

	/**
	 * Returns a number indicating how well this swap matches the specified session.
	 *
	 * <p>
	 * Uses the {@link MediaType#match(MediaType, boolean)} method algorithm to produce a number whereby a
	 * larger value indicates a "better match".
	 * The idea being that if multiple swaps are associated with a given POJO, we want to pick the best one.
	 *
	 * <p>
	 * For example, if the session media type is <js>"text/json"</js>, then the match values are shown below:
	 *
	 * <ul>
	 * 	<li><js>"text/json"</js> = <code>100,000</code>
	 * 	<li><js>"&#42;/json"</js> = <code>5,100</code>
	 * 	<li><js>"&#42;/&#42;"</js> = <code>5,000</code>
	 * 	<li>No media types specified on swap = <code>1</code>
	 * 	<li><js>"text/xml"</js> = <code>0</code>
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
		if (forMediaTypes != null)
			for (MediaType mt2 : forMediaTypes)
				i = Math.max(i, mt2.match(mt, false));
		return i;
	}

	/**
	 * If this transform is to be used to serialize non-serializable POJOs, it must implement this method.
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
		throw new SerializeException("Swap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
	}

	/**
	 * If this transform is to be used to reconstitute POJOs that aren't true Java beans, it must implement this method.
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
		throw new ParseException("Unswap method not implemented on PojoSwap ''{0}''", this.getClass().getName());
	}

	/**
	 * Returns the T class, the normalized form of the class.
	 *
	 * @return The normal form of this class.
	 */
	public Class<T> getNormalClass() {
		return normalClass;
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
	public Class<?> getSwapClass() {
		return swapClass;
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
		return isParentClass(normalClass, o.getClass());
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
		return isParentClass(swapClass, o.getClass());
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return getClass().getSimpleName() + '<' + getNormalClass().getSimpleName() + "," + getSwapClass().getSimpleName() + '>';
	}
}
