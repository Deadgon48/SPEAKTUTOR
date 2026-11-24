package com.itsx.speaktutor.ui.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.itsx.speaktutor.AppConfig
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

class StartIoAdManager(private val context: Context) {
    private var rewardedVideo: StartAppAd? = null
    private var estaCargado = false
    private var sinInventario = false

    fun precargarVideo() {
        if (!AppConfig.MOSTRAR_ANUNCIOS) return

        sinInventario = false
        rewardedVideo = StartAppAd(context)
        rewardedVideo?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                Log.d("RewardedStartIo", "✅ Video recompensado listo")
                estaCargado = true
                sinInventario = false
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                Log.e("RewardedStartIo", "❌ Video recompensado falló: ${ad?.errorMessage ?: "NO FILL"}")
                estaCargado = false
                sinInventario = true
            }
        })
    }

    fun mostrarVideoRecompensado(onRecompensaObtenida: () -> Unit) {
        if (!AppConfig.MOSTRAR_ANUNCIOS) {
            onRecompensaObtenida()
            return
        }

        if (estaCargado && rewardedVideo != null) {
            rewardedVideo?.setVideoListener {
                Log.d("RewardedStartIo", "🎁 Video completado, otorgando recompensa")
                onRecompensaObtenida()
                estaCargado = false
                precargarVideo()
            }
            rewardedVideo?.showAd()
        } else if (sinInventario) {
            Toast.makeText(
                context,
                "No hay anuncios disponibles en este momento. Desbloqueando recompensa...",
                Toast.LENGTH_SHORT
            ).show()
            onRecompensaObtenida()
            precargarVideo()
        } else {
            Toast.makeText(
                context,
                "Cargando video de recompensa, intenta de nuevo en unos segundos...",
                Toast.LENGTH_SHORT
            ).show()
            precargarVideo()
        }
    }
}