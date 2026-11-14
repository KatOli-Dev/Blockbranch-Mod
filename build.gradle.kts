plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

loom {
	accessWidenerPath.set(file("src/main/resources/blockbranch.accesswidener"))
	splitEnvironmentSourceSets()

	mods {
		register("blockbranch") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

// Optional version-specific internals can be omitted while updating Minecraft.
if (providers.gradleProperty("renderPatches").getOrElse("true") == "false") {
	sourceSets.named("client") { java.exclude("blockbranch/client/performance/render/patch/**") }
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}

apply(from = "gradle/testing/source-sets.gradle")

apply(from = "gradle/testing/column-smoke.gradle")

// Isolated build configuration for the realistic feature and its fixture world.
apply(from = "gradle/testing/realistic.gradle")
apply(from = "gradle/testing/wildlife.gradle")

apply(from = "gradle/testing/terrain-audit.gradle")
apply(from = "gradle/testing/initial-loading.gradle")

apply(from = "gradle/testing/column-lifecycle.gradle")

apply(from = "gradle/testing/ore-checks.gradle")

apply(from = "gradle/testing/archive-guards.gradle")
apply(from = "gradle/testing/documentation.gradle")

apply(from = "gradle/testing/ring-transition.gradle")
apply(from = "gradle/testing/structures.gradle")
apply(from = "gradle/testing/integrated-walking.gradle")

apply(from = "gradle/testing/historical-readers.gradle")

apply(from = "gradle/testing/overworld-coverage.gradle")
apply(from = "gradle/testing/integrated-overworld.gradle")
apply(from = "gradle/testing/crypt-performance.gradle")
apply(from = "gradle/testing/village-paths.gradle")

apply(from = "gradle/testing/pyramid-visual.gradle")

apply(from = "gradle/testing/mansion-visual.gradle")
apply(from = "gradle/testing/mansion-perimeter-visual.gradle")

apply(from = "gradle/testing/sanctuary-visual.gradle")
apply(from = "gradle/testing/crypt-visual.gradle")

apply(from = "gradle/testing/sanctuary-core.gradle")
apply(from = "gradle/testing/mansion-replacement.gradle")

apply(from = "gradle/testing/daylight.gradle")

apply(from = "gradle/testing/metallurgy.gradle")

apply(from = "gradle/testing/zinc-brass.gradle")

apply(from = "gradle/testing/steel.gradle")

apply(from = "gradle/testing/tungsten.gradle")

apply(from = "gradle/testing/stainless.gradle")

apply(from = "gradle/testing/stainless-equipment.gradle")

apply(from = "gradle/testing/lead.gradle")

apply(from = "gradle/testing/fools-diamond.gradle")

apply(from = "gradle/testing/precious.gradle")

apply(from = "gradle/testing/silver-equipment.gradle")

apply(from = "gradle/testing/platinum-alloy.gradle")

apply(from = "gradle/testing/platinum-equipment.gradle")

apply(from = "gradle/testing/titanium.gradle")

apply(from = "gradle/testing/titanium-equipment.gradle")

apply(from = "gradle/testing/surface-ownership.gradle")
apply(from = "gradle/testing/dry-season.gradle")
apply(from = "gradle/testing/cool-desert.gradle")
apply(from = "gradle/testing/abyssal-traces.gradle")
apply(from = "gradle/testing/deep-basin.gradle")
apply(from = "gradle/testing/land-ice.gradle")
apply(from = "gradle/testing/summer-dry.gradle")
apply(from = "gradle/testing/polar-ice.gradle")
apply(from = "gradle/testing/leaf-litter.gradle")
apply(from = "gradle/testing/warm-xeric.gradle")
apply(from = "gradle/testing/seasonal-forest.gradle")
apply(from = "gradle/testing/boreal-slope.gradle")
apply(from = "gradle/testing/mangrove-roots.gradle")
apply(from = "gradle/testing/lake-reed.gradle")
apply(from = "gradle/testing/birch-crowns.gradle")
apply(from = "gradle/testing/pine-crowns.gradle")
apply(from = "gradle/testing/coastal-oak.gradle")
apply(from = "gradle/testing/broadleaf-crowns.gradle")
apply(from = "gradle/testing/seasonal-crowns.gradle")
apply(from = "gradle/testing/floodplain.gradle")
apply(from = "gradle/testing/montane-forest.gradle")

apply(from = "gradle/testing/magnetite.gradle")

apply(from = "gradle/testing/resonite.gradle")

apply(from = "gradle/testing/uranium.gradle")

apply(from = "gradle/testing/bismuth.gradle")
