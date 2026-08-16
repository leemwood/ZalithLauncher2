plugins {
    id("java-library")
}

group = "org.lwjgl.glfw"

val lwjglVersion = "3.3.6"
val libsDir = file("libs/lwjgl-${lwjglVersion}")
val outputDir = file("../../ZalithLauncher/src/main/assets/components/lwjgl3/${lwjglVersion}")
// 需要单独拆出、不合并进主 jar 的模块（如 LWJGL2 的桥接层 lwjglx，只有 LWJGL2 游戏才需要）
val excludedModules = listOf("lwjgl-lwjglx.jar")

val fatJarDeps by configurations.creating {
    isCanBeResolved = true
    extendsFrom(configurations.runtimeClasspath.get())
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-${lwjglVersion}-merged-modules")
    destinationDirectory.set(outputDir)
    // Auto update the version with a timestamp so the project jar gets updated by Pojav
    doLast {
        val versionFile = outputDir.resolve("version")
        versionFile.writeText(System.currentTimeMillis().toString())
        // 将被排除的模块原样复制到产物目录
        fatJarDeps.filter { it.name in excludedModules }.forEach { module ->
            copy {
                from(module)
                into(outputDir)
            }
        }
    }
    from({
        fatJarDeps.filter { it.name !in excludedModules }.map {
            println(it.name)
            if (it.isDirectory) it else zipTree(it)
        }
    })
    exclude("net/java/openjdk/cacio/ctc/**")
    manifest {
        attributes("Manifest-Version" to lwjglVersion)
        attributes("Automatic-Module-Name" to "org.lwjgl")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to libsDir, "include" to listOf("*.jar"))))
    compileOnly(fileTree(mapOf("dir" to "compileOnly", "include" to listOf("*.jar"))))
}