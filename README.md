# Scala Style Gradle Plugin

[![Build Status](https://travis-ci.org/alenkacz/gradle-scalastyle.svg)](https://travis-ci.org/alenkacz/gradle-scalastyle)

Originally forked from https://github.com/ngbinh/gradle-scalastyle-plugin

### Instructions

```
maven repo: http://jcenter.bintray.com/
groupId: org.github.ngbinh.scalastyle
artifactId:  gradle-scalastyle-plugin_2.11
version: 0.7.2
```

Use `artifactId:  gradle-scalastyle-plugin_2.10` if you want to use with Scala `2.10`

```groovy
  apply plugin: 'scalaStyle'
```

Add following dependencies to your buildScript

```groovy
  classpath "org.github.ngbinh.scalastyle:gradle-scalastyle-plugin_2.11:0.7.2"
```

Configure the plugin

```groovy
  scalaStyle {
    configLocation = "/path/to/scalaStyle.xml"
  }

```

Other optional properties are

```groovy
  outputFile  //Default => $buildDir/scala_style_result.xml
  outputEncoding //Default => UTF-8
  failOnViolation //Default => true
  failOnWarning //Default => false
  skip  //Default => false
  verbose //Default => false
  quiet //Default => false
  includeTestSourceDirectory //Default => false
  testConfigLocation //Separate configuration file to be used for test sources
  inputEncoding //Default => UTF-8
  source // Default => "/src/main/scala"
  testSource // Default => "/src/test/scala"
```

#### Full Buildscript Example
```groovy
  apply plugin: 'scalaStyle'

  buildscript {
    repositories {
      jcenter() // only work after gradle 1.7
    }

    dependencies {
      classpath 'org.github.ngbinh.scalastyle:gradle-scalastyle-plugin_2.11:0.7.2'
    }
  }

  scalaStyle {
    configLocation = "mega-project/sub-project/scalastyle_config.xml"
    includeTestSourceDirectory = true
    source = "src/main/scala"
    testSource = "src/test/scala"
  }
```
