import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
    sourceSets.getByName("main").kotlin.srcDir("../comum")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

compose.desktop {
    application {
        mainClass = "caixa.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "ControleCaixa"
            packageVersion = "1.1.0"
            description = "Controle de Caixa"
            vendor = "Controle de Caixa"
            includeAllModules = true

            windows {
                iconFile.set(project.file("icone.ico"))
                menuGroup = "Controle de Caixa"
                shortcut = true
                menu = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "6f1c2a4e-8b3d-4f5a-9c7e-2d1b0a9e8f71"
            }
        }
    }
}
