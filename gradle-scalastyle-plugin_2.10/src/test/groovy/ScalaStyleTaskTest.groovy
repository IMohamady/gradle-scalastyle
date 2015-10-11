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
            scalaStyleConfigFile << scalaStyleConfigurationWithNameRule()
            targetScalaClass << scalaClassPassingScalaStyle()
        when:
        def actual = GradleRunner.create()
                .withProjectDir(targetProjectDir.root)
                .withArguments(':scalaStyle', '--stacktrace')
                .build()

        then:
            actual.task(":scalaStyle").outcome == TaskOutcome.SUCCESS
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
                source = "src/main/scala"
                verbose = true
            }

        """
    }

    def scalaStyleConfigurationWithNameRule() {
        '''
            <scalastyle commentFilter="enabled">
             <name>Scalastyle standard configuration</name>
             <check level="warning" class="org.scalastyle.scalariform.ObjectNamesChecker" enabled="true">
                <parameters>
                    <parameter name="regex">
                        <![CDATA[ [A-Z][A-Za-z]* ]]>
                    </parameter>
                </parameters>
            </check>
            </scalastyle>
        '''
    }

    def scalaClassPassingScalaStyle() {
        '''
        package com.avast.alenkacz.scalastyle.test

        class MainClas {
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
