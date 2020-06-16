import org.zaproxy.gradle.addon.AddOnStatus
version = "1.0.0"
description = "Allows Kotlin to be used for ZAP scripting"

zapAddOn {
    addOnName.set("Kotlin Support")
    addOnStatus.set(AddOnStatus.ALPHA)
    zapVersion.set("2.9.0")

    manifest {
        author.set("StackHawk Engineering")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable")
    implementation("org.jetbrains.kotlin:kotlin-script-util")

    testImplementation(project(":testutils"))
}
