package com.cmoronta.directorio

import android.app.Application
import com.cmoronta.directorio.data.local.BaseDatos
import com.cmoronta.directorio.data.red.MonitorConexion
import com.cmoronta.directorio.data.red.MonitorConexionAndroid
import com.cmoronta.directorio.data.remoto.ClienteRetrofit
import com.cmoronta.directorio.data.repositorio.UsuarioRepositorio

/**
 * Application: se crea una sola vez al iniciar el proceso (antes que cualquier Activity).
 * Aquí se construyen las dependencias compartidas (inyección manual de dependencias).
 */
class DirectorioApp : Application() {
    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
    }
}

class Contenedor(app: Application) {
    val conexion: MonitorConexion = MonitorConexionAndroid(app)
    private val baseDatos = BaseDatos.crear(app)
    val repositorio = UsuarioRepositorio(ClienteRetrofit.crearApi(), baseDatos.favoritoDao(), conexion)
}
