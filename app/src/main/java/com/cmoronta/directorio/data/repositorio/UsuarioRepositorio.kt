package com.cmoronta.directorio.data.repositorio

import com.cmoronta.directorio.data.local.FavoritoDao
import com.cmoronta.directorio.data.local.FavoritoEntity
import com.cmoronta.directorio.data.local.PublicacionEntity
import com.cmoronta.directorio.data.modelo.Publicacion
import com.cmoronta.directorio.data.modelo.Resultado
import com.cmoronta.directorio.data.modelo.Usuario
import com.cmoronta.directorio.data.red.MonitorConexion
import com.cmoronta.directorio.data.remoto.JsonPlaceholderApi
import com.cmoronta.directorio.data.remoto.PublicacionDto
import com.cmoronta.directorio.data.remoto.UsuarioDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

/**
 * Única fuente de datos para los ViewModels.
 * Decide si los datos vienen de la API (Retrofit) o de la base local (Room)
 * y convierte cualquier excepción en un [Resultado] para que la app nunca se cierre.
 */
class UsuarioRepositorio(
    private val api: JsonPlaceholderApi,
    private val dao: FavoritoDao,
    private val conexion: MonitorConexion,
) {
    // Caché en memoria de la última lista descargada (evita repetir la petición al abrir el detalle).
    private var cacheUsuarios: List<Usuario> = emptyList()

    val favoritos: Flow<List<Usuario>> = dao.observarFavoritos().map { lista -> lista.map { it.aModelo() } }
    val idsFavoritos: Flow<Set<Int>> = dao.observarIds().map { it.toSet() }
    fun esFavorito(id: Int): Flow<Boolean> = dao.observarEsFavorito(id)

    suspend fun obtenerUsuarios(): Resultado<List<Usuario>> = llamar {
        api.obtenerUsuarios().map { it.aModelo() }.also { cacheUsuarios = it }
    }

    suspend fun obtenerUsuario(id: Int): Resultado<Usuario> {
        cacheUsuarios.firstOrNull { it.id == id }?.let { return Resultado.Exito(it) }
        val local = dao.obtener(id)?.aModelo()
        if (local != null && !conexion.hayConexionAhora()) return Resultado.Exito(local, desdeCache = true)
        return when (val remoto = llamar { api.obtenerUsuario(id).aModelo() }) {
            is Resultado.Exito -> remoto
            else -> local?.let { Resultado.Exito(it, desdeCache = true) } ?: remoto
        }
    }

    /** Publicaciones del usuario. Si es favorito se guardan en Room y se usan cuando no hay conexión. */
    suspend fun obtenerPublicaciones(usuarioId: Int): Resultado<List<Publicacion>> {
        val remoto = llamar { api.obtenerPublicaciones(usuarioId).map { it.aModelo() } }
        if (remoto is Resultado.Exito) {
            if (dao.obtener(usuarioId) != null) dao.insertarPublicaciones(remoto.datos.map { it.aEntidad() })
            return remoto
        }
        val guardadas = dao.publicacionesDe(usuarioId)
        return if (guardadas.isNotEmpty()) Resultado.Exito(guardadas.map { it.aModelo() }, desdeCache = true) else remoto
    }

    suspend fun guardarFavorito(usuario: Usuario, publicaciones: List<Publicacion> = emptyList()) =
        dao.guardar(usuario.aEntidad(), publicaciones.map { it.aEntidad() })

    suspend fun quitarFavorito(id: Int) = dao.quitar(id)

    /** Ejecuta una llamada a la API y traduce los errores a mensajes claros. */
    private suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> = try {
        Resultado.Exito(bloque())
    } catch (e: CancellationException) {
        throw e // la corrutina se canceló (p. ej. se cerró la pantalla): no es un error
    } catch (e: IOException) {
        if (!conexion.hayConexionAhora()) Resultado.SinConexion
        else Resultado.Error("No se pudo contactar al servidor. Revisa tu conexión e inténtalo de nuevo.")
    } catch (e: HttpException) {
        Resultado.Error("El servidor respondió con un error (${e.code()}).")
    } catch (e: Exception) {
        Resultado.Error("Ocurrió un error inesperado: ${e.message ?: e.javaClass.simpleName}")
    }
}

// ---- Conversiones entre capas ----

fun UsuarioDto.aModelo() = Usuario(
    id = id,
    nombre = name,
    usuario = username.orEmpty(),
    correo = email.orEmpty(),
    telefono = phone.orEmpty(),
    web = website.orEmpty(),
    empresa = company?.name.orEmpty(),
    ciudad = address?.city.orEmpty(),
)

fun PublicacionDto.aModelo() = Publicacion(id = id, usuarioId = userId, titulo = title, cuerpo = body)

fun Usuario.aEntidad() = FavoritoEntity(id, nombre, usuario, correo, telefono, web, empresa, ciudad)

fun FavoritoEntity.aModelo() = Usuario(id, nombre, usuario, correo, telefono, web, empresa, ciudad)

fun Publicacion.aEntidad() = PublicacionEntity(id, usuarioId, titulo, cuerpo)

fun PublicacionEntity.aModelo() = Publicacion(id, usuarioId, titulo, cuerpo)
