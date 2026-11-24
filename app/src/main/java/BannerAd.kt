import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    if (!AppConfig.MOSTRAR_ANUNCIOS) return

    var bannerVisible by remember { mutableStateOf(false) }

    AndroidView(
        modifier = if (bannerVisible) {
            modifier
                .fillMaxWidth()
                .height(50.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(0.dp)
        },
        factory = { ctx ->
            val activity = ctx.findActivity()
            if (activity != null) {
                Banner(activity, object : BannerListener {
                    override fun onReceiveAd(banner: View?) {
                        Log.d("BannerStartIo", "✅ Banner recibido y listo")
                        bannerVisible = true
                    }

                    override fun onFailedToReceiveAd(banner: View?) {
                        Log.e("BannerStartIo", "❌ Falló la carga: ${banner?.tag ?: "sin inventario o sin red"}")
                        bannerVisible = false
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