package cr.ac.utn.taskmanager.domain.repository

import cr.ac.utn.taskmanager.domain.model.Product

interface IProductRepository {
    suspend fun getProducts(): Result<List<Product>>
    suspend fun getProductById(id: Int): Result<Product?>
    suspend fun createProduct(product: Product): Result<Product?>
    suspend fun updateProduct(id: Int, product: Product): Result<Product?>
    suspend fun deleteProduct(id: Int): Result<Boolean>
}
