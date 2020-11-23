import org.zaproxy.gradle.addon.AddOnStatus

version = "17"
description = "Imports and spiders OpenAPI definitions."

plugins {
    `maven-publish`
}

zapAddOn {
    addOnName.set("OpenAPI Support")
    addOnStatus.set(AddOnStatus.BETA)
    zapVersion.set("2.9.0")

    manifest {
        author.set("ZAP Dev Team plus Joanna Bona, Nathalie Bouchahine, Artur Grzesica, Mohammad Kamar, Markus Kiss, Michal Materniak, Marcin Spiewak, and SDA SE Open Industry Solutions")
        url.set("https://www.zaproxy.org/docs/desktop/addons/openapi-support/")
    }

    apiClientGen {
        api.set("org.zaproxy.zap.extension.openapi.OpenApiAPI")
        messages.set(file("src/main/resources/org/zaproxy/zap/extension/openapi/resources/Messages.properties"))
    }
}

configurations {
    "implementation" {
        // Not needed:
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "org.slf4j", module = "slf4j-ext")
    }
}

dependencies {
    implementation("io.swagger.parser.v3:swagger-parser:2.0.22")
    implementation("io.swagger:swagger-compat-spec-parser:1.0.51") {
        // Not needed:
        exclude(group = "com.github.java-json-tools", module = "json-schema-validator")
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("org.slf4j:slf4j-log4j12:1.7.30") {
        // Provided by ZAP.
        exclude(group = "log4j")
    }

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
        register("openapi", MavenPublication::class) {
            groupId = "org.zaproxy.addon"
            artifactId = "openapi"
            version = version
            from(components["java"])
        }
    }
}

tasks.register("publishToStackHawk") {
    group = "publishing"
    dependsOn(tasks.withType<PublishToMavenRepository>().matching {
        it.repository == publishing.repositories["nest"] &&
                it.publication == publishing.publications["openapi"]
    })
}