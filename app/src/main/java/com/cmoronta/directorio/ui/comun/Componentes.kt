package com.cmoronta.directorio.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PantallaCargando(texto: String = "Cargando…") {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PantallaMensaje(
    emoji: String,
    titulo: String,
    mensaje: String,
    textoBoton: String? = null,
    alPulsar: () -> Unit = {},
    textoBotonSecundario: String? = null,
    alPulsarSecundario: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (textoBoton != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = alPulsar) { Text(textoBoton) }
        }
        if (textoBotonSecundario != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = alPulsarSecundario) { Text(textoBotonSecundario) }
        }
    }
}

@Composable
fun PantallaSinConexion(alReintentar: () -> Unit, alVerFavoritos: (() -> Unit)? = null) = PantallaMensaje(
    emoji = "📡",
    titulo = "Sin conexión a Internet",
    mensaje = "Revisa tu Wi-Fi o datos móviles. Mientras tanto, puedes ver tus favoritos guardados.",
    textoBoton = "Reintentar",
    alPulsar = alReintentar,
    textoBotonSecundario = if (alVerFavoritos != null) "Ver favoritos" else null,
    alPulsarSecundario = alVerFavoritos ?: {},
)

@Composable
fun PantallaError(mensaje: String, alReintentar: () -> Unit) = PantallaMensaje(
    emoji = "⚠️",
    titulo = "No se pudieron cargar los datos",
    mensaje = mensaje,
    textoBoton = "Reintentar",
    alPulsar = alReintentar,
)

/** Franja superior que avisa cuando no hay conexión o se muestran datos guardados. */
@Composable
fun AvisoConexion(texto: String) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFF8A6D00), modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C4A00))
    }
}

@Composable
fun BotonFavorito(esFavorito: Boolean, alPulsar: () -> Unit) {
    IconButton(onClick = alPulsar, modifier = Modifier.semantics {
        contentDescription = if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos"
    }) {
        Icon(
            if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = if (esFavorito) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Círculo con las iniciales del usuario. */
@Composable
fun Avatar(
    nombre: String,
    tamano: Int = 44,
    fondo: Color = MaterialTheme.colorScheme.primaryContainer,
    texto: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(tamano.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales, color = texto, fontWeight = FontWeight.Bold)
    }
}
