package space.taran.arkshelf.data.network

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object OkHttpClientBuilder {
    fun build(): OkHttpClient {
        val manager = object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(manager), SecureRandom())

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslContext.socketFactory, manager)
        builder.hostnameVerifier { _, _ -> true }
        return builder.build()
    }
}