package cr.ac.utn.taskmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import cr.ac.utn.taskmanager.domain.model.Product
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class ProductListActivity : AppCompatActivity(), OnProductItemClickListener {

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModel.Factory(application)
    }

    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recycler = findViewById<RecyclerView>(R.id.rvProducts_List)
        productAdapter = ProductAdapter(emptyList(), this)
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.adapter = productAdapter

        val fabAddProduct = findViewById<FloatingActionButton>(R.id.fabAddProduct)
        fabAddProduct.setOnClickListener {
            util.openActivity(this, ProductActivity::class.java)
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        productViewModel.loadProducts()
    }

    private fun observeViewModel() {
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        var isFirstLoad = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    productViewModel.products.collect { products ->
                        productAdapter.updateProducts(products)
                        if (isFirstLoad && products.isNotEmpty()) {
                            isFirstLoad = false
                            progressDialog.dismiss()
                        }
                    }
                }

                launch {
                    productViewModel.isLoading.collect { isLoading ->
                        if (!isLoading && isFirstLoad) {
                            isFirstLoad = false
                            progressDialog.dismiss()
                        }
                    }
                }

                launch {
                    productViewModel.errorMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            Toast.makeText(this@ProductListActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onProductClick(product: Product) {
        util.openActivity(
            this,
            ProductActivity::class.java,
            EXTRA_MESSAGE_ID,
            product.id.toString()
        )
    }
}
