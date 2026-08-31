# Usage

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/code-lime/org.lime.core")
    }
}

dependencies {
    implementation 'org.lime:core:1.0'
}
```

## Gradle scripts

Release and alpha versions of the shared Gradle scripts are published through
GitHub Pages. Pin a version for reproducible builds:

```gradle
apply from: 'https://code-lime.github.io/org.lime.core/3.1.5/global.gradle'
apply from: 'https://code-lime.github.io/org.lime.core/3.1.5/version.gradle'
```

`ghcr.gradle` provides Docker-free Maven repository transport through an OCI
registry. Apply a released version of the script, then bind existing Maven
publications and register repositories:

```gradle
apply from: 'https://code-lime.github.io/org.lime.core/<version>/ghcr.gradle'

publishing {
    publications {
        library(MavenPublication) {
            from components.java
        }
    }
}

ghcr.mavenPublication(publishing.publications.library) {
    image = 'ghcr.io/owner/library-maven'
    diagnosticTag = providers.environmentVariable('GITHUB_RUN_ID')
            .map { "run-${it}" }
    source = 'https://github.com/owner/library'
    revision = providers.environmentVariable('GITHUB_SHA')
    created = providers.environmentVariable('SOURCE_DATE_EPOCH')
            .map { java.time.Instant.ofEpochSecond(it.toLong()).toString() }
    credentials {
        username = providers.environmentVariable('GHCR_USERNAME')
        password = providers.environmentVariable('GHCR_TOKEN')
    }
}

ghcr.mavenRepository('library') {
    image = 'ghcr.io/owner/library-maven'
    tag = providers.gradleProperty('versionLibrary')
    destinationDirectory = layout.projectDirectory.dir('.gradle/library-maven')
    group = 'org.example'
    module = 'library'
    credentials {
        username = providers.environmentVariable('GHCR_USERNAME')
        password = providers.environmentVariable('GHCR_TOKEN')
    }
}
```

Push and pull tasks are named `ghcrPushLibrary` and `ghcrPullLibrary`.
The standard `publish` task also depends on each registered GHCR push.
Repository hydration runs automatically before Gradle resolves the configured
module. The plugin never reads environment variables itself: credentials are
optional explicit values and omission delegates to anonymous access or the
local ORAS credential store. `source` and `revision` are required publication
annotations. The local destination must be inside the current Gradle build
root. With `--offline`, the pull succeeds only when both its marker and Maven
layout are already valid. Credentials are never Gradle task inputs and are not
written to the staging layout or hydrated repository marker.
