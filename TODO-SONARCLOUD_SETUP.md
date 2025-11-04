# SonarCloud Setup Guide for Apache Juneau

This guide explains how to set up SonarCloud analysis for the Apache Juneau project.

## Prerequisites

1. **SonarCloud Account**: Sign up at [sonarcloud.io](https://sonarcloud.io) using your GitHub account
2. **Organization**: The project should be under the `apache` organization on SonarCloud
3. **Repository Access**: Ensure you have admin access to the Apache Juneau repository

## Setup Steps

### 1. Create SonarCloud Project

1. Go to [SonarCloud](https://sonarcloud.io)
2. Click "Analyze new project"
3. Select "Apache" organization
4. Choose "apache/juneau" repository
5. Set project key to: `apache_juneau`
6. Choose "With GitHub Actions" as the analysis method

### 2. Configure GitHub Secrets

Add the following secret to your GitHub repository:

- **Secret Name**: `SONAR_TOKEN`
- **Secret Value**: Generate from SonarCloud → My Account → Security → Generate Tokens

To add the secret:
1. Go to GitHub repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `SONAR_TOKEN`
4. Value: Copy from SonarCloud token generation

### 3. Verify Configuration Files

The following files have been added to the project:

- **`.github/workflows/sonarcloud.yml`** - GitHub Actions workflow
- **`sonar-project.properties`** - SonarCloud configuration
- **`pom.xml`** - Updated with SonarCloud Maven plugin

### 4. Test the Setup

1. Push the changes to the repository
2. Check the Actions tab to see the SonarCloud workflow running
3. Visit [SonarCloud Dashboard](https://sonarcloud.io/project/overview?id=apache_juneau) to see results

## Configuration Details

### Workflow Triggers

The SonarCloud analysis runs:
- **On every push** to master branch
- **On every pull request** to master branch  
- **Scheduled**: Every Monday at 2:00 AM UTC

### Analysis Configuration

- **Java Version**: 17
- **Coverage**: JaCoCo integration
- **Test Reports**: Surefire reports
- **Exclusions**: Target directories, documentation, examples

### Quality Gate

The workflow includes a quality gate check that will:
- ✅ **Pass**: If code meets quality standards
- ❌ **Fail**: If critical issues are found (but won't block the build)

## Monitoring and Maintenance

### Dashboard Access

- **Public Dashboard**: [sonarcloud.io/project/overview?id=apache_juneau](https://sonarcloud.io/project/overview?id=apache_juneau)
- **Quality Gate Status**: Visible in README badges
- **Security Rating**: Displayed on security page

### Regular Tasks

1. **Review Quality Gate Results**: Check weekly for any failing quality gates
2. **Address Security Issues**: Prioritize security hotspots and vulnerabilities
3. **Monitor Technical Debt**: Track and reduce technical debt over time
4. **Update Dependencies**: Keep SonarCloud Maven plugin updated

## Troubleshooting

### Common Issues

1. **Token Issues**: Ensure `SONAR_TOKEN` secret is correctly set
2. **Build Failures**: Check Maven build logs for Java compilation issues
3. **Coverage Issues**: Verify JaCoCo reports are generated correctly
4. **Timeout Issues**: Large projects may need longer timeout settings

### Getting Help

- **SonarCloud Documentation**: [docs.sonarcloud.io](https://docs.sonarcloud.io)
- **GitHub Actions Logs**: Check workflow execution logs
- **SonarCloud Community**: [community.sonarcloud.io](https://community.sonarcloud.io)

## Benefits

With SonarCloud integration, you get:

- **Automated Quality Analysis**: Every commit and PR
- **Security Vulnerability Detection**: Proactive security scanning
- **Code Coverage Tracking**: Test coverage metrics
- **Technical Debt Monitoring**: Maintainability insights
- **Quality Gate Enforcement**: Consistent code quality standards
- **Public Quality Metrics**: Transparent project health indicators

## Next Steps

After successful setup:

1. **Review Initial Results**: Check the first analysis results
2. **Configure Quality Gates**: Set appropriate quality standards
3. **Address Critical Issues**: Fix high-priority security and quality issues
4. **Monitor Trends**: Track improvement over time
5. **Share Results**: Update documentation with quality metrics
