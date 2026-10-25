package com.cmoronta.directorio.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cmoronta.directorio.data.modelo.Publicacion
import com.cmoronta.directorio.data.modelo.Usuario
import com.cmoronta.directorio.ui.comun.AvisoConexion
import com.cmoronta.directorio.ui.comun.Avatar
import com.cmoronta.directorio.ui.comun.BotonFavorito
import com.cmoronta.directorio.ui.comun.EstadoUi
import com.cmoronta.directorio.ui.comun.PantallaCargando
import com.cmoronta.directorio.ui.comun.PantallaError
import com.cmoronta.directorio.ui.comun.PantallaSinConexion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    alVolver: () -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Fabrica),
) {
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val publicaciones by viewModel.publicaciones.collectAsStateWithLifecycle()
    val esFavorito by viewModel.esFavorito.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((usuario as? EstadoUi.Exito)?.datos?.nombre ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    if (usuario is EstadoUi.Exito) BotonFavorito(esFavorito, viewModel::alternarFavorito)
                },
            )
        },
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            when (val u = usuario) {
                EstadoUi.Cargando -> PantallaCargando("Cargando usuario…")
                EstadoUi.SinConexion -> PantallaSinConexion(alReintentar = viewModel::cargar)
                is EstadoUi.Error -> PantallaError(u.mensaje, alReintentar = viewModel::cargar)
                is EstadoUi.Exito -> {
                    if (u.desdeCache) AvisoConexion("Sin conexión. Mostrando datos guardados en favoritos.")
                    ContenidoDetalle(u.datos, publicaciones, viewModel::cargar)
                }
            }
        }
    }
}

@Composable
private fun ContenidoDetalle(usuario: Usuario, publicaciones: EstadoUi<List<Publicacion>>, alReintentar: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TarjetaUsuario(usuario) }
        item {
            Text("Publicaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp))
        }
        when (publicaciones) {
            EstadoUi.Cargando -> item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            EstadoUi.SinConexion -> item {
                MensajePublicaciones("📡 Sin conexión. Marca a este usuario como favorito cuando tengas Internet para guardar sus publicaciones.", alReintentar)
            }
            is EstadoUi.Error -> item { MensajePublicaciones("⚠️ ${publicaciones.mensaje}", alReintentar) }
            is EstadoUi.Exito -> {
                if (publicaciones.desdeCache) item { AvisoConexion("Publicaciones guardadas sin conexión.") }
                if (publicaciones.datos.isEmpty()) item { Text("Este usuario no tiene publicaciones.") }
                items(publicaciones.datos, key = { it.id }) { TarjetaPublicacion(it) }
            }
        }
    }
}

@Composable
private fun TarjetaUsuario(u: Usuario) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(u.nombre, tamano = 56, fondo = MaterialTheme.colorScheme.primary, texto = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(u.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@${u.usuario}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Dato("✉️", u.correo)
            Dato("📞", u.telefono)
            Dato("🌐", u.web)
            Dato("🏢", u.empresa)
            Dato("📍", u.ciudad)
        }
    }
}

@Composable
private fun Dato(icono: String, valor: String) {
    if (valor.isBlank()) return
    Text("$icono  $valor", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun TarjetaPublicacion(p: Publicacion) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(p.titulo.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(p.cuerpo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MensajePublicaciones(texto: String, alReintentar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(texto, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = alReintentar) { Text("Reintentar") }
    }
}
