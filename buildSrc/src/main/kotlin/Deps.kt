object Deps {

    object Version {
        const val kotlin = "1.3.61"
        const val dokka = "0.9.17"
        const val jvmTarget = "1.8"

    }


    const val rxJava = "io.reactivex.rxjava2:rxjava:2.2.15"
    const val kotlinJdk = "stdlib-jdk8"
    const val detektApi = "io.gitlab.arturbosch.detekt:detekt-api:$DETECT_VERSION"
    const val detektTest = "io.gitlab.arturbosch.detekt:detekt-test:$DETECT_VERSION"
    const val junit = "junit:junit:4.12"
    const val truth = "com.google.truth:truth:0.44"
    const val mockito = "org.mockito:mockito-core:2.28.2"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
}

private const val DETECT_VERSION = "1.2.1"
