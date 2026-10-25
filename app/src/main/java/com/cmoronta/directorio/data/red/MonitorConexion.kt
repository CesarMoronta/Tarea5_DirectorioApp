package com.cmoronta.directorio.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Informa si hay Internet y avisa cuando la conexión se pierde o vuelve. */
interface MonitorConexion {
    val hayConexion: Flow<Boolean>
    fun hayConexionAhora(): Boolean
}

class MonitorConexionAndroid(contexto: Context) : MonitorConexion {

    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)

    override fun hayConexionAhora(): Boolean {
        val capacidades = gestor.getNetworkCapabilities(gestor.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override val hayConexion: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(hayConexionAhora()) }
            override fun onUnavailable() { trySend(false) }
        }
        trySend(hayConexionAhora())
        gestor.registerDefaultNetworkCallback(callback)
        awaitClose { gestor.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
