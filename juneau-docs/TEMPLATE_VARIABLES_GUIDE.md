# Template Variables Guide

## Overview

You can use template variables in your markdown files that will be automatically replaced with values from your `docusaurus.config.ts` file.

## Available Variables

### 1. `${apiDocsUrl}` - API Documentation URL
- **Value**: `../site/apidocs` (from `customFields.apiDocsUrl`)
- **Usage**: Links to your Javadoc files

### 2. `${juneauVersion}` - Juneau Version
- **Value**: `9.0.1` (from `customFields.juneauVersion`)
- **Usage**: Version references in documentation

## Usage Examples

### API Documentation Links
```markdown
[BeanContext](${apiDocsUrl}/org/apache/juneau/BeanContext.html)
[RestContext](${apiDocsUrl}/org/apache/juneau/rest/RestContext.html)
```

### Version References
```markdown
This documentation is for Juneau version ${juneauVersion}
```

## How It Works

1. **Build Time Processing**: The variables are replaced during the Docusaurus build process
2. **Plugin Processing**: The `remark-custom-vars.js` plugin handles the replacements
3. **Link Checking**: The link checker properly skips these template variables

## Configuration

The variables are defined in `docusaurus.config.ts`:

```typescript
customFields: {
  juneauVersion: '9.0.1',
  apiDocsUrl: '../site/apidocs',
},
```

## Benefits

- **Consistency**: Single source of truth for URLs and versions
- **Maintainability**: Easy to update URLs across all documentation
- **Link Checking**: Template variables are properly handled by the link checker
- **Simplicity**: Clean, focused solution with just the variables you need
