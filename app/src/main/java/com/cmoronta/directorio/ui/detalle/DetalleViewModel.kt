package com.cmoronta.directorio.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmoronta.directorio.DirectorioApp
import com.cmoronta.directorio.data.modelo.Publicacion
import com.cmoronta.directorio.data.modelo.Usuario
import com.cmoronta.directorio.data.repositorio.UsuarioRepositorio
import com.cmoronta.directorio.ui.comun.EstadoUi
import com.cmoronta.directorio.ui.comun.aEstadoUi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetalleViewModel(
    estadoGuardado: SavedStateHandle,
    private val repositorio: UsuarioRepositorio,
) : ViewModel() {

    /** El id llega como argumento de navegación ("detalle/{id}") y sobrevive a la muerte del proceso. */
    private val usuarioId: Int = checkNotNull(estadoGuardado["id"])

    private val _usuario = MutableStateFlow<EstadoUi<Usuario>>(EstadoUi.Cargando)
    val usuario: StateFlow<EstadoUi<Usuario>> = _usuario.asStateFlow()

    private val _publicaciones = MutableStateFlow<EstadoUi<List<Publicacion>>>(EstadoUi.Cargando)
    val publicaciones: StateFlow<EstadoUi<List<Publicacion>>> = _publicaciones.asStateFlow()

    val esFavorito: StateFlow<Boolean> =
        repositorio.esFavorito(usuarioId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _usuario.value = EstadoUi.Cargando
            _publicaciones.value = EstadoUi.Cargando
            // Las dos peticiones van en paralelo.
            val usuario = async { repositorio.obtenerUsuario(usuarioId) }
            val posts = async { repositorio.obtenerPublicaciones(usuarioId) }
            _usuario.value = usuario.await().aEstadoUi()
            _publicaciones.value = posts.await().aEstadoUi()
        }
    }

    fun alternarFavorito() {
        val usuario = (_usuario.value as? EstadoUi.Exito)?.datos ?: return
        viewModelScope.launch {
            if (esFavorito.value) {
                repositorio.quitarFavorito(usuario.id)
            } else {
                // Guardamos también sus publicaciones para poder verlas sin conexión.
                val posts = (_publicaciones.value as? EstadoUi.Exito)?.datos.orEmpty()
                repositorio.guardarFavorito(usuario, posts)
            }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DirectorioApp
                DetalleViewModel(createSavedStateHandle(), app.contenedor.repositorio)
            }
        }
    }
}
