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
