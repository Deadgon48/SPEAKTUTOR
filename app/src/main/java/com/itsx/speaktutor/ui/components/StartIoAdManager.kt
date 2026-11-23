package com.itsx.speaktutor.ui.components

import android.content.Context
import android.widget.Toast
import com.itsx.speaktutor.AppConfig
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

class StartIoAdManager(private val context: Context) {
    private var rewardedVideo: StartAppAd? = null
    private var estaCargado = false

    fun precargarVideo() {
        if (!AppConfig.MOSTRAR_ANUNCIOS) return

        rewardedVideo = StartAppAd(context)
        rewardedVideo?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(ad: Ad) {
                estaCargado = true
            }

            override fun onFailedToReceiveAd(ad: Ad?) {
                estaCargado = false
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
                // El usuario completó el video: otorgamos la recompensa
                onRecompensaObtenida()
                estaCargado = false
                precargarVideo() // Precarga el siguiente anuncio
            }
            rewardedVideo?.showAd()
        } else {
            Toast.makeText(context, "Cargando video de recompensa, intenta de nuevo en unos segundos...", Toast.LENGTH_SHORT).show()
            precargarVideo()
        }
    }
}