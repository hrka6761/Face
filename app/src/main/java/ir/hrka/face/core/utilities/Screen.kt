package ir.hrka.face.core.utilities

import ir.hrka.face.core.utilities.Constants.SPLASH_SCREEN


enum class Screen(private val destination: String) {

    Splash(SPLASH_SCREEN);


    operator fun invoke() = destination


    fun appendArg(vararg arg: String): String {
        return buildString {
            append(destination)

            arg.forEach { arg ->
                append("/$arg")
            }
        }
    }
}