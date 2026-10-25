package com.cmoronta.directorio.ui.usuarios

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmoronta.directorio.ui.comun.AvisoConexion
import com.cmoronta.directorio.ui.comun.Avatar
import com.cmoronta.directorio.ui.comun.BotonFavorito
import com.cmoronta.directorio.ui.comun.EstadoUi
import com.cmoronta.directorio.ui.comun.PantallaCargando
import com.cmoronta.directorio.ui.comun.PantallaError
import com.cmoronta.directorio.ui.comun.PantallaMensaje
import com.cmoronta.directorio.ui.comun.PantallaSinConexion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuariosScreen(
    alAbrirUsuario: (Int) -> Unit,
    alAbrirFavoritos: () -> Unit,
    viewModel: UsuariosViewModel = viewModel(factory = UsuariosViewModel.Fabrica),
) {
    // collectAsStateWithLifecycle: solo observa mientras la pantalla está visible (entre onStart y onStop).
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val hayConexion by viewModel.hayConexion.collectAsStateWithLifecycle()

    // ON_RESUME: al volver a la app (p. ej. tras activar el Wi-Fi), reintenta si había fallado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.alReanudar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Directorio") },
                actions = {
                    IconButton(onClick = viewModel::cargar) { Icon(Icons.Filled.Refresh, "Recargar") }
                    IconButton(onClick = alAbrirFavoritos) { Icon(Icons.Filled.Favorite, "Favoritos") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            if (!hayConexion && estado is EstadoUi.Exito) AvisoConexion("Sin conexión. Mostrando la última lista descargada.")

            when (val e = estado) {
                EstadoUi.Cargando -> PantallaCargando("Cargando usuarios…")
                EstadoUi.SinConexion -> PantallaSinConexion(alReintentar = viewModel::cargar, alVerFavoritos = alAbrirFavoritos)
                is EstadoUi.Error -> PantallaError(e.mensaje, alReintentar = viewModel::cargar)
                is EstadoUi.Exito ->
                    if (e.datos.isEmpty()) PantallaMensaje("🗂️", "Sin usuarios", "La API no devolvió usuarios.")
                    else ListaUsuarios(e.datos, alAbrirUsuario, viewModel::alternarFavorito)
            }
        }
    }
}

@Composable
private fun ListaUsuarios(
    usuarios: List<UsuarioItem>,
    alAbrirUsuario: (Int) -> Unit,
    alAlternarFavorito: (UsuarioItem) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(12.dp)) {
        items(usuarios, key = { it.usuario.id }) { item ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { alAbrirUsuario(item.usuario.id) },
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(item.usuario.nombre)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.usuario.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(item.usuario.correo, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("🏢 ${item.usuario.empresa}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BotonFavorito(item.esFavorito) { alAlternarFavorito(item) }
                }
            }
        }
    }
}
