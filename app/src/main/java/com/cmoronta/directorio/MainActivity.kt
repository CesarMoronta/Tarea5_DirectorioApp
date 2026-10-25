package com.cmoronta.directorio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cmoronta.directorio.ui.detalle.DetalleScreen
import com.cmoronta.directorio.ui.favoritos.FavoritosScreen
import com.cmoronta.directorio.ui.tema.TemaDirectorio
import com.cmoronta.directorio.ui.usuarios.UsuariosScreen

/**
 * Única Activity de la app.
 * onCreate → setContent: se dibuja la UI de Compose. Cada pantalla obtiene su ViewModel,
 * que se crea la primera vez y sobrevive a los cambios de configuración (rotación),
 * por eso la carga de datos vive en el init {} del ViewModel y no aquí.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaDirectorio { Navegacion() }
        }
    }
}

@Composable
private fun Navegacion() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "usuarios") {
        composable("usuarios") {
            UsuariosScreen(
                alAbrirUsuario = { id -> nav.navigate("detalle/$id") },
                alAbrirFavoritos = { nav.navigate("favoritos") },
            )
        }
        composable("detalle/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
            DetalleScreen(alVolver = { nav.popBackStack() })
        }
        composable("favoritos") {
            FavoritosScreen(
                alVolver = { nav.popBackStack() },
                alAbrirUsuario = { id -> nav.navigate("detalle/$id") },
            )
        }
    }
}
