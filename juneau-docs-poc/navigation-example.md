# Docusaurus Navigation Panel Example

## 📱 **What Users Will See - 3-Level Navigation**

```
📚 Apache Juneau Documentation
├── 🏠 Home
├── 📖 Getting Started
└── 📋 Documentation
    │
    ├── 📂 1. Overview                           ← Level 1 (Major Section)
    │   ├── 📄 Overview                         ← Level 2 (Section Pages)
    │   ├── 📄 Marshalling
    │   ├── 📄 End-to-End REST
    │   ├── 📄 REST Server
    │   ├── 📄 REST Client
    │   ├── 📄 DTOs
    │   ├── 📄 Config Files
    │   ├── 📄 Fluent Assertions
    │   └── 📄 General Design
    │
    ├── 📂 2. Juneau Marshall                    ← Level 1 (Major Section)
    │   ├── 📄 Overview
    │   ├── 📄 Marshallers
    │   ├── 📄 Serializers and Parsers
    │   ├── 📄 Bean Contexts
    │   ├── 📂 4. Java Beans Support            ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Bean Annotations
    │   │   ├── 📄 Bean Properties
    │   │   ├── 📄 Bean Filters
    │   │   ├── 📄 Bean Dictionaries
    │   │   ├── 📄 Bean Subtyping
    │   │   ├── 📄 Bean Flattening
    │   │   └── 📄 Bean Constructors
    │   ├── 📄 HTTP Part Serializers/Parsers
    │   ├── 📄 Context Settings
    │   ├── 📄 Context Annotations
    │   ├── 📄 JsonMap
    │   ├── 📄 Complex Data Types
    │   ├── 📄 Serializer Sets/Parser Sets
    │   ├── 📂 11. Swaps                        ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 POJO Swaps
    │   │   ├── 📄 One-Way POJO Swaps
    │   │   ├── 📄 Swap Annotations
    │   │   ├── 📄 Per-Media-Type Swaps
    │   │   ├── 📄 Templated Swaps
    │   │   └── 📄 Surrogate Classes
    │   ├── 📄 Dynamically Applied Annotations
    │   ├── 📂 13. Bean Dictionaries            ← Level 2 (Subsection)
    │   │   └── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   ├── 📄 Virtual Beans
    │   ├── 📄 Recursion
    │   ├── 📄 Parsing into Generic Models
    │   ├── 📄 Reading Continuous Streams
    │   ├── 📄 Marshalling URIs
    │   ├── 📄 Jackson Comparison
    │   ├── 📄 POJO Categories
    │   ├── 📂 21. Simple Variable Language     ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 SVL Variables
    │   │   ├── 📄 SVL Methods
    │   │   └── 📄 SVL Widgets
    │   ├── 📄 Encoders
    │   ├── 📄 Object Tools
    │   ├── 📂 24. JSON Details                 ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 JSON Annotation
    │   │   ├── 📄 JSON Schema Support
    │   │   ├── 📄 JSON-B Support
    │   │   └── 📄 JSON Best Practices
    │   ├── 📄 JSON Schema Details
    │   ├── 📂 26. XML Details                  ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 XML Annotation
    │   │   ├── 📄 XML Namespaces
    │   │   ├── 📄 XML Schema Support
    │   │   ├── 📄 XML Best Practices
    │   │   ├── 📄 XML Validation
    │   │   └── 📄 XML Performance
    │   ├── 📂 27. HTML Details                 ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 HTML Annotation
    │   │   ├── 📄 HTML Templates
    │   │   ├── 📄 HTML Widgets
    │   │   ├── 📄 HTML Links
    │   │   ├── 📄 HTML Aside Content
    │   │   ├── 📄 HTML Nav Content
    │   │   └── 📄 HTML Best Practices
    │   ├── 📄 HTML Schema
    │   ├── 📂 29. UON Details                  ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 UON Grammar
    │   │   └── 📄 UON Best Practices
    │   ├── 📂 30. URL-Encoding Details         ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 URL-Encoding Grammar
    │   │   ├── 📄 URL-Encoding Annotation
    │   │   └── 📄 URL-Encoding Best Practices
    │   ├── 📂 31. MsgPack Details              ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   └── 📄 MsgPack Best Practices
    │   ├── 📂 32. OpenAPI Details              ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 OpenAPI Schema
    │   │   └── 📄 OpenAPI Best Practices
    │   └── 📄 Best Practices
    │
    ├── 📂 3. Juneau Marshall RDF               ← Level 1 (Major Section)
    │   └── 📄 Overview
    │
    ├── 📂 4. Juneau DTO                        ← Level 1 (Major Section)
    │   ├── 📄 Overview
    │   ├── 📄 HTML5
    │   ├── 📄 Atom
    │   ├── 📄 Swagger
    │   └── 📄 Swagger UI
    │
    ├── 📂 8. Juneau REST Server                ← Level 1 (Major Section)
    │   ├── 📄 Overview
    │   ├── 📄 Getting Started
    │   ├── 📄 Instantiation
    │   ├── 📄 REST Methods
    │   ├── 📄 Java Method Parameters
    │   ├── 📄 Java Method Return
    │   ├── 📂 6. Request Body                  ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Supported Content Types
    │   │   ├── 📄 Custom Parsers
    │   │   ├── 📄 Parser Properties
    │   │   └── 📄 Request Body Annotations
    │   ├── 📂 7. Response Body                 ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Supported Content Types
    │   │   ├── 📄 Custom Serializers
    │   │   └── 📄 Response Body Annotations
    │   ├── 📂 8. HTTP Parts                    ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Path Variables
    │   │   ├── 📄 Query Parameters
    │   │   ├── 📄 Form Data
    │   │   ├── 📄 Headers
    │   │   └── 📄 Custom HTTP Parts
    │   ├── 📂 9. HTTP Headers                  ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Standard Headers
    │   │   ├── 📄 Custom Headers
    │   │   └── 📄 Header Annotations
    │   ├── 📂 10. Swagger/OpenAPI              ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Swagger UI
    │   │   ├── 📄 OpenAPI Schema
    │   │   └── 📄 Custom Swagger
    │   ├── 📂 11. Security                     ← Level 2 (Subsection)
    │   │   ├── 📄 Overview                     ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Authentication
    │   │   ├── 📄 Authorization
    │   │   ├── 📄 Role-Based Security
    │   │   └── 📄 Security Best Practices
    │   └── 📄 ... (continues with all REST server topics)
    │
    ├── 📂 17. Security                         ← Level 1 (Major Section)
    │   ├── 📄 Overview
    │   ├── 📂 Security Best Practices          ← Level 2 (Subsection)
    │   │   ├── 📄 Input Validation             ← Level 3 (Sub-subsection)
    │   │   ├── 📄 Output Encoding
    │   │   └── 📄 Authentication
    │   └── 📄 Vulnerability Reporting
    │
    └── 📂 18. Migration Guide                  ← Level 1 (Major Section)
        └── 📄 v9.0 Migration Guide
```

## 🎨 **Interactive Features**

### **Collapsible Sections:**
- **Level 1** sections can be collapsed/expanded
- **Level 2** subsections can be collapsed/expanded  
- **Level 3** pages are always visible when parent is expanded

### **Current Page Highlighting:**
```
📂 2. Juneau Marshall                    ← Expanded (user is in this section)
├── 📄 Overview
├── 📄 Marshallers
├── 📄 Serializers and Parsers
├── 📄 Bean Contexts
├── 📂 11. Swaps                        ← Expanded (user is in this subsection)
│   ├── 📄 Overview
│   ├── 📄 POJO Swaps                   ← HIGHLIGHTED (current page)
│   ├── 📄 One-Way POJO Swaps
│   └── 📄 Swap Annotations
└── 📄 Best Practices
```

### **Breadcrumb Navigation:**
```
🏠 Home > 📚 Documentation > 2. Juneau Marshall > 11. Swaps > POJO Swaps
```

### **Search Integration:**
```
🔍 Search documentation...
   ↳ Results appear with section context:
     "POJO Swaps" in Juneau Marshall > Swaps
     "REST Methods" in REST Server
```

## 📱 **Mobile Responsive:**
- **Hamburger menu** on mobile
- **Slide-out navigation** panel
- **Touch-friendly** expand/collapse
- **Breadcrumbs** remain visible

## 🎯 **Navigation Benefits:**

| Feature | Current HTML | New Docusaurus |
|---------|-------------|----------------|
| **Depth** | Manual links | 3+ levels automatic |
| **Search** | Limited | Full-text with context |
| **Mobile** | Basic | Fully responsive |
| **Breadcrumbs** | None | Automatic |
| **Previous/Next** | Manual | Automatic |
| **Collapsing** | None | Smart expand/collapse |

This gives users a **much better navigation experience** while maintaining your logical organization! 🚀
