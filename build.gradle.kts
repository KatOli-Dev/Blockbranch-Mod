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

// Test, QA-run and guard definitions. Order matters: a script may configure tasks an earlier one registered,
// and check-suite must stay last because it reads the final `check` dependencies.
listOf(
    "source-sets", "column-smoke", "realistic", "generator-codec", "wildlife", "terrain-audit", "initial-loading",
    "column-lifecycle", "ore-checks", "archive-guards", "documentation", "ring-transition", "structures",
    "integrated-walking", "historical-readers", "overworld-coverage", "integrated-overworld", "crypt-performance",
    "village-paths", "pyramid-visual", "mansion-visual", "mansion-perimeter-visual", "sanctuary-visual",
    "crypt-visual", "sanctuary-core", "mansion-replacement", "daylight", "metallurgy", "zinc-brass",
    "steel", "tungsten", "stainless", "stainless-equipment", "lead", "fools-diamond", "precious",
    "silver-equipment", "platinum-alloy", "platinum-equipment", "titanium", "titanium-equipment",
    "surface-ownership", "dry-season", "cool-desert", "abyssal-traces", "deep-basin", "land-ice",
    "summer-dry", "polar-ice", "leaf-litter", "warm-xeric", "seasonal-forest", "boreal-slope", "mangrove-roots",
    "lake-reed", "birch-crowns", "pine-crowns", "coastal-oak", "humid-crown", "lowland-surface",
    "broadleaf-crowns", "seasonal-crowns", "floodplain", "montane-forest", "magnetite", "resonite",
    "uranium", "bismuth", "check-suite",
).forEach { apply(from = "gradle/testing/$it.gradle") }
