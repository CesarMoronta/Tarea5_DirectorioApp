package com.cmoronta.directorio.ui.favoritos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cmoronta.directorio.DirectorioApp
import com.cmoronta.directorio.data.modelo.Usuario
import com.cmoronta.directorio.data.repositorio.UsuarioRepositorio
import com.cmoronta.directorio.ui.comun.EstadoUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Los favoritos salen SOLO de Room: funcionan igual con o sin Internet. */
class FavoritosViewModel(private val repositorio: UsuarioRepositorio) : ViewModel() {

    val estado: StateFlow<EstadoUi<List<Usuario>>> = repositorio.favoritos
        .map<List<Usuario>, EstadoUi<List<Usuario>>> { EstadoUi.Exito(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoUi.Cargando)

    fun quitar(usuario: Usuario) {
        viewModelScope.launch { repositorio.quitarFavorito(usuario.id) }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DirectorioApp
                FavoritosViewModel(app.contenedor.repositorio)
            }
        }
    }
}
