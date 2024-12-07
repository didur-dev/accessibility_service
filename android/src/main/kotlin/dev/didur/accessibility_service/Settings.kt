package dev.didur.accessibility_service

import android.content.Context
import android.content.SharedPreferences

object Settings {
    var tirarPrintSolicitacaoCorrida = false

    var leituraOcrAtivado = true
    var leituraAcessibilidadeAtivado = true

    var recusaAutomatica = false

    var ocrBinarize = false
    var ocrUpscale = false
    var ocrIntervalo = 2000L

    fun savePreferences(context: Context) {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences(
                "FlutterSharedPreferences",
                Context.MODE_PRIVATE
            )
        sharedPref.edit().putString("settings_tirarPrintSolicitacaoCorrida", tirarPrintSolicitacaoCorrida.toString()).apply()

        sharedPref.edit().putString("settings_leituraOcrAtivado", leituraOcrAtivado.toString()).apply()
        sharedPref.edit().putString("settings_leituraAcessibilidadeAtivado", leituraAcessibilidadeAtivado.toString()).apply()

        sharedPref.edit().putString("settings_recusaAutomatica", recusaAutomatica.toString()).apply()

        sharedPref.edit().putString("settings_ocrBinarize", ocrBinarize.toString()).apply()
        sharedPref.edit().putString("settings_ocrUpscale", ocrUpscale.toString()).apply()
        sharedPref.edit().putString("settings_ocrIntervalo", ocrIntervalo.toString()).apply()
    }

    fun loadPreferences(context: Context) {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences(
                "FlutterSharedPreferences",
                Context.MODE_PRIVATE
            )
        tirarPrintSolicitacaoCorrida = sharedPref.getString("settings_tirarPrintSolicitacaoCorrida", tirarPrintSolicitacaoCorrida.toString()).toBoolean()

        leituraOcrAtivado = sharedPref.getString("settings_leituraOcrAtivado", leituraOcrAtivado.toString()).toBoolean()
        leituraAcessibilidadeAtivado = sharedPref.getString("settings_leituraAcessibilidadeAtivado", leituraAcessibilidadeAtivado.toString()).toBoolean()

        recusaAutomatica = sharedPref.getString("settings_recusaAutomatica", recusaAutomatica.toString()).toBoolean()

        ocrBinarize = sharedPref.getString("settings_ocrBinarize", ocrBinarize.toString()).toBoolean()
        ocrUpscale = sharedPref.getString("settings_ocrUpscale", ocrUpscale.toString()).toBoolean()
        ocrIntervalo = sharedPref.getString("settings_ocrIntervalo", ocrIntervalo.toString())!!.toLong()
    }
}