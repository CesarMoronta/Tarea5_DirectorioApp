package com.cmoronta.directorio.ui.favoritos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmoronta.directorio.ui.comun.Avatar
import com.cmoronta.directorio.ui.comun.BotonFavorito
import com.cmoronta.directorio.ui.comun.EstadoUi
import com.cmoronta.directorio.ui.comun.PantallaCargando
import com.cmoronta.directorio.ui.comun.PantallaMensaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosScreen(
    alVolver: () -> Unit,
    alAbrirUsuario: (Int) -> Unit,
    viewModel: FavoritosViewModel = viewModel(factory = FavoritosViewModel.Fabrica),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoritos") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
            )
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            when (val e = estado) {
                is EstadoUi.Exito ->
                    if (e.datos.isEmpty()) {
                        PantallaMensaje("🤍", "Aún no tienes favoritos",
                            "Toca el corazón de un usuario para guardarlo aquí. Podrás verlo incluso sin Internet.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp)) {
                            items(e.datos, key = { it.id }) { usuario ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { alAbrirUsuario(usuario.id) }) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Avatar(usuario.nombre)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(usuario.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text(usuario.correo, style = MaterialTheme.typography.bodyMedium)
                                            Text("🏢 ${usuario.empresa}", style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        BotonFavorito(esFavorito = true) { viewModel.quitar(usuario) }
                                    }
                                }
                            }
                        }
                    }
                else -> PantallaCargando("Cargando favoritos…")
            }
        }
    }
}
