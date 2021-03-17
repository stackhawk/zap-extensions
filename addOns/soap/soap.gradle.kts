version = "6"
description = "Imports and scans WSDL files containing SOAP endpoints."

plugins {
    `maven-publish`
}

zapAddOn {
    addOnName.set("SOAP Support")
    zapVersion.set("2.9.0")

    manifest {
        author.set("Alberto (albertov91) + ZAP Dev Team")
        notBeforeVersion.set("2.10.0")
    }

    apiClientGen {
        api.set("org.zaproxy.zap.extension.soap.SoapAPI")
        messages.set(file("src/main/resources/org/zaproxy/zap/extension/soap/resources/Messages.properties"))
    }
}

dependencies {
    implementation("com.predic8:soa-model-core:1.6.0")
    implementation("jakarta.xml.soap:jakarta.xml.soap-api:1.4.2")
    implementation("com.sun.xml.messaging.saaj:saaj-impl:1.5.2")

    testImplementation(project(":testutils"))
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "nest"
            // Only publish via from CodeBuild with the set env vars
            url = uri("""s3://artifacts.${System.getenv("APP_ENV") ?: "sandbox"}.stackhawk.com/${System.getenv("ARTIFACT_PATH") ?: "pushes"}/Nest/""")
            authentication {
                register("awsIm", AwsImAuthentication::class)
            }
        }
    }

    publications {
        register("soap", MavenPublication::class) {
            groupId = "org.zaproxy.addon"
            artifactId = "soap"
            version = version
            from(components["java"])
        }
    }
}

tasks.register("publishToStackHawk") {
    group = "publishing"
    dependsOn(tasks.withType<PublishToMavenRepository>().matching {
        it.repository == publishing.repositories["nest"] &&
                it.publication == publishing.publications["soap"]
    })
}
