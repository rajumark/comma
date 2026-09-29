// Standalone consumer of the PUBLISHED Comma artifacts, used to check a release before and after
// it goes live. It never sees the library source: io.github.rajumark comes only from
//   Maven Local   (default):          ./gradlew ...
//   Maven Central (-PcommaRepo=central): ./gradlew ... -PcommaRepo=central
rootProject.name = "comma-sample"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val fromCentral = providers.gradleProperty("commaRepo").orNull == "central"

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { if (fromCentral) mavenCentral() else mavenLocal() }
            filter { includeGroup("io.github.rajumark") }
        }
        google()
        mavenCentral()
    }
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
