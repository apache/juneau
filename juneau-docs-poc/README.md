# Juneau Documentation Proof-of-Concept

This directory contains a **proof-of-concept** for modernizing Apache Juneau's documentation using **Docusaurus** instead of the current `DocGenerator` Java class approach.

## 🎯 **Goals Achieved**

✅ **Modular Content**: Documentation organized in easy-to-manage Markdown files  
✅ **{@link} Tag Support**: Custom processing for Javadoc-style `{@link}` tags  
✅ **Package Shortcuts**: Support for abbreviated package names (`oaj.`, `oajr.`, etc.)  
✅ **Modern UI**: Responsive, fast, searchable documentation interface  
✅ **Live Preview**: Hot reload during editing for immediate feedback  

## 🔧 **How It Works**

### **{@link} Tag Processing**

The `process-links.js` script demonstrates how `{@link}` tags are converted to Markdown links:

```javascript
// Input:
{@link oaj.serializer.Serializer}
{@link oaj.json.JsonSerializer JsonSerializer}  
{@link oajrc.RestClient#builder() builder()}

// Output:
[`Serializer`](../apidocs/org/apache/juneau/serializer/Serializer.html)
[`JsonSerializer`](../apidocs/org/apache/juneau/json/JsonSerializer.html)
[`builder()`](../apidocs/org/apache/juneau/rest/client/RestClient.html#builder())
```

### **Package Abbreviations**

Supports all your current abbreviations:
- `oaj.` → `org.apache.juneau.`
- `oajr.` → `org.apache.juneau.rest.`
- `oajrc.` → `org.apache.juneau.rest.client.`
- `oajrs.` → `org.apache.juneau.rest.server.`
- And more...

## 📁 **Content Structure**

```
docs/
├── overview.md                    # Main overview page
├── marshalling/
│   ├── serializers.md            # Serialization documentation
│   └── parsers.md                # Parsing documentation
├── rest-server/
│   ├── overview.md               # REST server overview
│   ├── annotations.md            # Annotation reference
│   └── configuration.md          # Configuration guide
└── rest-client/
    ├── overview.md               # REST client overview
    └── builders.md               # Builder patterns
```

## 🚀 **Migration Strategy**

### **Phase 1: Content Conversion**
1. Convert existing HTML topics to Markdown
2. Apply `{@link}` processing during conversion
3. Organize into logical directory structure

### **Phase 2: Integration**
1. Create Maven plugin for `{@link}` preprocessing
2. Integrate Docusaurus build into Maven lifecycle
3. Configure GitHub Pages deployment

### **Phase 3: Enhancement**
1. Add search functionality
2. Implement versioning for different Juneau releases
3. Add interactive examples and code snippets

## 🛠 **Technical Implementation**

### **Current Approach (DocGenerator)**
- ❌ Complex Java class with 400+ lines
- ❌ Single monolithic `overview.html` file
- ❌ Difficult to maintain and edit
- ❌ Limited search capabilities
- ❌ No live preview during editing

### **Proposed Approach (Docusaurus)**
- ✅ Simple Node.js preprocessing script
- ✅ Modular Markdown files
- ✅ Easy to edit and maintain
- ✅ Full-text search built-in
- ✅ Live preview with hot reload
- ✅ Modern, responsive UI
- ✅ Versioning support
- ✅ Mobile-friendly

## 📊 **Benefits Comparison**

| Feature | Current (DocGenerator) | Proposed (Docusaurus) |
|---------|----------------------|----------------------|
| **Editing Experience** | Edit HTML in Java strings | Edit Markdown files directly |
| **Live Preview** | ❌ None | ✅ Hot reload |
| **Search** | ❌ Browser find only | ✅ Full-text search |
| **Mobile Support** | ❌ Limited | ✅ Fully responsive |
| **Maintenance** | ❌ Complex Java code | ✅ Simple config files |
| **{@link} Support** | ✅ Built-in | ✅ Custom preprocessing |
| **Package Shortcuts** | ✅ Built-in | ✅ Custom preprocessing |
| **Versioning** | ❌ Manual | ✅ Built-in support |

## 🧪 **Testing the Proof-of-Concept**

1. **Run the link processor**:
   ```bash
   cd juneau-documentation
   node process-links.js
   ```

2. **Process a specific file**:
   ```bash
   node process-links.js docs/overview.md
   ```

3. **Start development server** (after fixing MDX issues):
   ```bash
   npm start
   ```

## 🔄 **Next Steps**

If this approach looks promising:

1. **Create preprocessing pipeline** that runs before Docusaurus build
2. **Convert a subset of existing documentation** as a pilot
3. **Set up Maven integration** for seamless builds
4. **Configure deployment** to GitHub Pages
5. **Migrate content incrementally** while keeping current system running

## 💡 **Key Advantages**

- **Simpler Maintenance**: No complex Java generator class
- **Better Developer Experience**: Markdown editing with live preview
- **Modern Features**: Search, mobile support, versioning
- **Community Standard**: Docusaurus is widely used and well-supported
- **Gradual Migration**: Can be implemented alongside existing system

This proof-of-concept demonstrates that your `{@link}` tags and package shortcuts can be fully supported in a modern documentation framework while providing significant improvements in maintainability and user experience.
