/*
 *    Copyright 2014. Binh Nguyen
 *
 *    Copyright 2013. Muhammad Ashraf
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

class ScalaStyleTaskTest extends Specification {
    @Rule final TemporaryFolder targetProjectDir = new TemporaryFolder()
    File gradleBuildFile
    File scalaStyleConfigFile
    File targetScalaClass

    def setup() {
        gradleBuildFile = targetProjectDir.newFile('build.gradle')
        scalaStyleConfigFile = targetProjectDir.newFile('scalastyle.xml')

        File sourceCodeDir = targetProjectDir.newFolder('src', 'main', 'scala')
        targetScalaClass = new File(sourceCodeDir, 'Main.scala')
    }

    def "succeed on scala project with no violations"() {
        given:
            gradleBuildFile << buildFileWithScalaStyle()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassPassingScalaStyle()
        when:
        def actual = GradleRunner.create()
                .withProjectDir(targetProjectDir.root)
                .withArguments(':scalaStyle', '--stacktrace')
                .build()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.SUCCESS
    }

    def "fail on scala project with rule violation"() {
        given:
            gradleBuildFile << buildFileWithScalaStyle()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassFailingScalaStyle()
        when:
            def actual = GradleRunner.create()
                    .withProjectDir(targetProjectDir.root)
                    .withArguments(':scalaStyle', '--stacktrace')
            .buildAndFail()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.FAILED
    }

    def "not fail on scala project with rule violation when failOnViolation is turned off"() {
        given:
            gradleBuildFile << buildFileWithScalaStyleAndTurnedOffFailOnViolation()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassFailingScalaStyle()
        when:
            def actual = GradleRunner.create()
                    .withProjectDir(targetProjectDir.root)
                    .withArguments(':scalaStyle', '--stacktrace')
                    .build()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.SUCCESS
    }

    def "not fail when skip is turned on"() {
        given:
            gradleBuildFile << buildFileWithScalaStyleAndSkipTurnedOn()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassFailingScalaStyle()
        when:
            def actual = GradleRunner.create()
                    .withProjectDir(targetProjectDir.root)
                    .withArguments(':scalaStyle', '--stacktrace')
                    .build()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.SUCCESS
    }

    def "find failures in test sources if enabled"() {
        given:
            gradleBuildFile << buildFileWithScalaStyleAlsoForTests()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassPassingScalaStyle()
            def testFile = createTestFile()
            testFile << scalaClassFailingScalaStyle()
        when:
            def actual = GradleRunner.create()
                    .withProjectDir(targetProjectDir.root)
                    .withArguments(':scalaStyle', '--stacktrace')
                    .buildAndFail()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.FAILED
    }

    def "not find failures in test sources when disabled"() {
        given:
        gradleBuildFile << buildFileWithScalaStyleAlsoForTestsButDisabled()
        scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
        targetScalaClass << scalaClassPassingScalaStyle()
        def testFile = createTestFile()
        testFile << scalaClassFailingScalaStyle()
        when:
        def actual = GradleRunner.create()
                .withProjectDir(targetProjectDir.root)
                .withArguments(':scalaStyle', '--stacktrace')
                .build()

        then:
        actual.task(":scalaStyle").outcome == TaskOutcome.SUCCESS
    }

    def "be able to find source in the default folder even if not provided in configuration"() {
        given:
            gradleBuildFile << buildFileWithScalaStyleWithoutSourceSpecified()
            scalaStyleConfigFile << scalaStyleConfigurationWithSingleRule()
            targetScalaClass << scalaClassFailingScalaStyle()
        when:
            def actual = GradleRunner.create()
                    .withProjectDir(targetProjectDir.root)
                    .withArguments(':scalaStyle', '--stacktrace')
                    .buildAndFail()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.FAILED
    }

    def createTestFile() {
        File testSourceCodeDir = targetProjectDir.newFolder('src', 'test', 'scala')
        new File(testSourceCodeDir, 'MainTest.scala')
    }

    def buildFileWithScalaStyle() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
                includeTestSourceDirectory = false
                source = "/src/main/scala"
                verbose = true
            }

        """
    }

    def buildFileWithScalaStyleAndTurnedOffFailOnViolation() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
                includeTestSourceDirectory = false
                source = "src/main/scala"
                verbose = true
                failOnViolation = false
            }

        """
    }

    def buildFileWithScalaStyleAndSkipTurnedOn() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
                includeTestSourceDirectory = false
                skip = true
                source = "src/main/scala"
                verbose = true
            }

        """
    }

    def buildFileWithScalaStyleAlsoForTests() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
                includeTestSourceDirectory = true
                source = "src/main/scala"
                testSource = "src/test/scala"
                verbose = true
            }

        """
    }

    def buildFileWithScalaStyleAlsoForTestsButDisabled() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
                includeTestSourceDirectory = false
                source = "src/main/scala"
                testSource = "src/test/scala"
                verbose = true
            }

        """
    }

    def buildFileWithScalaStyleWithoutSourceSpecified() {
        def pluginClasspath = sourceCodeOfThisPluginClasspath()

        """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'cz.alenkacz.scalastyle'

            scalaStyle {
                configLocation = "${scalaStyleConfigFile.getAbsolutePath().replace('\\', '\\\\')}"
            }

        """
    }

    def scalaStyleConfigurationWithSingleRule() {
        '''
            <scalastyle commentFilter="enabled">
             <name>Scalastyle standard configuration</name>
             <check level="error" class="org.scalastyle.scalariform.EmptyClassChecker" enabled="true"/>
            </scalastyle>
        '''
    }

    def scalaClassPassingScalaStyle() {
        '''
        package com.avast.alenkacz.scalastyle.test

        class Main
        '''
    }

    def scalaClassFailingScalaStyle() {
        '''
        package com.avast.alenkacz.scalastyle.test

        class Main {
        }
        '''
    }

    /*
    This is needed to get the current plugin to the classpath. See https://docs.gradle.org/current/userguide/test_kit.html
     */
    def sourceCodeOfThisPluginClasspath() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")
    }
}
