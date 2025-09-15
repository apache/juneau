#!/usr/bin/env node
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


const fs = require('fs');
const path = require('path');

// Package abbreviations for {@link} processing
const packageAbbreviations = {
    'oaj': 'org.apache.juneau',
    'oajr': 'org.apache.juneau.rest',
    'oajrc': 'org.apache.juneau.rest.client', 
    'oajrs': 'org.apache.juneau.rest.server',
    'oajrss': 'org.apache.juneau.rest.server.springboot',
    'oajrm': 'org.apache.juneau.rest.mock',
    'oajmc': 'org.apache.juneau.microservice.core',
    'oajmj': 'org.apache.juneau.microservice.jetty',
};

function convertHtmlToMarkdown(htmlContent, sourceFile) {
    let markdown = htmlContent;
    
    // Remove HTML DOCTYPE and html/head/body structure
    markdown = markdown.replace(/<!DOCTYPE[^>]*>/gi, '');
    markdown = markdown.replace(/<html[^>]*>/gi, '');
    markdown = markdown.replace(/<\/html>/gi, '');
    markdown = markdown.replace(/<head>[\s\S]*?<\/head>/gi, '');
    markdown = markdown.replace(/<body[^>]*>/gi, '');
    markdown = markdown.replace(/<\/body>/gi, '');
    
    // Remove HTML comments
    markdown = markdown.replace(/<!--[\s\S]*?-->/gi, '');
    
    // Extract title from HTML or use filename
    let title = 'Documentation';
    const titleMatch = markdown.match(/<title>(.*?)<\/title>/i);
    if (titleMatch) {
        title = titleMatch[1].replace(/^\d+\.\s*/, '');
    } else {
        // Extract title from {title:'...'} patterns
        const juneauTitleMatch = markdown.match(/\{title:\s*['"]([^'"]*)['"]/i);
        if (juneauTitleMatch) {
            title = juneauTitleMatch[1];
        }
    }
    
    // Remove title and meta tags
    markdown = markdown.replace(/<title>.*?<\/title>/gi, '');
    markdown = markdown.replace(/<meta[^>]*>/gi, '');
    markdown = markdown.replace(/<link[^>]*>/gi, '');
    
    // Remove Juneau-specific metadata
    markdown = markdown.replace(/\{title:\s*[^}]*\}/gi, '');
    markdown = markdown.replace(/\{created:\s*[^}]*\}/gi, '');
    markdown = markdown.replace(/\{updated:\s*[^}]*\}/gi, '');
    
    // Remove divs with class="topic" but keep content
    markdown = markdown.replace(/<div\s+class=['"]topic['"][^>]*>/gi, '');
    markdown = markdown.replace(/<\/div>/gi, '');
    
    // Convert HTML entities FIRST, before other processing (except in BXML blocks)
    // BXML blocks need special handling to preserve XML structure
    markdown = markdown.replace(/(<p\s+class=['"][^'"]*\bbxml(?:\s+[^'"]*)['"][^>]*>[\s\S]*?<\/p>)|&lt;/gi, (match, bxmlBlock) => {
        return bxmlBlock || '<';
    });
    markdown = markdown.replace(/(<p\s+class=['"][^'"]*\bbxml(?:\s+[^'"]*)['"][^>]*>[\s\S]*?<\/p>)|&gt;/gi, (match, bxmlBlock) => {
        return bxmlBlock || '>';
    });
    markdown = markdown.replace(/&amp;/g, '&');
    markdown = markdown.replace(/&quot;/g, '"');
    markdown = markdown.replace(/&#39;/g, "'");
    markdown = markdown.replace(/&nbsp;/g, ' ');
    
    // Convert all code block types to proper Markdown with language hints
    // Updated regex to handle width classes like 'bxml w500', 'bcode w800', etc.
    markdown = markdown.replace(/<p\s+class=['"][^'"]*\bb(java|json|xml|bxml|ini|console|uon|urlenc|code)(?:\s+[^'"]*)['"][^>]*>((?:(?!<\/p>)[\s\S])*?)<\/p>/gi, (match, type, codeContent) => {
        
        // SIMPLE & RELIABLE DOCGENERATOR CONVERTER
        // Focus on getting indentation and empty lines right
        
        let lines = codeContent.split('\n');
        let processedLines = [];
        
        for (let line of lines) {
            // Match DocGenerator pipe pattern: "|" + content
            const pipeMatch = line.match(/^\s*\|(.*)$/);
            if (!pipeMatch) {
                // Skip non-pipe lines (but don't skip empty lines that might have pipes)
                if (line.trim() !== '') {
                    continue;
                }
                // For completely empty lines, skip them
                continue;
            }
            
            const afterPipe = pipeMatch[1];
            
            // Handle empty lines (just whitespace after pipe)
            if (afterPipe.trim() === '') {
                processedLines.push('');
                continue;
            }
            
            // Parse indentation and content
            const contentMatch = afterPipe.match(/^(\s*)(.*)$/);
            if (!contentMatch) {
                processedLines.push(afterPipe.trim());
                continue;
            }
            
            const [, indentChars, actualContent] = contentMatch;
            
            // Calculate indentation level
            const tabCount = (indentChars.match(/\t/g) || []).length;
            const spaceCount = indentChars.length;
            let indentLevel = 0;
            
            
            if (tabCount > 0) {
                // Tab-based: |\t = level 0, |\t\t = level 1
                indentLevel = Math.max(0, tabCount - 1);
            } else {
                // Space-based: |<spaces>
                if (spaceCount >= 15) {
                    indentLevel = Math.floor((spaceCount - 7) / 8);
                } else if (spaceCount >= 7) {
                    indentLevel = 0;
                } else {
                    indentLevel = 0;
                }
            }
            
            
            // Apply indentation
            const finalIndentation = '    '.repeat(indentLevel);
            const cleanContent = actualContent.trim();
            processedLines.push(finalIndentation + cleanContent);
        }
        
        // Join lines and preserve structure
        let code = processedLines.join('\n');
        
        
        // Process Juneau tags based on code type
        if (type === 'java') {
            // Java-specific tags
            code = code.replace(/<jk>(.*?)<\/jk>/gi, '$1'); // keywords
            code = code.replace(/<js>(.*?)<\/js>/gi, '$1'); // strings
            code = code.replace(/<jc>(.*?)<\/jc>/gi, '$1'); // comments
            code = code.replace(/<ja>(.*?)<\/ja>/gi, '$1'); // annotations
            code = code.replace(/<jf>(.*?)<\/jf>/gi, '$1'); // fields
            code = code.replace(/<jsm>(.*?)<\/jsm>/gi, '$1'); // static methods
            code = code.replace(/<jm>(.*?)<\/jm>/gi, '$1'); // methods
            code = code.replace(/<jv>(.*?)<\/jv>/gi, '$1'); // variables
            code = code.replace(/<jp>(.*?)<\/jp>/gi, '$1'); // parameters
        } else if (type === 'json') {
            // JSON-specific tags
            code = code.replace(/<jok>(.*?)<\/jok>/gi, '$1'); // JSON object keys
            code = code.replace(/<jov>(.*?)<\/jov>/gi, '$1'); // JSON object values
            code = code.replace(/<joc>(.*?)<\/joc>/gi, '$1'); // JSON comments
        } else if (type === 'xml') {
            // XML-specific tags
            code = code.replace(/<xt>(.*?)<\/xt>/gi, '$1'); // XML tags
        } else if (type === 'ini') {
            // INI/Config-specific tags
            code = code.replace(/<cc>(.*?)<\/cc>/gi, '$1'); // comments
            code = code.replace(/<cs>(.*?)<\/cs>/gi, '$1'); // sections
            code = code.replace(/<ck>(.*?)<\/ck>/gi, '$1'); // keys
            code = code.replace(/<cv>(.*?)<\/cv>/gi, '$1'); // values
        } else if (type === 'uon' || type === 'urlenc') {
            // UON/URL encoding may have string literals
            code = code.replace(/<js>(.*?)<\/js>/gi, '$1'); // strings
        }
        
        // Remove any remaining HTML tags (except for BXML which needs special handling)
        if (type !== 'bxml') {
            code = code.replace(/<[^>]*>/g, '');
        }
        
        // BXML-specific processing: Handle after other tag removal to preserve XML structure
        if (type === 'bxml') {
            // First remove the Juneau-specific tags while preserving their content
            code = code.replace(/<xt>(.*?)<\/xt>/gi, '$1'); // XML tags
            code = code.replace(/<xv>(.*?)<\/xv>/gi, '$1'); // XML values  
            code = code.replace(/<xa>(.*?)<\/xa>/gi, '$1'); // XML attributes
            // Then decode HTML entities to restore XML structure
            code = code.replace(/&lt;/g, '<');
            code = code.replace(/&gt;/g, '>');
            code = code.replace(/&amp;/g, '&');
            code = code.replace(/&quot;/g, '"');
            code = code.replace(/&#39;/g, "'");
            // Remove any remaining HTML tags (but not the XML tags we just created)
            code = code.replace(/<(?!\/?[a-zA-Z][a-zA-Z0-9]*\b[^>]*>)[^>]*>/g, '');
        }
        
        // For regular XML blocks (not bxml), also decode HTML entities
        if (type === 'xml') {
            code = code.replace(/&lt;/g, '<');
            code = code.replace(/&gt;/g, '>');
            code = code.replace(/&amp;/g, '&');
            code = code.replace(/&quot;/g, '"');
            code = code.replace(/&#39;/g, "'");
        }
        
        // Map code types to appropriate language hints for syntax highlighting
        const languageMap = {
            'java': 'java',
            'json': 'json',
            'xml': 'xml',
            'bxml': 'xml',       // BXML blocks are converted to XML syntax highlighting
            'ini': 'ini',
            'console': 'bash',
            'uon': 'javascript',  // Closest approximation
            'urlenc': 'text',
            'code': 'text'
        };
        
        const language = languageMap[type] || 'text';
        
        
        return '\n```' + language + '\n' + code + '\n```\n';
    });
    
    // Convert headings (h1-h6) to markdown
    markdown = markdown.replace(/<h1[^>]*>(.*?)<\/h1>/gi, '# $1');
    markdown = markdown.replace(/<h2[^>]*>(.*?)<\/h2>/gi, '## $1');
    markdown = markdown.replace(/<h3[^>]*>(.*?)<\/h3>/gi, '### $1');
    markdown = markdown.replace(/<h4[^>]*>(.*?)<\/h4>/gi, '#### $1');
    markdown = markdown.replace(/<h5[^>]*>(.*?)<\/h5>/gi, '##### $1');
    markdown = markdown.replace(/<h6[^>]*>(.*?)<\/h6>/gi, '###### $1');
    
    // Convert Java code blocks (look for multi-line code patterns)
    // DISABLED: This was interfering with the main code block conversion above
    // markdown = convertCodeBlocks(markdown);
    
    // Fix malformed code blocks (multiple consecutive ```java blocks)
    // DISABLED FOR DEBUGGING: markdown = markdown.replace(/```\s*\n\s*```java\s*\n/g, '\n');
    
    // Fix code blocks split across multiple p tags by merging adjacent ones
    // DISABLED FOR DEBUGGING: markdown = markdown.replace(/```\n\n```java\n/g, '\n');
    
    // Convert inline code with custom Juneau tags
    markdown = markdown.replace(/<c>(.*?)<\/c>/gi, (match, content) => {
        // Process Juneau-specific tags within code
        content = content.replace(/<jk>(.*?)<\/jk>/gi, '$1'); // keywords
        content = content.replace(/<js>(.*?)<\/js>/gi, '$1'); // strings
        content = content.replace(/<jc>(.*?)<\/jc>/gi, '$1'); // comments
        content = content.replace(/<ja>(.*?)<\/ja>/gi, '$1'); // annotations
        content = content.replace(/<jf>(.*?)<\/jf>/gi, '$1'); // fields
        content = content.replace(/<jsm>(.*?)<\/jsm>/gi, '$1'); // static methods
        content = content.replace(/<jm>(.*?)<\/jm>/gi, '$1'); // methods
        content = content.replace(/<jv>(.*?)<\/jv>/gi, '$1'); // variables
        content = content.replace(/<jp>(.*?)<\/jp>/gi, '$1'); // parameters
        return '`' + content + '`';
    });
    
    // Convert paragraphs
    markdown = markdown.replace(/<p[^>]*>/gi, '\n\n');
    markdown = markdown.replace(/<\/p>/gi, '\n\n');
    
    // Convert line breaks
    markdown = markdown.replace(/<br[^>]*>/gi, '\n');
    
    // Convert bold/strong
    markdown = markdown.replace(/<(b|strong)[^>]*>(.*?)<\/\1>/gi, '**$2**');
    
    // Convert italic/emphasis 
    markdown = markdown.replace(/<(i|em)[^>]*>(.*?)<\/\1>/gi, '*$2*');
    
    // Convert doclinks to TODO links first (before processing special sections)
    markdown = markdown.replace(/<a[^>]*class=['"]doclink['"][^>]*href=['"]#jm\.([^'"]*)['"][^>]*>(.*?)<\/a>/gi, '[TODO: $2](TODO.md)');
    
    // Handle special note list items - convert to note blocks with icons
    markdown = markdown.replace(/<li[^>]*class=['"]note['"][^>]*>([\s\S]*?)<\/li>/gi, '\n:::note\n$1\n:::\n');
    
    // Handle "See Also" sections - convert to Docusaurus info admonition blocks (BEFORE general ul processing)
    markdown = markdown.replace(/<ul[^>]*class=['"]seealso['"][^>]*>([\s\S]*?)<\/ul>/gi, (match, content) => {
        // Extract the link content
        const linkMatch = content.match(/<li[^>]*class=['"]link['"][^>]*>([\s\S]*?)<\/li>/gi);
        if (linkMatch) {
            let seeAlsoContent = '\n:::info See Also\n\n';
            linkMatch.forEach(link => {
                const linkContent = link.replace(/<li[^>]*class=['"]link['"][^>]*>([\s\S]*?)<\/li>/gi, '$1');
                seeAlsoContent += `🔗 ${linkContent}\n\n`;
            });
            seeAlsoContent += ':::\n';
            return seeAlsoContent;
        }
        return '\n:::info See Also\n\n' + content + '\n\n:::\n';
    });
    
    // Convert simple lists (ul/ol) 
    markdown = markdown.replace(/<ul[^>]*>/gi, '\n');
    markdown = markdown.replace(/<\/ul>/gi, '\n');
    markdown = markdown.replace(/<ol[^>]*>/gi, '\n');
    markdown = markdown.replace(/<\/ol>/gi, '\n');
    
    // Handle proper <li>...</li> pairs first
    markdown = markdown.replace(/<li[^>]*>(.*?)<\/li>/gi, '- $1\n');
    
    // Handle standalone <li> tags without closing tags (common in Juneau docs)
    markdown = markdown.replace(/<li[^>]*>([^<]*?)(?=\n\s*<li|$)/gi, '- $1\n');
    markdown = markdown.replace(/<li[^>]*>([^<]*?)(?=\n)/gi, '- $1\n');
    
    // Clean up stray HTML tags that might be left over
    markdown = markdown.replace(/<\/?li[^>]*>/gi, ''); // Remove any remaining <li> or </li> tags
    markdown = markdown.replace(/<\/?[a-z]+[^>]*>/gi, ''); // Remove any remaining HTML tags
    
    // Convert general links
    markdown = markdown.replace(/<a\s+href=['"]([^'"]*?)['"][^>]*>(.*?)<\/a>/gi, '[$2]($1)');
    
    // Process {@link} tags for Javadoc-style links
    markdown = processJuneauLinks(markdown);
    
    // Convert Java trees to custom elements (must come before convertClassHierarchies)
    markdown = convertJavaTreeToCustomElements(markdown);
    
    // Convert class hierarchy trees 
    markdown = convertClassHierarchies(markdown);
    
    // Convert tables (basic support)
    markdown = convertTables(markdown);
    
    // Clean up whitespace and stray characters
    markdown = markdown.replace(/\n\s*\n\s*\n/g, '\n\n'); // Multiple newlines to double
    markdown = markdown.replace(/^\s*\|\s*/gm, ''); // Remove stray | prefixes
    
    // Fix quote issues in JSON strings (common conversion artifact)
    markdown = markdown.replace(/"\\"([^"]*)\\""/g, '"$1"'); // Fix escaped quotes in JSON
    markdown = markdown.replace(/"\\"([^"]*)"([^"]*)\\""/g, '"$1"$2"'); // Fix partial escapes
    
    // IMPROVED: Escape curly braces that might be interpreted as JSX expressions (but not in code blocks)
    // Handle nested braces by escaping ALL individual braces outside code blocks
    markdown = markdown.replace(/[{}]/g, (match, offset, string) => {
        // Check if we're inside a code block by counting ``` markers before this position
        const beforeMatch = string.substring(0, offset);
        const codeBlockMarkers = (beforeMatch.match(/```/g) || []).length;
        
        // If odd number of ``` markers, we're inside a code block - don't escape
        if (codeBlockMarkers % 2 === 1) {
            return match; // Keep original curly braces in code blocks
        }
        
        // Escape individual braces outside code blocks
        return '\\' + match;
    });
    
    // Remove leading whitespace, but preserve indentation in code blocks
    markdown = markdown.replace(/^\s+/gm, (match, offset, string) => {
        const beforeMatch = string.substring(0, offset);
        const codeBlockMarkers = (beforeMatch.match(/```/g) || []).length;
        
        // If odd number of ``` markers, we're inside a code block - preserve indentation
        if (codeBlockMarkers % 2 === 1) {
            return match; // Keep original indentation in code blocks
        }
        
        // Remove leading whitespace outside code blocks
        return '';
    });
    markdown = markdown.replace(/[ \t]+$/gm, ''); // Trailing whitespace on lines
    markdown = markdown.trim();
    
    // Add frontmatter
    const frontmatter = `---
title: "${title}"
---

`;
    
    return frontmatter + markdown;
}

function convertCodeBlocks(content) {
    // Look for code patterns that span multiple lines with | prefix (from DocGenerator)
    content = content.replace(/(\|[\s\S]*?)(?=\n\n|\n[^|]|$)/g, (match) => {
        // Check if this looks like code (contains Java keywords, semicolons, etc.)
        if (match.includes('public ') || match.includes('private ') || match.includes('class ') || 
            match.includes('import ') || match.includes('return ') || match.includes('void ') ||
            match.includes(';') || match.includes('new ') || match.includes('@Override') ||
            match.includes('<jk>') || match.includes('<js>') || match.includes('<jc>')) {
            
            // Remove the | prefix (used for HTML indentation) and convert to code block
            let code = match.replace(/^\|\s*/gm, '').trim();
            
            // Process Juneau tags in code
            code = code.replace(/<jk>(.*?)<\/jk>/gi, '$1'); // keywords
            code = code.replace(/<js>(.*?)<\/js>/gi, '$1'); // strings
            code = code.replace(/<jc>(.*?)<\/jc>/gi, '$1'); // comments
            code = code.replace(/<ja>(.*?)<\/ja>/gi, '$1'); // annotations
            code = code.replace(/<jf>(.*?)<\/jf>/gi, '$1'); // fields
            code = code.replace(/<jsm>(.*?)<\/jsm>/gi, '$1'); // static methods
            code = code.replace(/<jm>(.*?)<\/jm>/gi, '$1'); // methods
            code = code.replace(/<jv>(.*?)<\/jv>/gi, '$1'); // variables
            code = code.replace(/<jp>(.*?)<\/jp>/gi, '$1'); // parameters
            
            return '\n```java\n' + code + '\n```\n';
        }
        return match; // Not code, leave as is
    });
    
    return content;
}

function processJuneauLinks(content) {
    const javadocBaseUrl = '../apidocs';
    
    function expandPackageAbbreviations(className) {
        for (const [abbrev, fullPackage] of Object.entries(packageAbbreviations)) {
            if (className.startsWith(abbrev + '.')) {
                return className.replace(abbrev + '.', fullPackage + '.');
            }
        }
        return className;
    }
    
    // Process {@link package.Class#method method} patterns
    content = content.replace(/\{@link\s+([a-zA-Z0-9_.]+)#([a-zA-Z0-9_()]+)(?:\s+([^}]+))?\}/g, (match, className, method, displayText) => {
        const expandedClass = expandPackageAbbreviations(className);
        const classPath = expandedClass.replace(/\./g, '/');
        const display = displayText || `${className.split('.').pop()}#${method}`;
        return `[${display}](${javadocBaseUrl}/${classPath}.html#${method})`;
    });

    // Process {@link package.Class Class} patterns
    content = content.replace(/\{@link\s+([a-zA-Z0-9_.]+)(?:\s+([^}]+))?\}/g, (match, className, displayText) => {
        const expandedClass = expandPackageAbbreviations(className);
        const classPath = expandedClass.replace(/\./g, '/');
        const display = displayText || className.split('.').pop();
        return `[${display}](${javadocBaseUrl}/${classPath}.html)`;
    });

    return content;
}

function convertClassHierarchies(content) {
    // Look for class hierarchy patterns in HTML and convert to markdown with CSS classes
    content = content.replace(/<ul\s+class=['"](?:.*\s)?javatree(?:\s.*)?['"][^>]*>([\s\S]*?)<\/ul>/gi, (match, listContent) => {
        let converted = '\n<div class="javatree">\n\n';
        
        // Convert list items with class attributes
        listContent = listContent.replace(/<li\s+class=['"]([^'"]*?)['"][^>]*>(.*?)<\/li>/gi, (match, className, content) => {
            // Clean up nested HTML
            content = content.replace(/<[^>]*>/g, '');
            content = content.trim();
            return `- <li class="${className}">${content}</li>`;
        });
        
        converted += listContent;
        converted += '\n\n</div>\n';
        return converted;
    });
    
    return content;
}

function convertJavaTreeToCustomElements(content) {
    // Convert Java tree HTML structures to custom element format
    console.log('🔍 convertJavaTreeToCustomElements called, content length:', content.length);
    
    const result = content.replace(/<ul\s+class=['"](?:.*\s)?javatree(?:\s.*)?['"][^>]*>([\s\S]*?)<\/ul>/gi, (match, listContent) => {
        try {
            console.log('Converting Java tree:', match.substring(0, 100) + '...');
            const lines = parseJavaTreeToLines(listContent, 0);
            return '\n' + lines.join('\n') + '\n';
        } catch (error) {
            console.error('Error converting Java tree:', error);
            return match;
        }
    });
    
    console.log('🔍 convertJavaTreeToCustomElements finished');
    return result;
}

function parseJavaTreeToLines(html, depth) {
    const lines = [];
    const depthPrefix = '>'.repeat(depth);
    
    // Parse properly nested <li> elements 
    let pos = 0;
    while (pos < html.length) {
        const liMatch = html.substring(pos).match(/<li\s+class=['"]([^'"]*?)['"][^>]*>/);
        if (!liMatch) break;
        
        const fullLiStart = pos + liMatch.index;
        const className = liMatch[1];
        const liOpenEnd = fullLiStart + liMatch[0].length;
        
        // Find the matching </li> tag, accounting for nested <li> elements
        let liEnd = findMatchingLiClose(html, liOpenEnd);
        if (liEnd === -1) {
            // If no closing </li> found, take until next <li> or end
            const nextLi = html.substring(liOpenEnd).search(/<li\s+class=['"][^'"]*['"][^>]*>/);
            liEnd = nextLi !== -1 ? liOpenEnd + nextLi : html.length;
        }
        
        const content = html.substring(liOpenEnd, liEnd).trim();
        
        // Extract the main content before any nested <ul>
        const ulIndex = content.indexOf('<ul');
        const mainContent = ulIndex !== -1 ? content.substring(0, ulIndex).trim() : content.trim();
        
        // Process {@link} tags and convert to markdown links
        const processedContent = processJavaTreeContent(mainContent);
        
        // Map class names to custom elements
        const elementType = mapClassToElement(className);
        
        // Handle javatreec (condensed) lists
        if (content.includes("class='javatreec'") || content.includes('class="javatreec"')) {
            const condensedItems = extractCondensedItems(content);
            if (condensedItems.length > 0) {
                const condensedLine = depthPrefix + '>' + condensedItems.join('&nbsp;&nbsp;');
                lines.push(condensedLine);
                pos = liEnd + 5; // Skip past </li>
                continue;
            }
        }
        
        // Add the main element
        lines.push(`${depthPrefix}<${elementType}>${processedContent}</${elementType}>`);
        
        // Process nested <ul> elements
        const nestedUls = content.match(/<ul[^>]*>([\s\S]*?)<\/ul>/gi);
        if (nestedUls) {
            nestedUls.forEach(nestedUl => {
                const nestedContent = nestedUl.replace(/^<ul[^>]*>/, '').replace(/<\/ul>$/, '');
                const nestedLines = parseJavaTreeToLines(nestedContent, depth + 1);
                lines.push(...nestedLines);
            });
        }
        
        pos = liEnd + 5; // Skip past </li>
    }
    
    return lines;
}

function findMatchingLiClose(html, startPos) {
    let depth = 1;
    let pos = startPos;
    
    while (pos < html.length && depth > 0) {
        const nextLiOpen = html.substring(pos).search(/<li[^>]*>/);
        const nextLiClose = html.substring(pos).search(/<\/li>/);
        
        if (nextLiClose === -1) return -1;
        
        if (nextLiOpen !== -1 && nextLiOpen < nextLiClose) {
            depth++;
            pos += nextLiOpen + 4;
        } else {
            depth--;
            if (depth === 0) {
                return pos + nextLiClose;
            }
            pos += nextLiClose + 5;
        }
    }
    
    return -1;
}

function mapClassToElement(className) {
    const mapping = {
        'jac': 'java-abstract-class',
        'jc': 'javac-class', 
        'jic': 'java-interface',
        'ja': 'java-annotation',
        'je': 'java-enum',
        'jm': 'java-method',
        'jmp': 'java-method-private',
        'jma': 'java-method-annotation',
        'jf': 'java-field',
        'jfp': 'java-field-private'
    };
    
    return mapping[className] || 'java-class';
}

function processJavaTreeContent(content) {
    // Process {@link} tags
    content = content.replace(/\{@link\s+([^}]+)\}/gi, (match, linkContent) => {
        const parts = linkContent.split(/\s+/);
        const target = parts[0];
        const text = parts.slice(1).join(' ') || target.split('.').pop();
        
        // Expand oaj abbreviation
        const expandedTarget = target.replace(/^oaj\./, 'org.apache.juneau.');
        const linkPath = expandedTarget.replace(/\./g, '/');
        
        return `[${text}](../apidocs/${linkPath}.html)`;
    });
    
    // Process <c> tags (convert to backticks)
    content = content.replace(/<c>(.*?)<\/c>/gi, '`$1`');
    
    // Process <jk> tags (Java keywords)
    content = content.replace(/<jk>(.*?)<\/jk>/gi, '$1');
    
    // Clean up HTML entities
    content = content.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&nbsp;/g, ' ');
    
    // Remove extra whitespace
    content = content.replace(/\s+/g, ' ').trim();
    
    return content;
}

function extractCondensedItems(content) {
    const items = [];
    const javatreecMatch = content.match(/<ul\s+class=['"]javatreec['"][^>]*>([\s\S]*?)<\/ul>/i);
    
    if (javatreecMatch) {
        const innerContent = javatreecMatch[1];
        const liMatches = innerContent.match(/<li\s+class=['"]([^'"]*?)['"][^>]*>([\s\S]*?)<\/li>/gi);
        
        if (liMatches) {
            liMatches.forEach(liMatch => {
                const classMatch = liMatch.match(/class=['"]([^'"]*?)['"][^>]*>/);
                const contentMatch = liMatch.replace(/^<li[^>]*>/, '').replace(/<\/li>$/, '');
                
                if (classMatch) {
                    const className = classMatch[1];
                    const elementType = mapClassToElement(className);
                    const processedContent = processJavaTreeContent(contentMatch);
                    items.push(`<${elementType}>${processedContent}</${elementType}>`);
                }
            });
        }
    }
    
    return items;
}

function parseJavaTreeNode(html) {
    const nodes = [];
    
    // Clean up the HTML and normalize it
    let normalizedHtml = html.trim();
    
    // Split by <li> tags and process each
    const liParts = normalizedHtml.split(/<li\s+class=['"]([^'"]*?)['"][^>]*>/i);
    
    // Skip the first empty part
    for (let i = 1; i < liParts.length; i += 2) {
        const className = liParts[i];
        let content = liParts[i + 1] || '';
        
        // Find the end of this li content (next <li> or end of string)
        const nextLiIndex = content.search(/<li\s+class=['"][^'"]*['"][^>]*>/i);
        if (nextLiIndex !== -1) {
            content = content.substring(0, nextLiIndex);
        }
        
        const node = parseJavaTreeItem(className, content);
        if (node) {
            nodes.push(node);
        }
    }
    
    return nodes;
}

function parseJavaTreeItem(className, content) {
    try {
        // Extract the main text/link for this item
        const { name, link, type } = extractNodeInfo(className, content);
        
        const node = {
            name: name,
            type: type,
            link: link
        };
        
        // Look for nested <ul> elements for children
        const children = [];
        const nestedUlPattern = /<ul(?:\s+class=['"]([^'"]*?)['"])?[^>]*>([\s\S]*?)<\/ul>/gi;
        let ulMatch;
        
        while ((ulMatch = nestedUlPattern.exec(content)) !== null) {
            const ulClass = ulMatch[1] || '';
            const ulContent = ulMatch[2];
            
            if (ulClass.includes('javatreec')) {
                // This is a condensed children list
                const condensedChildren = parseCondensedChildren(ulContent);
                if (condensedChildren.length > 0) {
                    // Find the last child that can have condensed children
                    if (children.length > 0) {
                        const lastChild = children[children.length - 1];
                        lastChild.childrenCondensed = true;
                        lastChild.children = (lastChild.children || []).concat(condensedChildren);
                    } else {
                        // Apply to current node
                        node.childrenCondensed = true;
                        node.children = (node.children || []).concat(condensedChildren);
                    }
                }
            } else {
                // Regular nested children
                const nestedChildren = parseJavaTreeNode(ulContent);
                children.push(...nestedChildren);
            }
        }
        
        if (children.length > 0) {
            node.children = children;
        }
        
        return node;
        
    } catch (error) {
        console.error('Error parsing Java tree item:', error);
        return null;
    }
}

function parseCondensedChildren(html) {
    const children = [];
    const liPattern = /<li\s+class=['"]([^'"]*?)['"][^>]*>([\s\S]*?)<\/li>/gi;
    let match;
    
    while ((match = liPattern.exec(html)) !== null) {
        const className = match[1];
        const content = match[2];
        
        const { name, link, type } = extractNodeInfo(className, content);
        children.push({
            name: name,
            type: type,
            link: link
        });
    }
    
    return children;
}

function extractNodeInfo(className, content) {
    // Map CSS class to our type system
    const typeMap = {
        'jac': 'java-abstract-class',
        'jc': 'java-class', 
        'jic': 'java-interface',
        'ja': 'java-annotation',
        'je': 'java-enum',
        'jm': 'java-method',
        'jf': 'java-field',
        'jmp': 'java-private-method',
        'jfp': 'java-private-field',
        'jma': 'java-annotation-method'
    };
    
    const type = typeMap[className] || 'java-class';
    
    // Extract the main content before any nested <ul> tags
    const mainContent = content.split('<ul')[0].trim();
    
    // Extract link and name from {@link} patterns
    const linkPattern = /\{@link\s+([^}]+)\}/gi;
    const linkMatch = linkPattern.exec(mainContent);
    
    let name = '';
    let link = '';
    
    if (linkMatch) {
        const linkTarget = linkMatch[1];
        
        // Handle package abbreviations
        let expandedTarget = linkTarget;
        for (const [abbrev, fullPackage] of Object.entries(packageAbbreviations)) {
            expandedTarget = expandedTarget.replace(new RegExp(`^${abbrev}\\.`, 'g'), `${fullPackage}.`);
        }
        
        // Extract class name and method
        const parts = expandedTarget.split('#');
        const classPath = parts[0];
        const methodName = parts[1];
        
        // Get the simple class name
        const classSegments = classPath.split('.');
        name = classSegments[classSegments.length - 1];
        
        // If there's a method, format it properly
        if (methodName) {
            // Clean up method signature from content after the {@link}
            const afterLink = mainContent.replace(/\{@link[^}]+\}/, '').trim();
            const codeMatch = afterLink.match(/<c>(.*?)<\/c>/);
            let returnType = codeMatch ? codeMatch[1] : '';
            
            // Decode HTML entities in return type
            returnType = returnType.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
            
            // Extract the method signature (everything after the return type)
            const methodText = afterLink.replace(/<c>.*?<\/c>/, '').replace(/&nbsp;/g, ' ').trim();
            let methodSig = methodText || methodName;
            
            // Decode HTML entities in method signature
            methodSig = methodSig.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
            
            if (returnType) {
                name = `${returnType} ${methodSig}`;
            } else {
                name = methodSig;
            }
        }
        
        // Convert to documentation link
        const classPathWithSlashes = classPath.replace(/\./g, '/');
        link = `../apidocs/${classPathWithSlashes}.html`;
        if (methodName) {
            // Clean up method name for anchor - just the method name without params
            const cleanMethodName = methodName.split('(')[0];
            link += `#${cleanMethodName}`;
        }
    } else {
        // Fallback: extract text content
        name = mainContent.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim();
        // Decode HTML entities
        name = name.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
    }
    
    return { name, link, type };
}

function generateClassHierarchyComponent(nodes) {
    const indent = '  ';
    
    function nodeToString(node, depth = 1) {
        const baseIndent = indent.repeat(depth);
        let result = `${baseIndent}{ name: "${escapeQuotes(node.name)}", type: "${node.type}"`;
        
        if (node.link) {
            result += `, link: "${node.link}"`;
        }
        
        if (node.childrenCondensed) {
            result += `, childrenCondensed: true`;
        }
        
        if (node.children && node.children.length > 0) {
            result += `,\n${baseIndent}  children: [\n`;
            
            const childStrings = node.children.map(child => nodeToString(child, depth + 2));
            result += childStrings.join(',\n');
            
            result += `\n${baseIndent}  ]`;
        }
        
        result += ' }';
        return result;
    }
    
    const nodeStrings = nodes.map(node => nodeToString(node));
    
    return `<ClassHierarchy nodes={[\n${nodeStrings.join(',\n')}\n]} />`;
}

function escapeQuotes(text) {
    return text.replace(/"/g, '\\"').replace(/'/g, "\\'");
}

function convertTables(content) {
    // Convert HTML tables to Markdown tables
    content = content.replace(/<table[^>]*>([\s\S]*?)<\/table>/gi, (match, tableContent) => {
        let rows = [];
        
        // Extract table rows
        const rowMatches = tableContent.match(/<tr[^>]*>([\s\S]*?)<\/tr>/gi);
        if (!rowMatches) return match; // Keep original if no rows found
        
        let isHeaderProcessed = false;
        let lastCategoryCell = '';
        
        rowMatches.forEach((row, index) => {
            // Extract cells from each row
            const cellMatches = row.match(/<t[hd][^>]*>([\s\S]*?)<\/t[hd]>/gi);
            if (cellMatches) {
                let cells = [];
                let categoryFound = false;
                
                cellMatches.forEach(cell => {
                    // Check for rowspan attribute (indicates category cell)
                    const rowspanMatch = cell.match(/rowspan=['"](\d+)['"]/i);
                    const hasRowspan = rowspanMatch !== null;
                    
                    // Remove HTML tags but preserve structure for lists
                    let content = cell.replace(/<\/?t[hd][^>]*>/gi, '');
                    
                    // Convert links - extract just the text content
                    content = content.replace(/<a[^>]*>([\s\S]*?)<\/a>/gi, '$1');
                    
                    // Convert nested lists to simple text with bullet points
                    content = content.replace(/<ul[^>]*>([\s\S]*?)<\/ul>/gi, (match, listContent) => {
                        const items = listContent.match(/<li[^>]*>([\s\S]*?)<\/li>/gi) || [];
                        const listItems = items.map(item => {
                            let text = item.replace(/<\/?li[^>]*>/gi, '').replace(/<[^>]*>/g, '').trim();
                            return `• ${text}`;
                        });
                        return listItems.join('<br>');
                    });
                    
                    // Clean up remaining HTML tags
                    content = content.replace(/<[^>]*>/g, '');
                    content = content.replace(/\s+/g, ' ').trim();
                    
                    // Handle empty cells
                    if (!content) content = ' ';
                    
                    if (hasRowspan && !categoryFound) {
                        // This is a category cell with rowspan
                        lastCategoryCell = content;
                        categoryFound = true;
                        cells.push(content);
                    } else {
                        cells.push(content);
                    }
                });
                
                // If this row doesn't have a category cell, use the last one
                if (!categoryFound && lastCategoryCell && index > 0) {
                    cells.unshift(lastCategoryCell);
                }
                
                // Add the row
                if (cells.length > 0) {
                    rows.push('| ' + cells.join(' | ') + ' |');
                }
                
                // Add header separator after first row if it contains <th> tags
                if (!isHeaderProcessed && row.includes('<th')) {
                    const separator = '| ' + cells.map(() => '---').join(' | ') + ' |';
                    rows.push(separator);
                    isHeaderProcessed = true;
                }
            }
        });
        
        return rows.length > 0 ? '\n\n' + rows.join('\n') + '\n\n' : match;
    });
    
    return content;
}

function convertFile(inputFile, outputFile) {
    try {
        const htmlContent = fs.readFileSync(inputFile, 'utf8');
        const markdown = convertHtmlToMarkdown(htmlContent, inputFile);
        
        
        // Ensure output directory exists
        const outputDir = path.dirname(outputFile);
        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }
        
        fs.writeFileSync(outputFile, markdown, 'utf8');
        console.log(`✅ Converted: ${inputFile} -> ${outputFile}`);
        return true;
    } catch (error) {
        console.error(`❌ Error converting ${inputFile}:`, error.message);
        return false;
    }
}

// CLI usage
if (require.main === module) {
    const args = process.argv.slice(2);
    if (args.length < 2) {
        console.log('Usage: node html-to-md-converter.js <input.html> <output.md>');
        process.exit(1);
    }
    
    const [inputFile, outputFile] = args;
    convertFile(inputFile, outputFile);
}

module.exports = { convertHtmlToMarkdown, convertFile };
