package com.cmoronta.directorio.ui.comun

import com.cmoronta.directorio.data.modelo.Resultado

/** Los estados posibles de una pantalla que carga datos. */
sealed interface EstadoUi<out T> {
    data object Cargando : EstadoUi<Nothing>
    data class Exito<T>(val datos: T, val desdeCache: Boolean = false) : EstadoUi<T>
    data object SinConexion : EstadoUi<Nothing>
    data class Error(val mensaje: String) : EstadoUi<Nothing>
}

fun <T> Resultado<T>.aEstadoUi(): EstadoUi<T> = when (this) {
    is Resultado.Exito -> EstadoUi.Exito(datos, desdeCache)
    Resultado.SinConexion -> EstadoUi.SinConexion
    is Resultado.Error -> EstadoUi.Error(mensaje)
}

inline fun <T, R> EstadoUi<T>.mapear(transformar: (T) -> R): EstadoUi<R> = when (this) {
    is EstadoUi.Exito -> EstadoUi.Exito(transformar(datos), desdeCache)
    EstadoUi.Cargando -> EstadoUi.Cargando
    EstadoUi.SinConexion -> EstadoUi.SinConexion
    is EstadoUi.Error -> this
}
