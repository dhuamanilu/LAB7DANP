// data/repository/ProductRepository.kt
package com.example.remarket.data.repository

import com.android.identity.util.UUID
import com.example.remarket.data.local.ProductDao
import com.example.remarket.data.local.ProductEntity
import com.example.remarket.data.local.SyncStatus
import com.example.remarket.data.local.toDomain
import com.example.remarket.data.model.Product
import com.example.remarket.data.model.ProductDto
import com.example.remarket.data.network.ApiService
import com.example.remarket.util.Resource
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject // Importar Inject
import com.example.remarket.data.model.toDomain
import com.example.remarket.data.network.ProductRequest
import com.example.remarket.data.network.ReportRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.format.DateTimeFormatter

class ProductRepository @Inject constructor(
    private val dao: ProductDao,
    private val api: ApiService
) : IProductRepository{
    // OPCIÓN 1: Estrategia Single Source of Truth (Recomendada)
    override suspend fun getAllProducts(): Flow<List<Product>> = flow {
        try {
            // 1. Emitir datos locales primero (carga rápida)
            val localProducts = dao.getAll().map { entities ->
                entities.map { it.toDomain() }
            }

            // Emitir los datos locales primero
            localProducts.collect { products ->
                emit(products)
            }

            // 2. Obtener datos frescos de la API
            val remoteProducts = api.getProducts() // Necesitas crear este endpoint

            // 3. Guardar en base de datos local
            val entities = remoteProducts.map { dto ->
                ProductEntity(
                    id = dto.id,
                    sellerId = dto.sellerId,
                    brand = dto.brand,
                    model = dto.model,
                    storage = dto.storage,
                    price = dto.price,
                    imei = dto.imei,
                    description = dto.description,
                    images = dto.imageUrls,
                    boxImageUrl = dto.boxImageUrl,
                    invoiceUrl = dto.invoiceUrl,
                    status = dto.status,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = System.currentTimeMillis()
                )
            }

            // Limpiar datos antiguos y insertar nuevos
            dao.deleteAll() // Necesitas agregar este método
            entities.forEach { dao.upsert(it) }

            // 4. Emitir datos actualizados
            val updatedProducts = dao.getAll().map { updatedEntities ->
                updatedEntities.map { it.toDomain() }
            }

            updatedProducts.collect { products ->
                emit(products)
            }

        } catch (e: Exception) {
            // En caso de error de red, emitir solo datos locales
            val localProducts = dao.getAll().map { entities ->
                entities.map { it.toDomain() }
            }
            localProducts.collect { products ->
                emit(products)
            }
        }
    }.flowOn(Dispatchers.IO)
    override suspend fun createProduct(request: ProductRequest): Resource<Unit> {
        // 1) Genera un Instant “ahora” y su epoch millis
        val nowInstant = Instant.now()                     // TemporalAccessor
        val nowEpoch   = nowInstant.toEpochMilli()         // Long

        // 2) Formatea la fecha ISO-8601
        val iso        = DateTimeFormatter.ISO_INSTANT
            .format(nowInstant)           // OK: Instant es TemporalAccessor
        // 2) Crea entity marcándolo PENDING
        val entity = ProductEntity(
            id             = UUID.randomUUID().toString(),
            sellerId       = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw IllegalStateException("Usuario no autenticado"),
            brand          = request.brand,
            model          = request.model,
            storage        = request.storage,
            price          = request.price,
            imei           = request.imei,
            description    = request.description,
            images         = request.imageUrls,
            boxImageUrl    = request.boxImageUrl,
            invoiceUrl     = request.invoiceUrl,
            status         = "CREATED",
            active         = true,
            createdAt      = iso,
            updatedAt      = iso,
            syncStatus     = SyncStatus.PENDING,
            lastModified   = nowEpoch
        )
        dao.upsert(entity)
        return Resource.Success(Unit)
    }

    override suspend fun getPendingProducts(): List<ProductEntity> =
        dao.getByStatus(SyncStatus.PENDING)

    override suspend fun syncPendingProducts() {
        val pendings = getPendingProducts()
        pendings.forEach { e ->
            try {
                // 3) Llamada al servidor usando el mismo ProductRequest
                val req = ProductRequest(
                    brand       = e.brand,
                    model       = e.model,
                    storage     = e.storage,
                    price       = e.price,
                    imei        = e.imei,
                    description = e.description,
                    imageUrls   = e.images,
                    boxImageUrl = e.boxImageUrl,
                    invoiceUrl  = e.invoiceUrl
                )
                val dto = api.createProduct(req)
                // 4) Al éxito, actualiza entidad con datos del servidor y marca SYNCED
                val updatedEntity = e.copy(
                    id           = dto.id,
                    createdAt    = dto.createdAt,
                    updatedAt    = dto.updatedAt,
                    syncStatus   = SyncStatus.SYNCED,
                    lastModified = System.currentTimeMillis()
                )
                dao.upsert(updatedEntity)
            } catch (ex: Exception) {
                // marcase ERROR para reintentar luego
                dao.update(e.copy(syncStatus = SyncStatus.ERROR))
            }
        }
    }
    override suspend fun getProductById(productId: String): Flow<Resource<Product>> = flow {
        try {
            // Llamada al endpoint específico
            val dto= api.getProductById(productId)
            emit(Resource.Success(dto.toDomain()))
        } catch (e: HttpException) {
            // Manejo de errores HTTP (404, 500, etc.)
            val msg = when (e.code()) {
                404 -> "Producto no encontrado (404)"
                500 -> "Error interno del servidor (500)"
                else -> "Error ${e.code()}: ${e.message()}"
            }
            emit(Resource.Error(msg))
        } catch (e: IOException) {
            // Errores de red
            emit(Resource.Error("Error de red: ${e.localizedMessage}"))
        }
    }
        .catch { e ->
            // Captura cualquier otra excepción y emite Resource.Error
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido al obtener producto"))
        }
        .flowOn(Dispatchers.IO)

    override suspend fun toggleFavorite(productId: String): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun reportProduct(productId: String, reason: String): Flow<Boolean> = flow {
        try {
            // Realiza POST /reports
            val response = api.createReport(ReportRequest(productId, reason))
            emit(response.isSuccessful)
        } catch (e: HttpException) {
            emit(false)
        } catch (e: IOException) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)
    // Agrega este método a tu ProductRepository.kt

    override suspend fun syncAllProducts() {
        try {
            // 1. Obtener todos los productos del servidor
            val remoteProducts = api.getProducts()

            // 2. Convertir a ProductEntity usando la función toDomain() existente
            val entities = remoteProducts.map { dto ->
                val product = dto.toDomain()

                ProductEntity(
                    id = product.id,
                    sellerId = product.sellerId,
                    brand = product.brand,
                    model = product.model,
                    storage = product.storage,
                    price = product.price,
                    imei = product.imei,
                    description = product.description,
                    images = product.images,
                    boxImageUrl = if (product.box.isNotEmpty()) product.box else null,
                    invoiceUrl = if (product.invoiceUri.isNotEmpty()) product.invoiceUri else null,
                    status = product.status,
                    active = product.active,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = System.currentTimeMillis()
                )
            }

            // 3. Actualizar base de datos local
            // Estrategia: Mantener productos PENDING locales, reemplazar solo SYNCED
            dao.deleteAllSynced() // Eliminar solo productos ya sincronizados
            entities.forEach { dao.upsert(it) }

            println("Sincronización completada: ${entities.size} productos actualizados")

        } catch (e: Exception) {
            println("Error en syncAllProducts: ${e.message}")
            // No lanzar excepción para que el Worker no falle constantemente
            // Si quieres que el Worker reintente, descomenta la siguiente línea:
            // throw e
        }
    }
}
