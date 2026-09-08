plugins {
    application
}

group = "com.retronova"
version = "0.1.3-alpha"

repositories {
    mavenCentral()
    // TinySound e Sheeter não existem no Maven Central: TinySound está sem release
    // desde 2012 e o Sheeter é uma biblioteca interna do time.
    flatDir { dirs("libs") }
}

java {
    // O código usa SequencedCollection (getLast / removeLast), de Java 21.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(files("libs/TinySound.jar", "libs/Sheeter.jar"))

    // Registra o decodificador Ogg Vorbis como Service Provider do javax.sound.
    // O TinySound usa AudioSystem.getAudioInputStream() por baixo, então passa a
    // ler OGG só por esta biblioteca estar no classpath — nenhuma linha de código
    // de áudio precisou mudar por causa do formato.
    runtimeOnly("com.github.trilarion:vorbis-support:1.1.0")

    // json-simple arrasta junit como dependência de compilação, o que é um erro
    // conhecido do POM dele; excluímos para não vazar test scope no runtime.
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }

    // Usado apenas pelo overlay de debug (F3).
    implementation("com.github.oshi:oshi-core:6.12.0")
    // O oshi exige JNA 5.14+. O projeto vinha com a 5.13 vendorizada, e o
    // overlay quebrava com NoSuchMethodError em IsProcessorFeaturePresent.
    // Presa na série 6.x do oshi de propósito: a 7.x troca os artefatos JNA
    // para jna-jpms/jna-platform-jpms, o que invalidaria este pin.
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    implementation("org.slf4j:slf4j-api:2.0.19")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.19")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.retronova.Main"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    // A suíte não deve abrir janela; qualquer teste que toque em AWT roda headless.
    systemProperty("java.awt.headless", "true")
}

tasks.named<JavaExec>("run") {
    // O jogo grava config.json ao lado do executável; sem isto ele iria parar
    // em build/ durante o desenvolvimento.
    workingDir = rootDir

    // `./gradlew run -Pvitrine` liga o modo de revisão de itens: o vendedor
    // oferece todos, o dinheiro não cai e a prateleira não esvazia. Fica numa
    // propriedade e não numa constante do código justamente para não haver o que
    // esquecer ligado. Ver engine/Debugging.java.
    if (project.hasProperty("vitrine")) {
        systemProperty("vitrine", "true")
    }
}

/**
 * Regenera os PNGs de UI a partir do gerador procedural em tools/.
 * Não entra no ciclo de build: os PNGs são versionados, e um artista pode
 * repintá-los à mão sem que o build os sobrescreva.
 */
tasks.register<Exec>("genUiAssets") {
    group = "assets"
    description = "Regera os sprites de UI (botões e ícone) a partir de tools/GenUiAssets.java"
    workingDir = rootDir
    // Modo arquivo-fonte único do java (JEP 330): não precisa entrar no source set
    // nem ser compilado antes.
    val launcher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    commandLine(
        launcher.get().executablePath.asFile.absolutePath,
        "tools/GenUiAssets.java"
    )
}

/**
 * A CI inteira, aqui, antes do push.
 *
 * Espelha `.github/workflows/ci.yml` — ver tools/CiLocal.java para o que ele faz
 * além de repetir os comandos de lá.
 */
tasks.register<Exec>("ci") {
    group = "verification"
    description = "Roda a CI local, a mesma que o hook de pre-push executa"
    workingDir = rootDir
    val launcher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    commandLine(
        launcher.get().executablePath.asFile.absolutePath,
        "tools/CiLocal.java"
    )
}

/**
 * Aponta o git para os hooks versionados.
 *
 * Precisa ser rodado UMA vez por clone: o git não versiona `.git/hooks`, então
 * um hook só vale para quem o instalou. `core.hooksPath` resolve isso apontando
 * para uma pasta que está no repositório — assim o time inteiro recebe o mesmo
 * hook ao clonar, bastando este comando.
 */
tasks.register<Exec>("hooks") {
    group = "verification"
    description = "Instala os hooks de .githooks (roda a CI local antes de cada push)"
    workingDir = rootDir
    commandLine("git", "config", "core.hooksPath", ".githooks")
    doLast {
        println("hooks instalados: o push passa a rodar a CI local antes.")
    }
}

/** Fat-jar executável, para distribuir sem depender do Gradle. */
tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "Gera um jar único e executável com todas as dependências"
    archiveClassifier = "all"
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    // Assinaturas dos jars originais invalidam o jar combinado.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}
