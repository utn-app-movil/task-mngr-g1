package cr.ac.utn.taskmanager.data.repository

import cr.ac.utn.taskmanager.data.remote.ProductApiService
import cr.ac.utn.taskmanager.domain.model.Product
import cr.ac.utn.taskmanager.domain.repository.IProductRepository

class ProductRepositoryImpl(
    private val apiService: ProductApiService
) : IProductRepository {

    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = apiService.getProducts()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()?.data ?: emptyList()
                Result.success(data)
            } else {
                Result.failure(Exception(response.message() ?: "Error fetching products"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductById(id: Int): Result<Product?> {
        return try {
            val response = apiService.getProductById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception(response.message() ?: "Error fetching product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProduct(product: Product): Result<Product?> {
        return try {
            val response = apiService.createProduct(product)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception(response.message() ?: "Error creating product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(id: Int, product: Product): Result<Product?> {
        return try {
            val response = apiService.updateProduct(id, product)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception(response.message() ?: "Error updating product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(id: Int): Result<Boolean> {
        return try {
            val response = apiService.deleteProduct(id)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message() ?: "Error deleting product"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
