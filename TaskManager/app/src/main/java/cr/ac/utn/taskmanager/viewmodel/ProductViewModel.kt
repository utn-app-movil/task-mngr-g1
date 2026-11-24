package cr.ac.utn.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.TaskManagerApp
import cr.ac.utn.taskmanager.domain.model.Product
import cr.ac.utn.taskmanager.domain.repository.IProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IProductRepository = (application as TaskManagerApp).productRepository

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.getProducts()
            result.onSuccess { productList ->
                _products.value = productList
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Error loading products"
            }
            _isLoading.value = false
        }
    }

    suspend fun getProductById(id: Int): Product? {
        val result = repository.getProductById(id)
        return result.getOrNull()
    }

    fun saveProduct(product: Product, isEditMode: Boolean, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = if (isEditMode) {
                repository.updateProduct(product.id, product)
            } else {
                repository.createProduct(product)
            }
            result.onSuccess {
                loadProducts()
                onComplete(true, null)
            }.onFailure { exception ->
                onComplete(false, exception.message ?: "Operation failed")
            }
        }
    }

    fun deleteProduct(id: Int, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteProduct(id)
            result.onSuccess {
                loadProducts()
                onComplete(true, null)
            }.onFailure { exception ->
                onComplete(false, exception.message ?: "Failed to delete product")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                return ProductViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
