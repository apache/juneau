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
package org.apache.juneau.html.dto;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Superclass for all HTML elements.
 * <p>
 * These are beans that when serialized using {@link HtmlSerializer} generate
 * valid HTML5 elements.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@org.apache.juneau.html.annotation.Html(asXml=true)
@SuppressWarnings("hiding")
public abstract class HtmlElement {

	/**
	 * The children of this element.
	 */
	@Xml(format=MIXED)
	public List<Object> children;

	/**
	 * Adds a child element to this element;
 	 *
	 * @param child
	 * @return This object (for method chaining).
	 */
	public HtmlElement child(Object child) {
		if (children == null)
			children = new LinkedList<Object>();
		children.add(child);
		return this;
	}

	/**
	 * <code>accesskey</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.keylabellist'>List of key labels</a>.
	 * A key label or list of key labels with which to associate the element; each key label represents a keyboard shortcut which UAs can use to activate the element or give focus to the element.
	 * An <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-ordered-tokens'>ordered set of unique space-separated tokens</a>, each of which must be exactly one Unicode code point in length.
	 */
	@Xml(format=ATTR)
	public String accesskey;

	/**
	 * <code>accesskey</code> setter.
	 * @param accesskey - The new value.
	 * @return This object (for method chaining).
	 * @see #accesskey
	 */
	public HtmlElement accesskey(String accesskey) {
		this.accesskey = accesskey;
		return this;
	}

	/**
	 * <code>class</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.tokens'>Set of space-separated tokens</a>.
	 * A name of a classification, or list of names of classifications, to which the element belongs.
	 */
	@Xml(format=ATTR)
	@BeanProperty(name="class")
	public String _class;

	/**
	 * <code>class</code> setter.
	 * @param _class - The new value.
	 * @return This object (for method chaining).
	 * @see #_class
	 */
	public HtmlElement _class(String _class) {
		this._class = _class;
		return this;
	}

	/**
	 * <code>contenteditable</code> - <js>"true"</js> or <js>"false"</js> or <js>""</js> (empty string) or <a href='https://www.w3.org/TR/html-markup/syntax.html#syntax-attr-empty'>empty</a>.
	 * Specifies whether the contents of the element are editable.
	 */
	@Xml(format=ATTR)
	public String contenteditable;

	/**
	 * <code>contenteditable</code> setter.
	 * @param contenteditable - The new value.
	 * @return This object (for method chaining).
	 * @see #contenteditable
	 */
	public HtmlElement contenteditable(String contenteditable) {
		this.contenteditable = contenteditable;
		return this;
	}

	/**
	 * <code>contextmenu</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.idref'>ID reference</a>.
	 * The value of the id attribute on the menu with which to associate the element as a context menu.
	 */
	@Xml(format=ATTR)
	public String contextmenu;

	/**
	 * <code>contextmenu</code> setter.
	 * @param contextmenu - The new value.
	 * @return This object (for method chaining).
	 * @see #contextmenu
	 */
	public HtmlElement contextmenu(String contextmenu) {
		this.contextmenu = contextmenu;
		return this;
	}

	/**
	 * <code>dir</code> - <js>ltr"</js> or <js>"rtl"</js> or <js>"auto"</js>.
	 * Specifies the element’s text directionality.
	 */
	@Xml(format=ATTR)
	public String dir;

	/**
	 * <code>dir</code> setter.
	 * @param dir - The new value.
	 * @return This object (for method chaining).
	 * @see #dir
	 */
	public HtmlElement dir(String dir) {
		this.dir = dir;
		return this;
	}

	/**
	 * <code>draggable</code> - <js>"true"</js> or <js>"false"</js>.
	 * Specifies whether the element is draggable.
	 */
	@Xml(format=ATTR)
	public String draggable;

	/**
	 * <code>draggable</code> setter.
	 * @param draggable - The new value.
	 * @return This object (for method chaining).
	 * @see #draggable
	 */
	public HtmlElement draggable(String draggable) {
		this.draggable = draggable;
		return this;
	}

	/**
	 * <code>dropzone</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.dropzonevalue'>Dropzone value</a>.
	 * Specifies what types of content can be dropped on the element, and instructs the UA about which actions to take with content when it is dropped on the element.
	 *
	 * An <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-unordered-tokens'>unordered set of unique space-separated tokens</a>, each of which is a <a href='https://www.w3.org/TR/html-markup/terminology.html#case-insensitive'>case-insensitive match</a> for one of the following:
	 * <ul>
	 * 	<li><js>"copy"</js> - Indicates that dropping an accepted item on the element will result in a copy of the dragged data.
	 * 	<li><js>"move"</js> - Indicates that dropping an accepted item on the element will result in the dragged data being moved to the new location.
	 * 	<li><js>"link"</js> - Indicates that dropping an accepted item on the element will result in a link to the original data.
	 * 	<li>Any <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-string'>string</a> with three characters or more, beginning with the literal string <js>"string:"</js>.
	 * 		Indicates that Plain Unicode string items, of the type indicated by the part of of the keyword after the <js>"string:"</js> string, can be dropped on this element.
	 * 	<li>Any <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-string'>string</a> with three characters or more, beginning with the literal string <js>"file:"</js>.
	 * 		Indicates that File items, of the type indicated by the part of of the keyword after the <js>"file:"</js> string, can be dropped on this element.
	 * </ul>
	 *
	 * The value must not have more than one of the three tokens <js>"copy"</js>, <js>"move"</js>, or <js>"link"</js>. If none are specified, the element represents a copy dropzone.
	 */
	@Xml(format=ATTR)
	public String dropzone;

	/**
	 * <code>dropzone</code> setter.
	 * @param dropzone - The new value.
	 * @return This object (for method chaining).
	 * @see #dropzone
	 */
	public HtmlElement dropzone(String dropzone) {
		this.dropzone = dropzone;
		return this;
	}

	/**
	 * <code>hidden</code> - <js>"hidden"</js> or <js>""</js> (empty string) or <a href='https://www.w3.org/TR/html-markup/syntax.html#syntax-attr-empty'>empty</a>.
	 * Specifies that the element represents an element that is not yet, or is no longer, relevant.
	 */
	@Xml(format=ATTR)
	public String hidden;

	/**
	 * <code>hidden</code> setter.
	 * @param hidden - The new value.
	 * @return This object (for method chaining).
	 * @see #hidden
	 */
	public HtmlElement hidden(String hidden) {
		this.hidden = hidden;
		return this;
	}

	/**
	 * <code>id</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.id'>ID</a>.
	 * A unique identifier for the element.
	 * There must not be multiple elements in a document that have the same id value.
	 *
	 * Value:  Any <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-string'>string</a>, with the following restrictions:
	 * <ul>
	 * 	<li>Must be at least one character long.
	 * 	<li>Must not contain any <a href='https://www.w3.org/TR/html-markup/terminology.html#space'>space characters</a>.
	 * </ul>
	 */
	@Xml(format=ATTR)
	public String id;

	/**
	 * <code>id</code> setter.
	 * @param id - The new value.
	 * @return This object (for method chaining).
	 * @see #id
	 */
	public HtmlElement id(String id) {
		this.id = id;
		return this;
	}

	/**
	 * <code>lang</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.langcode'>Language tag</a>.
	 * Specifies the primary language for the <a href='https://www.w3.org/TR/html-markup/syntax.html#contents'>contents</a> of the element and for any of the element’s attributes that contain text.
	 * Value:  A valid language tag as defined in <a href='https://www.w3.org/TR/html-markup/references.html#refsBCP47'>[BCP 47]</a>.
	 */
	@Xml(format=ATTR)
	public String lang;

	/**
	 * <code>lang</code> setter.
	 * @param lang - The new value.
	 * @return This object (for method chaining).
	 * @see #lang
	 */
	public HtmlElement lang(String lang) {
		this.lang = lang;
		return this;
	}

	/**
	 * <code>spellcheck</code> - <js>"true"</js> or <js>"false"</js> or <js>""</js> (empty string) or <a href='https://www.w3.org/TR/html-markup/syntax.html#syntax-attr-empty'>empty</a>.
	 * pecifies whether the element represents an element whose <a href='https://www.w3.org/TR/html-markup/syntax.html#contents'>contents</a> are subject to spell checking and grammar checking.
	 */
	@Xml(format=ATTR)
	public String spellcheck;

	/**
	 * <code>spellcheck</code> setter.
	 * @param spellcheck - The new value.
	 * @return This object (for method chaining).
	 * @see #spellcheck
	 */
	public HtmlElement spellcheck(String spellcheck) {
		this.spellcheck = spellcheck;
		return this;
	}

	/**
	 * <code>style</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#data-string'>String</a>.
	 * Specifies zero or more CSS declarations that apply to the element <a href='https://www.w3.org/TR/html-markup/references.html#refsCSS'>[CSS]</a>.
	 */
	@Xml(format=ATTR)
	public String style;

	/**
	 * <code>style</code> setter.
	 * @param style - The new value.
	 * @return This object (for method chaining).
	 * @see #style
	 */
	public HtmlElement style(String style) {
		this.style = style;
		return this;
	}

	/**
	 * <code>tabindex</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.integer'>Integer</a>.
	 * Specifies whether the element represents an element that is is focusable (that is, an element which is part of the sequence of focusable elements in the document), and the relative order of the element in the sequence of focusable elements in the document.
	 */
	@Xml(format=ATTR)
	public String tabindex;

	/**
	 * <code>tabindex</code> setter.
	 * @param tabindex - The new value.
	 * @return This object (for method chaining).
	 * @see #tabindex
	 */
	public HtmlElement tabindex(String tabindex) {
		this.tabindex = tabindex;
		return this;
	}

	/**
	 * <code>title</code> - <a href='https://www.w3.org/TR/html-markup/syntax.html#syntax-attribute-value'>Any value</a>.
	 * Advisory information associated with the element.
	 */
	@Xml(format=ATTR)
	public String title;

	/**
	 * <code>title</code> setter.
	 * @param title - The new value.
	 * @return This object (for method chaining).
	 * @see #title
	 */
	public HtmlElement title(String title) {
		this.title = title;
		return this;
	}

	/**
	 * <code>translate</code> - <js>"yes"</js> or <js>"no"</js>.
	 * Specifies whether an element’s attribute values and contents of its children are to be translated when the page is localized, or whether to leave them unchanged.
	 */
	@Xml(format=ATTR)
	public String translate;

	/**
	 * <code>translate</code> setter.
	 * @param translate - The new value.
	 * @return This object (for method chaining).
	 * @see #translate
	 */
	public HtmlElement translate(String translate) {
		this.translate = translate;
		return this;
	}

	/**
	 * <code>onabort</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Load of element was aborted by the user.
	 */
	@Xml(format=ATTR)
	public String onabort;

	/**
	 * <code>onabort</code> setter.
	 * @param onabort - The new value.
	 * @return This object (for method chaining).
	 * @see #onabort
	 */
	public HtmlElement onabort(String onabort) {
		this.onabort = onabort;
		return this;
	}

	/**
	 * <code>onblur</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element lost focus.
	 */
	@Xml(format=ATTR)
	public String onblur;

	/**
	 * <code>onblur</code> setter.
	 * @param onblur - The new value.
	 * @return This object (for method chaining).
	 * @see #onblur
	 */
	public HtmlElement onblur(String onblur) {
		this.onblur = onblur;
		return this;
	}

	/**
	 * <code>oncanplay</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The UA can resume playback of media data for this video or audio element, but estimates that if playback were to be started now, the video or audio could not be rendered at the current playback rate up to its end without having to stop for further buffering of content.
	 */
	@Xml(format=ATTR)
	public String oncanplay;

	/**
	 * <code>oncanplay</code> setter.
	 * @param oncanplay - The new value.
	 * @return This object (for method chaining).
	 * @see #oncanplay
	 */
	public HtmlElement oncanplay(String oncanplay) {
		this.oncanplay = oncanplay;
		return this;
	}

	/**
	 * <code>oncanplaythrough</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The UA estimates that if playback were to be started now, the video or audio element could be rendered at the current playback rate all the way to its end without having to stop for further buffering.
	 */
	@Xml(format=ATTR)
	public String oncanplaythrough;

	/**
	 * <code>oncanplaythrough</code> setter.
	 * @param oncanplaythrough - The new value.
	 * @return This object (for method chaining).
	 * @see #oncanplaythrough
	 */
	public HtmlElement oncanplaythrough(String oncanplaythrough) {
		this.oncanplaythrough = oncanplaythrough;
		return this;
	}

	/**
	 * <code>onchange</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User committed a change to the value of element (form control).
	 */
	@Xml(format=ATTR)
	public String onchange;

	/**
	 * <code>onchange</code> setter.
	 * @param onchange - The new value.
	 * @return This object (for method chaining).
	 * @see #onchange
	 */
	public HtmlElement onchange(String onchange) {
		this.onchange = onchange;
		return this;
	}

	/**
	 * <code>onclick</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User pressed pointer button down and released pointer button over element, or otherwise activated the pointer in a manner that emulates such an action.
	 */
	@Xml(format=ATTR)
	public String onclick;

	/**
	 * <code>onclick</code> setter.
	 * @param onclick - The new value.
	 * @return This object (for method chaining).
	 * @see #onclick
	 */
	public HtmlElement onclick(String onclick) {
		this.onclick = onclick;
		return this;
	}

	/**
	 * <code>oncontextmenu</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User requested the context menu for element.
	 */
	@Xml(format=ATTR)
	public String oncontextmenu;

	/**
	 * <code>oncontextmenu</code> setter.
	 * @param oncontextmenu - The new value.
	 * @return This object (for method chaining).
	 * @see #oncontextmenu
	 */
	public HtmlElement oncontextmenu(String oncontextmenu) {
		this.oncontextmenu = oncontextmenu;
		return this;
	}

	/**
	 * <code>ondblclick</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User clicked pointer button twice over element, or otherwise activated the pointer in a manner that simulates such an action.
	 */
	@Xml(format=ATTR)
	public String ondblclick;

	/**
	 * <code>ondblclick</code> setter.
	 * @param ondblclick - The new value.
	 * @return This object (for method chaining).
	 * @see #ondblclick
	 */
	public HtmlElement ondblclick(String ondblclick) {
		this.ondblclick = ondblclick;
		return this;
	}

	/**
	 * <code>ondrag</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User is continuing to drag element.
	 */
	@Xml(format=ATTR)
	public String ondrag;

	/**
	 * <code>ondrag</code> setter.
	 * @param ondrag - The new value.
	 * @return This object (for method chaining).
	 * @see #ondrag
	 */
	public HtmlElement ondrag(String ondrag) {
		this.ondrag = ondrag;
		return this;
	}

	/**
	 * <code>ondragend</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User ended dragging element.
	 */
	@Xml(format=ATTR)
	public String ondragend;

	/**
	 * <code>ondragend</code> setter.
	 * @param ondragend - The new value.
	 * @return This object (for method chaining).
	 * @see #ondragend
	 */
	public HtmlElement ondragend(String ondragend) {
		this.ondragend = ondragend;
		return this;
	}

	/**
	 * <code>ondragenter</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User’s drag operation entered element.
	 */
	@Xml(format=ATTR)
	public String ondragenter;

	/**
	 * <code>ondragenter</code> setter.
	 * @param ondragenter - The new value.
	 * @return This object (for method chaining).
	 * @see #ondragenter
	 */
	public HtmlElement ondragenter(String ondragenter) {
		this.ondragenter = ondragenter;
		return this;
	}

	/**
	 * <code>ondragleave</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User’s drag operation left element.
	 */
	@Xml(format=ATTR)
	public String ondragleave;

	/**
	 * <code>ondragleave</code> setter.
	 * @param ondragleave - The new value.
	 * @return This object (for method chaining).
	 * @see #ondragleave
	 */
	public HtmlElement ondragleave(String ondragleave) {
		this.ondragleave = ondragleave;
		return this;
	}

	/**
	 * <code>ondragover</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User is continuing drag operation over element.
	 */
	@Xml(format=ATTR)
	public String ondragover;

	/**
	 * <code>ondragover</code> setter.
	 * @param ondragover - The new value.
	 * @return This object (for method chaining).
	 * @see #ondragover
	 */
	public HtmlElement ondragover(String ondragover) {
		this.ondragover = ondragover;
		return this;
	}

	/**
	 * <code>ondragstart</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User started dragging element.
	 */
	@Xml(format=ATTR)
	public String ondragstart;

	/**
	 * <code>ondragstart</code> setter.
	 * @param ondragstart - The new value.
	 * @return This object (for method chaining).
	 * @see #ondragstart
	 */
	public HtmlElement ondragstart(String ondragstart) {
		this.ondragstart = ondragstart;
		return this;
	}

	/**
	 * <code>ondrop</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User completed drop operation over element.
	 */
	@Xml(format=ATTR)
	public String ondrop;

	/**
	 * <code>ondrop</code> setter.
	 * @param ondrop - The new value.
	 * @return This object (for method chaining).
	 * @see #ondrop
	 */
	public HtmlElement ondrop(String ondrop) {
		this.ondrop = ondrop;
		return this;
	}

	/**
	 * <code>ondurationchange</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The DOM attribute duration on the video or audio element has been updated.
	 */
	@Xml(format=ATTR)
	public String ondurationchange;

	/**
	 * <code>ondurationchange</code> setter.
	 * @param ondurationchange - The new value.
	 * @return This object (for method chaining).
	 * @see #ondurationchange
	 */
	public HtmlElement ondurationchange(String ondurationchange) {
		this.ondurationchange = ondurationchange;
		return this;
	}

	/**
	 * <code>onemptied</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The video or audio element has returned to the uninitialized state.
	 */
	@Xml(format=ATTR)
	public String onemptied;

	/**
	 * <code>onemptied</code> setter.
	 * @param onemptied - The new value.
	 * @return This object (for method chaining).
	 * @see #onemptied
	 */
	public HtmlElement onemptied(String onemptied) {
		this.onemptied = onemptied;
		return this;
	}

	/**
	 * <code>onended</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The end of the video or audio element has been reached.
	 */
	@Xml(format=ATTR)
	public String onended;

	/**
	 * <code>onended</code> setter.
	 * @param onended - The new value.
	 * @return This object (for method chaining).
	 * @see #onended
	 */
	public HtmlElement onended(String onended) {
		this.onended = onended;
		return this;
	}

	/**
	 * <code>onerror</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element failed to load properly.
	 */
	@Xml(format=ATTR)
	public String onerror;

	/**
	 * <code>onerror</code> setter.
	 * @param onerror - The new value.
	 * @return This object (for method chaining).
	 * @see #onerror
	 */
	public HtmlElement onerror(String onerror) {
		this.onerror = onerror;
		return this;
	}

	/**
	 * <code>onfocus</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element received focus.
	 */
	@Xml(format=ATTR)
	public String onfocus;

	/**
	 * <code>onfocus</code> setter.
	 * @param onfocus - The new value.
	 * @return This object (for method chaining).
	 * @see #onfocus
	 */
	public HtmlElement onfocus(String onfocus) {
		this.onfocus = onfocus;
		return this;
	}

	/**
	 * <code>oninput</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User changed the value of element (form control).
	 */
	@Xml(format=ATTR)
	public String oninput;

	/**
	 * <code>oninput</code> setter.
	 * @param oninput - The new value.
	 * @return This object (for method chaining).
	 * @see #oninput
	 */
	public HtmlElement oninput(String oninput) {
		this.oninput = oninput;
		return this;
	}

	/**
	 * <code>oninvalid</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element (form control) did not meet validity constraints.
	 */
	@Xml(format=ATTR)
	public String oninvalid;

	/**
	 * <code>oninvalid</code> setter.
	 * @param oninvalid - The new value.
	 * @return This object (for method chaining).
	 * @see #oninvalid
	 */
	public HtmlElement oninvalid(String oninvalid) {
		this.oninvalid = oninvalid;
		return this;
	}

	/**
	 * <code>onkeydown</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User pressed down a key.
	 */
	@Xml(format=ATTR)
	public String onkeydown;

	/**
	 * <code>onkeydown</code> setter.
	 * @param onkeydown - The new value.
	 * @return This object (for method chaining).
	 * @see #onkeydown
	 */
	public HtmlElement onkeydown(String onkeydown) {
		this.onkeydown = onkeydown;
		return this;
	}

	/**
	 * <code>onkeypress</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User pressed down a key that is associated with a character value.
	 */
	@Xml(format=ATTR)
	public String onkeypress;

	/**
	 * <code>onkeypress</code> setter.
	 * @param onkeypress - The new value.
	 * @return This object (for method chaining).
	 * @see #onkeypress
	 */
	public HtmlElement onkeypress(String onkeypress) {
		this.onkeypress = onkeypress;
		return this;
	}

	/**
	 * <code>onkeyup</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User released a key.
	 */
	@Xml(format=ATTR)
	public String onkeyup;

	/**
	 * <code>onkeyup</code> setter.
	 * @param onkeyup - The new value.
	 * @return This object (for method chaining).
	 * @see #onkeyup
	 */
	public HtmlElement onkeyup(String onkeyup) {
		this.onkeyup = onkeyup;
		return this;
	}

	/**
	 * <code>onload</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element finished loading.
	 */
	@Xml(format=ATTR)
	public String onload;

	/**
	 * <code>onload</code> setter.
	 * @param onload - The new value.
	 * @return This object (for method chaining).
	 * @see #onload
	 */
	public HtmlElement onload(String onload) {
		this.onload = onload;
		return this;
	}

	/**
	 * <code>onloadeddata</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA can render the video or audio element at the current playback position for the first time.
	 */
	@Xml(format=ATTR)
	public String onloadeddata;

	/**
	 * <code>onloadeddata</code> setter.
	 * @param onloadeddata - The new value.
	 * @return This object (for method chaining).
	 * @see #onloadeddata
	 */
	public HtmlElement onloadeddata(String onloadeddata) {
		this.onloadeddata = onloadeddata;
		return this;
	}

	/**
	 * <code>onloadedmetadata</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA has just determined the duration and dimensions of the video or audio element.
	 */
	@Xml(format=ATTR)
	public String onloadedmetadata;

	/**
	 * <code>onloadedmetadata</code> setter.
	 * @param onloadedmetadata - The new value.
	 * @return This object (for method chaining).
	 * @see #onloadedmetadata
	 */
	public HtmlElement onloadedmetadata(String onloadedmetadata) {
		this.onloadedmetadata = onloadedmetadata;
		return this;
	}

	/**
	 * <code>onloadstart</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA has begun looking for media data in the video or audio element.
	 */
	@Xml(format=ATTR)
	public String onloadstart;

	/**
	 * <code>onloadstart</code> setter.
	 * @param onloadstart - The new value.
	 * @return This object (for method chaining).
	 * @see #onloadstart
	 */
	public HtmlElement onloadstart(String onloadstart) {
		this.onloadstart = onloadstart;
		return this;
	}

	/**
	 * <code>onmousedown</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User pressed down pointer button over element.
	 */
	@Xml(format=ATTR)
	public String onmousedown;

	/**
	 * <code>onmousedown</code> setter.
	 * @param onmousedown - The new value.
	 * @return This object (for method chaining).
	 * @see #onmousedown
	 */
	public HtmlElement onmousedown(String onmousedown) {
		this.onmousedown = onmousedown;
		return this;
	}

	/**
	 * <code>onmousemove</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User moved mouse.
	 */
	@Xml(format=ATTR)
	public String onmousemove;

	/**
	 * <code>onmousemove</code> setter.
	 * @param onmousemove - The new value.
	 * @return This object (for method chaining).
	 * @see #onmousemove
	 */
	public HtmlElement onmousemove(String onmousemove) {
		this.onmousemove = onmousemove;
		return this;
	}

	/**
	 * <code>onmouseout</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User moved pointer off boundaries of element.
	 */
	@Xml(format=ATTR)
	public String onmouseout;

	/**
	 * <code>onmouseout</code> setter.
	 * @param onmouseout - The new value.
	 * @return This object (for method chaining).
	 * @see #onmouseout
	 */
	public HtmlElement onmouseout(String onmouseout) {
		this.onmouseout = onmouseout;
		return this;
	}

	/**
	 * <code>onmouseover</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User moved pointer into boundaries of element or one of its descendant elements.
	 */
	@Xml(format=ATTR)
	public String onmouseover;

	/**
	 * <code>onmouseover</code> setter.
	 * @param onmouseover - The new value.
	 * @return This object (for method chaining).
	 * @see #onmouseover
	 */
	public HtmlElement onmouseover(String onmouseover) {
		this.onmouseover = onmouseover;
		return this;
	}

	/**
	 * <code>onmouseup</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User released pointer button over element.
	 */
	@Xml(format=ATTR)
	public String onmouseup;

	/**
	 * <code>onmouseup</code> setter.
	 * @param onmouseup - The new value.
	 * @return This object (for method chaining).
	 * @see #onmouseup
	 */
	public HtmlElement onmouseup(String onmouseup) {
		this.onmouseup = onmouseup;
		return this;
	}

	/**
	 * <code>onmousewheel</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User rotated wheel of mouse or other device in a manner that emulates such an action.
	 */
	@Xml(format=ATTR)
	public String onmousewheel;

	/**
	 * <code>onmousewheel</code> setter.
	 * @param onmousewheel - The new value.
	 * @return This object (for method chaining).
	 * @see #onmousewheel
	 */
	public HtmlElement onmousewheel(String onmousewheel) {
		this.onmousewheel = onmousewheel;
		return this;
	}

	/**
	 * <code>onpause</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User has paused playback of the video or audio element.
	 */
	@Xml(format=ATTR)
	public String onpause;

	/**
	 * <code>onpause</code> setter.
	 * @param onpause - The new value.
	 * @return This object (for method chaining).
	 * @see #onpause
	 */
	public HtmlElement onpause(String onpause) {
		this.onpause = onpause;
		return this;
	}

	/**
	 * <code>onplay</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA has initiated playback of the video or audio element.
	 */
	@Xml(format=ATTR)
	public String onplay;

	/**
	 * <code>onplay</code> setter.
	 * @param onplay - The new value.
	 * @return This object (for method chaining).
	 * @see #onplay
	 */
	public HtmlElement onplay(String onplay) {
		this.onplay = onplay;
		return this;
	}

	/**
	 * <code>onplaying</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Playback of the video or audio element has started.
	 */
	@Xml(format=ATTR)
	public String onplaying;

	/**
	 * <code>onplaying</code> setter.
	 * @param onplaying - The new value.
	 * @return This object (for method chaining).
	 * @see #onplaying
	 */
	public HtmlElement onplaying(String onplaying) {
		this.onplaying = onplaying;
		return this;
	}

	/**
	 * <code>onprogress</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA is fetching media data for the video or audio element.
	 */
	@Xml(format=ATTR)
	public String onprogress;

	/**
	 * <code>onprogress</code> setter.
	 * @param onprogress - The new value.
	 * @return This object (for method chaining).
	 * @see #onprogress
	 */
	public HtmlElement onprogress(String onprogress) {
		this.onprogress = onprogress;
		return this;
	}

	/**
	 * <code>onratechange</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Either the DOM attribute defaultPlaybackRate or the DOM attribute playbackRate on the video or audio element has been updated.
	 */
	@Xml(format=ATTR)
	public String onratechange;

	/**
	 * <code>onratechange</code> setter.
	 * @param onratechange - The new value.
	 * @return This object (for method chaining).
	 * @see #onratechange
	 */
	public HtmlElement onratechange(String onratechange) {
		this.onratechange = onratechange;
		return this;
	}

	/**
	 * <code>onreadystatechange</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element and all its subresources have finished loading.
	 */
	@Xml(format=ATTR)
	public String onreadystatechange;

	/**
	 * <code>onreadystatechange</code> setter.
	 * @param onreadystatechange - The new value.
	 * @return This object (for method chaining).
	 * @see #onreadystatechange
	 */
	public HtmlElement onreadystatechange(String onreadystatechange) {
		this.onreadystatechange = onreadystatechange;
		return this;
	}

	/**
	 * <code>onreset</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The form element was reset.
	 */
	@Xml(format=ATTR)
	public String onreset;

	/**
	 * <code>onreset</code> setter.
	 * @param onreset - The new value.
	 * @return This object (for method chaining).
	 * @see #onreset
	 */
	public HtmlElement onreset(String onreset) {
		this.onreset = onreset;
		return this;
	}

	/**
	 * <code>onscroll</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Element or document view was scrolled.
	 */
	@Xml(format=ATTR)
	public String onscroll;

	/**
	 * <code>onscroll</code> setter.
	 * @param onscroll - The new value.
	 * @return This object (for method chaining).
	 * @see #onscroll
	 */
	public HtmlElement onscroll(String onscroll) {
		this.onscroll = onscroll;
		return this;
	}

	/**
	 * <code>onseeked</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The value of the IDL attribute seeking changed to false (a seek operation on the video or audio element ended).
	 */
	@Xml(format=ATTR)
	public String onseeked;

	/**
	 * <code>onseeked</code> setter.
	 * @param onseeked - The new value.
	 * @return This object (for method chaining).
	 * @see #onseeked
	 */
	public HtmlElement onseeked(String onseeked) {
		this.onseeked = onseeked;
		return this;
	}

	/**
	 * <code>onseeking</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The value of the IDL attribute seeking changed to true, and the seek operation on the video or audio elements is taking long enough that the UA has time to fire the seeking event.
	 */
	@Xml(format=ATTR)
	public String onseeking;

	/**
	 * <code>onseeking</code> setter.
	 * @param onseeking - The new value.
	 * @return This object (for method chaining).
	 * @see #onseeking
	 */
	public HtmlElement onseeking(String onseeking) {
		this.onseeking = onseeking;
		return this;
	}

	/**
	 * <code>onselect</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User selected some text.
	 */
	@Xml(format=ATTR)
	public String onselect;

	/**
	 * <code>onselect</code> setter.
	 * @param onselect - The new value.
	 * @return This object (for method chaining).
	 * @see #onselect
	 */
	public HtmlElement onselect(String onselect) {
		this.onselect = onselect;
		return this;
	}

	/**
	 * <code>onshow</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * User requested the element be shown as a context menu.
	 */
	@Xml(format=ATTR)
	public String onshow;

	/**
	 * <code>onshow</code> setter.
	 * @param onshow - The new value.
	 * @return This object (for method chaining).
	 * @see #onshow
	 */
	public HtmlElement onshow(String onshow) {
		this.onshow = onshow;
		return this;
	}

	/**
	 * <code>onstalled</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA is attempting to fetch media data for the video or audio element, but that data is not forthcoming.
	 */
	@Xml(format=ATTR)
	public String onstalled;

	/**
	 * <code>onstalled</code> setter.
	 * @param onstalled - The new value.
	 * @return This object (for method chaining).
	 * @see #onstalled
	 */
	public HtmlElement onstalled(String onstalled) {
		this.onstalled = onstalled;
		return this;
	}

	/**
	 * <code>onsubmit</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The form element was submitted.
	 */
	@Xml(format=ATTR)
	public String onsubmit;

	/**
	 * <code>onsubmit</code> setter.
	 * @param onsubmit - The new value.
	 * @return This object (for method chaining).
	 * @see #onsubmit
	 */
	public HtmlElement onsubmit(String onsubmit) {
		this.onsubmit = onsubmit;
		return this;
	}

	/**
	 * <code>onsuspend</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * UA is intentionally not currently fetching media data for the video or audio element, but does not yet have the entire contents downloaded.
	 */
	@Xml(format=ATTR)
	public String onsuspend;

	/**
	 * <code>onsuspend</code> setter.
	 * @param onsuspend - The new value.
	 * @return This object (for method chaining).
	 * @see #onsuspend
	 */
	public HtmlElement onsuspend(String onsuspend) {
		this.onsuspend = onsuspend;
		return this;
	}

	/**
	 * <code>ontimeupdate</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * The current playback position of the video or audio element changed either as part of normal playback, or in an especially interesting way (for example, discontinuously).
	 */
	@Xml(format=ATTR)
	public String ontimeupdate;

	/**
	 * <code>ontimeupdate</code> setter.
	 * @param ontimeupdate - The new value.
	 * @return This object (for method chaining).
	 * @see #ontimeupdate
	 */
	public HtmlElement ontimeupdate(String ontimeupdate) {
		this.ontimeupdate = ontimeupdate;
		return this;
	}

	/**
	 * <code>onvolumechange</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Either the DOM attribute volume or the DOM attribute muted on the video or audio element has been changed.
	 */
	@Xml(format=ATTR)
	public String onvolumechange;

	/**
	 * <code>onvolumechange</code> setter.
	 * @param onvolumechange - The new value.
	 * @return This object (for method chaining).
	 * @see #onvolumechange
	 */
	public HtmlElement onvolumechange(String onvolumechange) {
		this.onvolumechange = onvolumechange;
		return this;
	}

	/**
	 * <code>onwaiting</code> - <a href='https://www.w3.org/TR/html-markup/datatypes.html#common.data.functionbody'>functionbody</a>.
	 * Playback of the video or audio element has stopped because the next frame is not yet available (but UA agent expects that frame to become available in due course).
	 */
	@Xml(format=ATTR)
	public String onwaiting;

	/**
	 * <code>onwaiting</code> setter.
	 * @param onwaiting - The new value.
	 * @return This object (for method chaining).
	 * @see #onwaiting
	 */
	public HtmlElement onwaiting(String onwaiting) {
		this.onwaiting = onwaiting;
		return this;
	}
}
