plugins {
    `java-library`
    id("dev.isxander.mtk.modrepos")
    `maven-publish`
}

group = "dev.isxander"

java {
    withSourcesJar()
}

repositories {
    mavenCentral()
    isxander()
    terraformersMC()
	nucleoid()
    exclusiveContent {
        forRepository { maven("https://maven.quiltmc.org/repository/release") }
        filter { includeGroupAndSubgroups("org.quiltmc") }
    }
    modrinthApi.exclusive()
    caffeineMC()
}

// The upstream publish repository was removed on 2026-09-25 along with publish-mod.yml.
// Releases go to GitHub by hand with gh, so there is nothing to publish to a maven, and a
// fork has no credentials for that one anyway. isxander() stays in repositories above -
// that is where YACL and the other dependencies COME FROM, and removing it breaks the build.


