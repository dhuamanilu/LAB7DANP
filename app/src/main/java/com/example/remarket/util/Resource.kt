package com.example.remarket.util

sealed class Resource<out T> {
    object Idle    : Resource<Nothing>()      // <- Nuevo estado inicial
    object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
