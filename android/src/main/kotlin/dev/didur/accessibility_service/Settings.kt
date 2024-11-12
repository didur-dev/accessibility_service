package dev.didur.accessibility_service

import android.content.Context
import android.content.SharedPreferences

object Settings {
    var tirarPrintSolicitacaoCorrida = false

    fun savePreferences(context: Context) {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences(
                "FlutterSharedPreferences",
                Context.MODE_PRIVATE
            )
        sharedPref.edit().putBoolean("settings_tirarPrintSolicitacaoCorrida", tirarPrintSolicitacaoCorrida).apply()
    }

    fun loadPreferences(context: Context) {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences(
                "FlutterSharedPreferences",
                Context.MODE_PRIVATE
            )
        tirarPrintSolicitacaoCorrida = sharedPref.getBoolean("settings_tirarPrintSolicitacaoCorrida", tirarPrintSolicitacaoCorrida)
    }
}