package cr.ac.utn.taskmanager.data.remote

import cr.ac.utn.taskmanager.domain.model.Product
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProductApiService {

    @GET("api/products")
    suspend fun getProducts(): Response<ApiResponse<List<Product>>>

    @GET("api/products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<ApiResponse<Product>>

    @POST("api/products")
    suspend fun createProduct(@Body product: Product): Response<ApiResponse<Product>>

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body product: Product
    ): Response<ApiResponse<Product>>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<ApiResponse<Any>>
}
