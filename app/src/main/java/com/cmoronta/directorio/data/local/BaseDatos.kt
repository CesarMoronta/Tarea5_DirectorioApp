package com.cmoronta.directorio.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Usuario marcado como favorito, con todos sus datos para poder verlo sin conexión. */
@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val usuario: String,
    val correo: String,
    val telefono: String,
    val web: String,
    val empresa: String,
    val ciudad: String,
    val guardadoEn: Long = System.currentTimeMillis(),
)

/** Publicaciones de los favoritos, guardadas para mostrarlas sin conexión. */
@Entity(tableName = "publicaciones", indices = [Index("usuarioId")])
data class PublicacionEntity(
    @PrimaryKey val id: Int,
    val usuarioId: Int,
    val titulo: String,
    val cuerpo: String,
)

@Dao
interface FavoritoDao {

    @Query("SELECT * FROM favoritos ORDER BY nombre")
    fun observarFavoritos(): Flow<List<FavoritoEntity>>

    @Query("SELECT id FROM favoritos")
    fun observarIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE id = :id)")
    fun observarEsFavorito(id: Int): Flow<Boolean>

    @Query("SELECT * FROM favoritos WHERE id = :id")
    suspend fun obtener(id: Int): FavoritoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(favorito: FavoritoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPublicaciones(publicaciones: List<PublicacionEntity>)

    @Query("SELECT * FROM publicaciones WHERE usuarioId = :usuarioId ORDER BY id")
    suspend fun publicacionesDe(usuarioId: Int): List<PublicacionEntity>

    @Query("DELETE FROM favoritos WHERE id = :id")
    suspend fun eliminarFavorito(id: Int)

    @Query("DELETE FROM publicaciones WHERE usuarioId = :usuarioId")
    suspend fun eliminarPublicaciones(usuarioId: Int)

    @Transaction
    suspend fun guardar(favorito: FavoritoEntity, publicaciones: List<PublicacionEntity>) {
        insertar(favorito)
        if (publicaciones.isNotEmpty()) insertarPublicaciones(publicaciones)
    }

    @Transaction
    suspend fun quitar(id: Int) {
        eliminarPublicaciones(id)
        eliminarFavorito(id)
    }
}

@Database(entities = [FavoritoEntity::class, PublicacionEntity::class], version = 1, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {
    abstract fun favoritoDao(): FavoritoDao

    companion object {
        fun crear(contexto: Context): BaseDatos =
            Room.databaseBuilder(contexto, BaseDatos::class.java, "directorio.db").build()
    }
}
