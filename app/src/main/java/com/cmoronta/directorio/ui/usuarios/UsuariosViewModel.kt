package com.cmoronta.directorio.ui.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmoronta.directorio.DirectorioApp
import com.cmoronta.directorio.data.modelo.Usuario
import com.cmoronta.directorio.data.red.MonitorConexion
import com.cmoronta.directorio.data.repositorio.UsuarioRepositorio
import com.cmoronta.directorio.ui.comun.EstadoUi
import com.cmoronta.directorio.ui.comun.aEstadoUi
import com.cmoronta.directorio.ui.comun.mapear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UsuarioItem(val usuario: Usuario, val esFavorito: Boolean)

class UsuariosViewModel(
    private val repositorio: UsuarioRepositorio,
    conexion: MonitorConexion,
) : ViewModel() {

    private val carga = MutableStateFlow<EstadoUi<List<Usuario>>>(EstadoUi.Cargando)

    /** Lista + marca de favorito, recalculada cada vez que cambia la lista o los favoritos de Room. */
    val estado: StateFlow<EstadoUi<List<UsuarioItem>>> =
        combine(carga, repositorio.idsFavoritos) { estado, favoritos ->
            estado.mapear { lista -> lista.map { UsuarioItem(it, it.id in favoritos) } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    val hayConexion: StateFlow<Boolean> =
        conexion.hayConexion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        // Se ejecuta UNA vez al crear el ViewModel (primer onCreate de la pantalla).
        // Al rotar el teléfono el ViewModel sobrevive, así que NO se vuelve a descargar.
        cargar()

        // Si estábamos sin conexión y vuelve Internet, reintenta solo.
        viewModelScope.launch {
            conexion.hayConexion.collect { online ->
                if (online && carga.value is EstadoUi.SinConexion) cargar()
            }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            carga.value = EstadoUi.Cargando
            carga.value = repositorio.obtenerUsuarios().aEstadoUi()
        }
    }

    /** Llamado en ON_RESUME: si la última carga falló, se vuelve a intentar al regresar a la app. */
    fun alReanudar() {
        if (carga.value is EstadoUi.Error || carga.value is EstadoUi.SinConexion) cargar()
    }

    fun alternarFavorito(item: UsuarioItem) {
        viewModelScope.launch {
            if (item.esFavorito) repositorio.quitarFavorito(item.usuario.id)
            else repositorio.guardarFavorito(item.usuario)
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DirectorioApp
                UsuariosViewModel(app.contenedor.repositorio, app.contenedor.conexion)
            }
        }
    }
}
