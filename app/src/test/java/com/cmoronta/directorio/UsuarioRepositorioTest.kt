package com.cmoronta.directorio

import com.cmoronta.directorio.data.local.FavoritoDao
import com.cmoronta.directorio.data.local.FavoritoEntity
import com.cmoronta.directorio.data.local.PublicacionEntity
import com.cmoronta.directorio.data.modelo.Resultado
import com.cmoronta.directorio.data.red.MonitorConexion
import com.cmoronta.directorio.data.remoto.EmpresaDto
import com.cmoronta.directorio.data.remoto.JsonPlaceholderApi
import com.cmoronta.directorio.data.remoto.PublicacionDto
import com.cmoronta.directorio.data.remoto.UsuarioDto
import com.cmoronta.directorio.data.repositorio.UsuarioRepositorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class UsuarioRepositorioTest {

    private val leanne = UsuarioDto(1, "Leanne Graham", "Bret", "Sincere@april.biz", "1-770", "hildegard.org",
        EmpresaDto("Romaguera-Crona"), null)

    private class ApiFalsa(var error: Exception? = null, val usuarios: List<UsuarioDto> = emptyList()) : JsonPlaceholderApi {
        override suspend fun obtenerUsuarios(): List<UsuarioDto> { error?.let { throw it }; return usuarios }
        override suspend fun obtenerUsuario(id: Int): UsuarioDto { error?.let { throw it }; return usuarios.first { it.id == id } }
        override suspend fun obtenerPublicaciones(usuarioId: Int): List<PublicacionDto> {
            error?.let { throw it }
            return listOf(PublicacionDto(10, usuarioId, "titulo", "cuerpo"))
        }
    }

    private class ConexionFalsa(var online: Boolean) : MonitorConexion {
        override val hayConexion: Flow<Boolean> = MutableStateFlow(online)
        override fun hayConexionAhora() = online
    }

    private class DaoFalso : FavoritoDao {
        val favoritos = MutableStateFlow<Map<Int, FavoritoEntity>>(emptyMap())
        val publicaciones = mutableMapOf<Int, PublicacionEntity>()
        override fun observarFavoritos() = favoritos.map { it.values.sortedBy { f -> f.nombre } }
        override fun observarIds() = favoritos.map { it.keys.toList() }
        override fun observarEsFavorito(id: Int) = favoritos.map { id in it }
        override suspend fun obtener(id: Int) = favoritos.value[id]
        override suspend fun insertar(favorito: FavoritoEntity) { favoritos.value = favoritos.value + (favorito.id to favorito) }
        override suspend fun insertarPublicaciones(publicaciones: List<PublicacionEntity>) {
            publicaciones.forEach { this.publicaciones[it.id] = it }
        }
        override suspend fun publicacionesDe(usuarioId: Int) = publicaciones.values.filter { it.usuarioId == usuarioId }
        override suspend fun eliminarFavorito(id: Int) { favoritos.value = favoritos.value - id }
        override suspend fun eliminarPublicaciones(usuarioId: Int) { publicaciones.values.removeAll { it.usuarioId == usuarioId } }
    }

    @Test
    fun `lista de usuarios exitosa trae nombre, correo y empresa`() = runTest {
        val repo = UsuarioRepositorio(ApiFalsa(usuarios = listOf(leanne)), DaoFalso(), ConexionFalsa(true))
        val r = repo.obtenerUsuarios() as Resultado.Exito
        val u = r.datos.single()
        assertEquals(listOf("Leanne Graham", "Sincere@april.biz", "Romaguera-Crona"), listOf(u.nombre, u.correo, u.empresa))
    }

    @Test
    fun `sin internet devuelve SinConexion en vez de lanzar una excepcion`() = runTest {
        val repo = UsuarioRepositorio(ApiFalsa(error = IOException("sin red")), DaoFalso(), ConexionFalsa(false))
        assertEquals(Resultado.SinConexion, repo.obtenerUsuarios())
    }

    @Test
    fun `error del servidor se convierte en mensaje claro`() = runTest {
        val http = HttpException(Response.error<Any>(500, "".toResponseBody()))
        val repo = UsuarioRepositorio(ApiFalsa(error = http), DaoFalso(), ConexionFalsa(true))
        val r = repo.obtenerUsuarios() as Resultado.Error
        assertTrue(r.mensaje.contains("500"))
    }

    @Test
    fun `favorito y sus publicaciones se ven sin conexion`() = runTest {
        val api = ApiFalsa(usuarios = listOf(leanne))
        val conexion = ConexionFalsa(true)
        val dao = DaoFalso()
        val repo = UsuarioRepositorio(api, dao, conexion)

        val usuario = (repo.obtenerUsuarios() as Resultado.Exito).datos.single()
        val posts = (repo.obtenerPublicaciones(1) as Resultado.Exito).datos
        repo.guardarFavorito(usuario, posts)

        // Se va Internet y se reinicia la app (repositorio nuevo, sin caché en memoria)
        api.error = IOException("sin red")
        conexion.online = false
        val sinRed = UsuarioRepositorio(api, dao, conexion)

        val u = sinRed.obtenerUsuario(1) as Resultado.Exito
        assertEquals("Leanne Graham", u.datos.nombre)
        assertTrue(u.desdeCache)
        val p = sinRed.obtenerPublicaciones(1) as Resultado.Exito
        assertEquals(1, p.datos.size)
        assertTrue(p.desdeCache)
    }

    @Test
    fun `quitar favorito borra tambien sus publicaciones`() = runTest {
        val dao = DaoFalso()
        val repo = UsuarioRepositorio(ApiFalsa(usuarios = listOf(leanne)), dao, ConexionFalsa(true))
        val usuario = (repo.obtenerUsuarios() as Resultado.Exito).datos.single()
        repo.guardarFavorito(usuario, (repo.obtenerPublicaciones(1) as Resultado.Exito).datos)
        repo.quitarFavorito(1)
        assertTrue(dao.favoritos.value.isEmpty())
        assertTrue(dao.publicaciones.isEmpty())
    }
}
