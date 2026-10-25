package com.cmoronta.directorio.data.modelo

/** Modelos de dominio: lo que usa la UI, independientes de la API y de Room. */

data class Usuario(
    val id: Int,
    val nombre: String,
    val usuario: String,
    val correo: String,
    val telefono: String,
    val web: String,
    val empresa: String,
    val ciudad: String,
)

data class Publicacion(
    val id: Int,
    val usuarioId: Int,
    val titulo: String,
    val cuerpo: String,
)

/** Resultado de una operación del repositorio: éxito, sin conexión o error. */
sealed interface Resultado<out T> {
    data class Exito<T>(val datos: T, val desdeCache: Boolean = false) : Resultado<T>
    data object SinConexion : Resultado<Nothing>
    data class Error(val mensaje: String) : Resultado<Nothing>
}
