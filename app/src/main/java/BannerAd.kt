import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.itsx.speaktutor.AppConfig

@Composable
fun BannerAdMob(modifier: Modifier = Modifier) {
    // Si la versión es PRO, no renderizamos nada
    if (!AppConfig.MOSTRAR_ANUNCIOS) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context: Context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // REEMPLAZA con tu ID de bloque de anuncios de prueba o real de AdMob
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}