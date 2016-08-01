/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import javax.xml.stream.*;

import com.ibm.juno.core.dto.atom.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Customization class that allows a bean (or parts of a bean) to be serialized as XML text or mixed content.
 * <p>
 * 	For example, the ATOM specification allows text elements (e.g. title, subtitle...)
 * 		to be either plain text or XML depending on the value of a <xa>type</xa> attribute.
 * 	The behavior of text escaping thus depends on that attribute.
 *
 * <p class='bcode'>
 * 	<xt>&lt;feed</xt> <xa>xmlns</xa>=<xs>"http://www.w3.org/2005/Atom"</xs><xt>&gt;</xt>
 * 		<xt>&lt;title</xt> <xa>type</xa>=<xs>"html"</xs><xt>&gt;</xt>
 * 			&amp;lt;p&amp;gt;&amp;lt;i&amp;gt;This is the title&amp;lt;/i&amp;gt;&amp;lt;/p&amp;gt;
 * 		<xt>&lt;/title&gt;</xt>
 * 		<xt>&lt;title</xt> <xa>type</xa>=<xs>"xhtml"</xs><xt>&gt;</xt>
 * 			<xt>&lt;div</xt> <xa>xmlns</xa>=<xs>"http://www.w3.org/1999/xhtml"</xs><xt>&gt;</xt>
 * 				<xt>&lt;p&gt;&lt;i&gt;</xt>This is the subtitle<xt>&lt;/i&gt;&lt;/p&gt;</xt>
 * 			<xt>&lt;/div&gt;</xt>
 * 		<xt>&lt;/title&gt;</xt>
 * 	<xt>&lt;/feed&gt;</xt>
 * </p>
 *
 * <p>
 * 	The ATOM {@link Text} class (the implementation for both the <xt>&lt;title&gt;</xt> and <xt>&lt;subtitle&gt;</xt>
 * 		tags shown above) then associates a content handler through the {@link Xml#contentHandler()} annotation
 * 		on the bean property containing the text, like so...
 *
 * <p class='bcode'>
 * 	<ja>@Xml</ja>(format=<jsf>ATTR</jsf>)
 * 	<jk>public</jk> String getType() {
 * 		<jk>return</jk> <jf>type</jf>;
 * 	}
 *
 * 	<ja>@Xml</ja>(format=<jsf>CONTENT</jsf>, contentHandler=TextContentHandler.<jk>class</jk>)
 * 	<jk>public</jk> String getText() {
 * 		<jk>return</jk> <jf>text</jf>;
 * 	}
 *
 * 	<jk>public void</jk> setText(String text) {
 * 		<jk>this</jk>.<jf>text</jf> = text;
 * 	}
 * </p>
 *
 * <p>
 * 	The content handler that transforms the output is shown below...
 *
 * <p class='bcode'>
 * 	<jk>public static class</jk> TextContentHandler <jk>implements</jk> XmlContentHandler&lt;Text&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> parse(XMLStreamReader r, Text text) <jk>throws</jk> Exception {
 * 			String type = text.<jf>type</jf>;
 * 			<jk>if</jk> (type != <jk>null</jk> && type.equals(<js>"xhtml"</js>))
 * 				text.<jf>text</jf> = <jsm>decode</jsm>(readXmlContents(r).trim());
 * 			<jk>else</jk>
 * 				text.<jf>text</jf> = <jsm>decode</jsm>(r.getElementText().trim());
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public void</jk> serialize(XmlSerializerWriter w, Text text) <jk>throws</jk> Exception {
 * 			String type = text.<jf>type</jf>;
 * 			String content = text.<jf>text</jf>;
 * 			<jk>if</jk> (type != <jk>null</jk> && type.equals(<js>"xhtml"</js>))
 * 				w.encodeTextInvalidChars(content);
 * 			<jk>else</jk>
 * 				w.encodeText(content);
 * 		}
 * 	}
 * </p>
 *
 * <h6 class='topic'>Notes</h6>
 * <ul>
 * 	<li>The {@link Xml#contentHandler()} annotation can only be specified on a bean class, or a bean property
 * 		of format {@link XmlFormat#CONTENT}.
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type of the bean
 */
public interface XmlContentHandler<T> {

	/**
	 * Represents <jk>null</jk> on the {@link Xml#contentHandler()} annotation.
	 */
	public static interface NULL extends XmlContentHandler<Object> {}

	/**
	 * Reads XML element content the specified reader and sets the appropriate value on the specified bean.
	 * <p>
	 * 	When this method is called, the attributes have already been parsed and set on the bean.
	 * 	Therefore, if the content handling is different based on some XML attribute (e.g.
	 * 		<code><xa>type</xa>=<xs>"text/xml"</xs></code> vs <code><xa>type</xa>=<xs>"text/plain"</xs></code>)
	 * 		then that attribute value can be obtained via the set bean property.
	 *
	 * @param r The XML stream reader.
	 * 	When called, the reader is positioned on the element containing the text to read.
	 * 	For example, calling <code>r.getElementText()</code> can be called immediately
	 * 	to return the element text if the element contains only characters and whitespace.
	 * 	However typically, the stream is going to contain XML elements that need to
	 * 	be handled special (otherwise you wouldn't need to use an <code>XmlContentHandler</code>
	 * 	to begin with).
	 * @param bean The bean where the parsed contents are going to be placed.
	 * 	Subclasses determine how the content maps to values in the bean.
	 * 	However, typically the contents map to a single property on the bean.
	 * @throws Exception If any problem occurs.  Causes parse to fail.
	 */
	public void parse(XMLStreamReader r, T bean) throws Exception;

	/**
	 * Writes XML element content from values in the specified bean.
	 *
	 * @param w The XML output writer.
	 * 	When called, the XML element/attributes and
	 * 		whitespace/indentation (if enabled) have already been written to the stream.
	 * 	Subclasses must simply write the contents of the element.
	 * @param bean The bean whose values will be converted to XML content.
	 * @throws Exception If any problems occur.  Causes serialize to fail.
	 */
	public void serialize(XmlSerializerWriter w, T bean) throws Exception;

}
