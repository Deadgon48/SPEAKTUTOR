import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.itsx.speaktutor.AppConfig
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    Log.d("BannerStartIo", "Componiendo BannerAd. MOSTRAR_ANUNCIOS = ${AppConfig.MOSTRAR_ANUNCIOS}")
    if (!AppConfig.MOSTRAR_ANUNCIOS) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            val activity = ctx.findActivity()
            if (activity != null) {
                Banner(activity, object : BannerListener {
                    override fun onReceiveAd(banner: View?) {
                        Log.d("BannerStartIo", "✅ Banner recibido y listo")
                    }

                    override fun onFailedToReceiveAd(banner: View?) {
                        Log.e("BannerStartIo", "❌ Falló la carga: ${banner?.tag ?: "sin inventario o sin red"}")
                    }

                    override fun onImpression(banner: View?) {
                        Log.d("BannerStartIo", "👁️ Impresión registrada")
                    }

                    override fun onClick(banner: View?) {
                        Log.d("BannerStartIo", "👆 Clic en banner")
                    }
                }).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    loadAd(320, 50)
                }
            } else {
                Log.e("BannerStartIo", "❌ Activity es null")
                View(ctx)
            }
        }
    )
}