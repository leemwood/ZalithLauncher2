val lwjglVersions = arrayOf("3.3.6", "3.4.1")

tasks.register("jar") {
    dependsOn(lwjglVersions.map { ":LWJGL:lwjgl-$it:jar" })
}