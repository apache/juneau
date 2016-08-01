/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import static com.ibm.juno.core.utils.ArrayUtils.*;

import java.util.*;

import com.ibm.juno.core.*;

/**
 * Represents a group of {@link Parser Parsers} that can be looked up by media type.
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Provides the following features:
 * <ul>
 * 	<li>Finds parsers based on HTTP <code>Content-Type</code> header values.
 * 	<li>Sets common properties on all parsers in a single method call.
 * 	<li>Locks all parsers in a single method call.
 * 	<li>Clones existing groups and all parsers within the group in a single method call.
 * </ul>
 *
 *
 * <h6 class='topic'>Match ordering</h6>
 * <p>
 * 	Parsers are matched against <code>Content-Type</code> strings in the order they exist in this group.
 * <p>
 * 	Adding new entries will cause the entries to be prepended to the group.
 *  	This allows for previous parsers to be overridden through subsequent calls.
 * <p>
 * 	For example, calling <code>g.append(P1.<jk>class</jk>,P2.<jk>class</jk>).append(P3.<jk>class</jk>,P4.<jk>class</jk>)</code>
 * 	will result in the order <code>P3, P4, P1, P2</code>.
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Construct a new parser group</jc>
 * 	ParserGroup g = <jk>new</jk> ParserGroup();
 *
 * 	<jc>// Add some parsers to it</jc>
 * 	g.append(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
 *
 * 	<jc>// Change settings on parsers simultaneously</jc>
 * 	g.setProperty(BeanContextProperties.<jsf>BEAN_beansRequireSerializable</jsf>, <jk>true</jk>)
 * 		.addFilters(CalendarFilter.ISO8601DT.<jk>class</jk>)
 * 		.lock();
 *
 * 	<jc>// Find the appropriate parser by Content-Type</jc>
 * 	ReaderParser p = (ReaderParser)g.getParser(<js>"text/json"</js>);
 *
 * 	<jc>// Parse a bean from JSON</jc>
 * 	String json = <js>"{...}"</js>;
 * 	AddressBook addressBook = p.parse(json, AddressBook.<jk>class</jk>);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ParserGroup extends Lockable {

	private transient Map<String,ParserEntry> entryMap = new HashMap<String,ParserEntry>();
	private transient LinkedList<ParserEntry> tempEntries = new LinkedList<ParserEntry>();
	private transient ParserEntry[] entries;


	/**
	 * Registers the specified REST parsers with this parser group.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 */
	public ParserGroup append(Parser<?>...p) {
		checkLock();
		entries = null;
		for (Parser<?> pp : reverse(p))  {
			ParserEntry e = new ParserEntry(pp);
			tempEntries.addFirst(e);
			for (String mediaType : e.mediaTypes)
				entryMap.put(mediaType, e);
		}
		return this;
	}

	/**
	 * Same as {@link #append(Parser[])}, except specify classes instead of class instances
	 * 	 of {@link Parser}.
	 * <p>
	 * Note that this can only be used on {@link Parser Parsers} with no-arg constructors.
	 *
	 * @param p The parsers to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Parser} could not be constructed.
	 */
	public ParserGroup append(Class<? extends Parser<?>>...p) throws Exception {
		checkLock();
		for (Class<? extends Parser<?>> c : reverse(p)) {
			try {
			append(c.newInstance());
			} catch (NoClassDefFoundError e) {
				// Ignore if dependent library not found (e.g. Jena).
				System.err.println(e);
			}
		}
		return this;
	}

	/**
	 * Same as {@link #append(Class[])}, except specify a single class to avoid unchecked compile warnings.
	 *
	 * @param p The parser to append to this group.
	 * @return This object (for method chaining).
	 * @throws Exception Thrown if {@link Parser} could not be constructed.
	 */
	public ParserGroup append(Class<? extends Parser<?>> p) throws Exception {
		checkLock();
		try {
		append(p.newInstance());
		} catch (NoClassDefFoundError e) {
			// Ignore if dependent library not found (e.g. Jena).
			System.err.println(e);
		}
		return this;
	}

	/**
	 * Returns the parser registered to handle the specified media type.
	 * <p>
	 * The media-type string must not contain any parameters such as <js>";charset=X"</js>.
	 *
	 * @param mediaType The media-type string (e.g. <js>"text/json"</js>).
	 * @return The REST parser that handles the specified request content type, or <jk>null</jk> if
	 * 		no parser is registered to handle it.
	 */
	public Parser<?> getParser(String mediaType) {
		ParserEntry e = entryMap.get(mediaType);
		return (e == null ? null : e.parser);
	}

	/**
	 * Searches the group for a parser that can handle the specified media type.
	 *
	 * @param mediaType The accept string.
	 * @return The media type registered by one of the parsers that matches the <code>mediaType</code> string,
	 * 	or <jk>null</jk> if no media types matched.
	 */
	public String findMatch(String mediaType) {
		MediaRange[] mr = MediaRange.parse(mediaType);
		if (mr.length == 0)
			mr = MediaRange.parse("*/*");

		for (MediaRange a : mr)
			for (ParserEntry e : getEntries())
				for (MediaRange a2 : e.mediaRanges)
					if (a.matches(a2))
						return a2.getMediaType();

		return null;
	}

	/**
	 * Returns the media types that all parsers in this group can handle
	 * <p>
	 * Entries are ordered in the same order as the parsers in the group.
	 *
	 * @return The list of media types.
	 */
	public List<String> getSupportedMediaTypes() {
		List<String> l = new ArrayList<String>();
		for (ParserEntry e : getEntries())
			for (String mt : e.mediaTypes)
				if (! l.contains(mt))
					l.add(mt);
		return l;
	}

	private ParserEntry[] getEntries() {
		if (entries == null)
			entries = tempEntries.toArray(new ParserEntry[tempEntries.size()]);
		return entries;
	}

	static class ParserEntry {
		Parser<?> parser;
		MediaRange[] mediaRanges;
		String[] mediaTypes;

		ParserEntry(Parser<?> p) {
			parser = p;

			mediaTypes = new String[p.getMediaTypes().length];
			int i = 0;
			for (String mt : p.getMediaTypes())
				mediaTypes[i++] = mt.toLowerCase(Locale.ENGLISH);

			List<MediaRange> l = new LinkedList<MediaRange>();
			for (i = 0; i < mediaTypes.length; i++)
				l.addAll(Arrays.asList(MediaRange.parse(mediaTypes[i])));
			mediaRanges = l.toArray(new MediaRange[l.size()]);
		}
	}


	//--------------------------------------------------------------------------------
	// Convenience methods for setting properties on all parsers.
	//--------------------------------------------------------------------------------

	/**
	 * Shortcut for calling {@link Parser#setProperty(String, Object)} on all parsers in this group.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup setProperty(String property, Object value) throws LockedException {
		checkLock();
		for (ParserEntry e : getEntries())
			e.parser.setProperty(property, value);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#setProperties(ObjectMap)} on all parsers in this group.
	 *
	 * @param properties The properties to set.  Ignored if <jk>null</jk>.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup setProperties(ObjectMap properties) {
		checkLock();
		if (properties != null)
			for (Map.Entry<String,Object> e : properties.entrySet())
				setProperty(e.getKey(), e.getValue());
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addNotBeanClasses(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to specify as not-beans to the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addNotBeanClasses(Class<?>...classes) throws LockedException {
		checkLock();
		for (ParserEntry e : getEntries())
			e.parser.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addFilters(Class[])} on all parsers in this group.
	 *
	 * @param classes The classes to add bean filters for to the underlying bean context of all parsers in this group.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public ParserGroup addFilters(Class<?>...classes) throws LockedException {
		checkLock();
		for (ParserEntry e : getEntries())
			e.parser.addFilters(classes);
		return this;
	}

	/**
	 * Shortcut for calling {@link Parser#addImplClass(Class, Class)} on all parsers in this group.
	 *
	 * @param <T> The interface or abstract class type.
	 * @param interfaceClass The interface or abstract class.
	 * @param implClass The implementation class.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public <T> ParserGroup addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		for (ParserEntry e : getEntries())
			e.parser.addImplClass(interfaceClass, implClass);
		return this;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	/**
	 * Locks this group and all parsers in this group.
	 */
	@Override /* Lockable */
	public ParserGroup lock() {
		super.lock();
		for (ParserEntry e : getEntries())
			e.parser.lock();
		return this;
	}

	/**
	 * Clones this group and all parsers in this group.
	 */
	@Override /* Lockable */
	public ParserGroup clone() throws CloneNotSupportedException {
		ParserGroup c = (ParserGroup)super.clone();
		c.entryMap = new HashMap<String,ParserEntry>();
		c.tempEntries = new LinkedList<ParserEntry>();
		c.entries = null;
		ParserEntry[] e = getEntries();
		for (int i = e.length-1; i >= 0; i--)
			c.append(e[i].parser.clone());
		return c;
	}
}
