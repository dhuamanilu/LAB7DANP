package com.example.remarket.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.remarket.data.local.AppDatabase
import com.example.remarket.data.local.ProductDao
import com.example.remarket.data.network.ApiService
import com.example.remarket.data.network.AuthInterceptor
import com.example.remarket.data.repository.IProductRepository
import com.example.remarket.data.repository.ProductRepository
import com.example.remarket.data.repository.UserRepository
import com.example.remarket.domain.usecase.GetProductsUseCase
import com.google.android.gms.tasks.Tasks
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.example.remarket.data.network.TokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.remarket.data.repository.ConnectivityRepository
import com.example.remarket.data.repository.IConnectivityRepository
import dagger.hilt.android.qualifiers.ApplicationContext


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideConnectivityRepository(
        @ApplicationContext context: Context
    ): IConnectivityRepository = ConnectivityRepository(context)
    // --- REEMPLAZA EL TOKEN PROVIDER HARDCODEADO ---
    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): () -> String = {
        tokenManager.getToken() ?: "" // Devuelve el token guardado o un string vacío
    }

    // --- AÑADE UN PROVIDER PARA FIREBASE AUTH ---
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor // si lo tienes definido e inyectable
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true) // ✅ esto ayuda
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://161.132.50.99:9364/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        Log.d("HiltModule", "provideApiService() called with retrofit: $retrofit")
        return retrofit.create(ApiService::class.java)
    }

    // 2️⃣ Repositorios

    @Provides @Singleton
    fun provideProductRepository(
        dao: ProductDao,
        apiService: ApiService,

    ): IProductRepository = ProductRepository(dao,apiService)
    // Fíjate que devolvemos la INTERFAZ IProductRepository

    @Provides
    @Singleton
    fun provideUserRepository(api: ApiService): UserRepository =
        UserRepository(api)

    @Provides @Singleton
    fun provideGetProductsUseCase(repo: IProductRepository): GetProductsUseCase =
        GetProductsUseCase(repo)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "remarket-db").build()

    @Provides @Singleton
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
}
