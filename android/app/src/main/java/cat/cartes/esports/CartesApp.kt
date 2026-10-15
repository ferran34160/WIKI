package cat.cartes.esports

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

/**
 * Wikimedia rebutja les peticions amb l'identificador per defecte d'OkHttp
 * (política d'User-Agent), de manera que sense això les fotos no es carreguen.
 */
class CartesApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()
                        )
                    }
                    .build()
            }
            .build()

    private companion object {
        const val USER_AGENT = "CartesEsports/0.1 (https://github.com/ferran34160/WIKI)"
    }
}
