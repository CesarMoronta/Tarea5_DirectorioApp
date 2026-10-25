package com.cmoronta.directorio.data.remoto

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de https://jsonplaceholder.typicode.com que consume la app (Retrofit). */
interface JsonPlaceholderApi {

    @GET("users")
    suspend fun obtenerUsuarios(): List<UsuarioDto>

    @GET("users/{id}")
    suspend fun obtenerUsuario(@Path("id") id: Int): UsuarioDto

    @GET("posts")
    suspend fun obtenerPublicaciones(@Query("userId") usuarioId: Int): List<PublicacionDto>

    companion object {
        const val URL_BASE = "https://jsonplaceholder.typicode.com/"
    }
}

// ---- DTOs: reflejan exactamente el JSON de la API ----

data class UsuarioDto(
    val id: Int,
    val name: String,
    val username: String?,
    val email: String?,
    val phone: String?,
    val website: String?,
    val company: EmpresaDto?,
    val address: DireccionDto?,
)

data class EmpresaDto(val name: String?)

data class DireccionDto(val city: String?)

data class PublicacionDto(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
)
