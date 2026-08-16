plugins {
    id("java-library")
}

group = "org.lwjgl.glfw"

val lwjglVersion = "3.4.1"
val libsDir = file("libs/lwjgl-${lwjglVersion}")
val outputDir = file("../../ZalithLauncher/src/main/assets/components/lwjgl3/${lwjglVersion}")
val excludedModules = listOf(
    "jsr305.jar", "lwjgl.jar", "lwjgl-freetype.jar", "lwjgl-jemalloc.jar",
    "lwjgl-lwjglx.jar", "lwjgl-nanovg.jar", "lwjgl-openal.jar",
    "lwjgl-shaderc.jar", "lwjgl-spvc.jar", "lwjgl-stb.jar",
    "lwjgl-tinyfd.jar", "lwjgl-vma.jar", "lwjgl-vulkan.jar",
    "lwjgl-sdl.jar", "lwjgl-spng.jar"
)

val fatJarDeps by configurations.creating {
    isCanBeResolved = true
    extendsFrom(configurations.runtimeClasspath.get())
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-${lwjglVersion}-merged-modules")
    destinationDirectory.set(outputDir)
    doLast {
        // 被排除的模块原样复制到产物目录（lwjglx 桥接层也在此，仅 LWJGL2 游戏使用）
        fatJarDeps.filter { it.name in excludedModules }.forEach { module ->
            copy {
                from(module)
                into(outputDir)
            }
        }
        // Auto update the version with a timestamp so the project jar gets updated by Pojav
        outputDir.resolve("version").writeText(System.currentTimeMillis().toString())
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
    // LWJGL 3.4 起注解改为 jspecify（3.3.x 时代是 jsr305 jar）
    implementation("org.jspecify:jspecify:1.0.0")
    // 补丁源码（org/lwjgl/util/mapped）编译需要 asm
    compileOnly(fileTree(mapOf("dir" to "compileOnly", "include" to listOf("*.jar"))))
}