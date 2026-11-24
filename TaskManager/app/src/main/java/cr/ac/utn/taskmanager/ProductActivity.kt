package cr.ac.utn.taskmanager

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cr.ac.utn.taskmanager.domain.model.Product
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class ProductActivity : AppCompatActivity() {

    private lateinit var txtId: EditText
    private lateinit var txtNombre: EditText
    private lateinit var txtCategoria: EditText
    private lateinit var txtPrecio: EditText
    private lateinit var txtCantidad: EditText
    private lateinit var txtFotoPath: EditText

    private var isEditMode: Boolean = false

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtId = findViewById(R.id.txtId_Product)
        txtNombre = findViewById(R.id.txtNombre_Product)
        txtCategoria = findViewById(R.id.txtCategoria_Product)
        txtPrecio = findViewById(R.id.txtPrecio_Product)
        txtCantidad = findViewById(R.id.txtCantidad_Product)
        txtFotoPath = findViewById(R.id.txtFotoPath_Product)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Product)

        val productId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!productId.isNullOrBlank()) {
            txtId.setText(productId)
            searchProduct(productId.toIntOrNull() ?: 0)
        }

        btnSearch.setOnClickListener {
            val id = txtId.text.toString().trim().toIntOrNull()
            if (id != null && id > 0) {
                searchProduct(id)
            } else {
                Toast.makeText(this, "Ingrese un ID válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mnuSave -> {
                if (isEditMode) {
                    util.showDialogCondition(
                        this,
                        getString(R.string.TextSaveQuestion)
                    ) { saveProduct() }
                } else {
                    saveProduct()
                }
                true
            }
            R.id.mnuDelete -> {
                util.showDialogCondition(
                    this,
                    getString(R.string.TextDeleteQuestion)
                ) { deleteProduct() }
                true
            }
            R.id.mnuCancel -> {
                cleanScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun searchProduct(id: Int) {
        if (id <= 0) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val product = productViewModel.getProductById(id)
                if (product != null) {
                    isEditMode = true
                    txtId.isEnabled = false
                    txtNombre.setText(product.nombre)
                    txtCategoria.setText(product.categoria)
                    txtPrecio.setText(product.precio.toString())
                    txtCantidad.setText(product.cantidad.toString())
                    txtFotoPath.setText(product.fotoPath)
                    invalidateOptionsMenu()
                } else {
                    Toast.makeText(
                        this@ProductActivity,
                        getString(R.string.TextDataNoFound),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                cleanScreen()
                Toast.makeText(
                    this@ProductActivity,
                    e.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun cleanScreen() {
        isEditMode = false
        txtId.isEnabled = true
        txtId.setText("")
        txtNombre.setText("")
        txtCategoria.setText("")
        txtPrecio.setText("")
        txtCantidad.setText("")
        txtFotoPath.setText("")
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return txtNombre.text.toString().trim().isNotBlank()
                && txtCategoria.text.toString().trim().isNotBlank()
                && txtPrecio.text.toString().trim().isNotBlank()
                && txtCantidad.text.toString().trim().isNotBlank()
    }

    private fun saveProduct() {
        if (!isValidationData()) {
            Toast.makeText(
                this@ProductActivity,
                "Datos incompletos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val id = txtId.text.toString().trim().toIntOrNull() ?: 0
        val nombre = txtNombre.text.toString().trim()
        val categoria = txtCategoria.text.toString().trim()
        val precio = txtPrecio.text.toString().trim().toDoubleOrNull() ?: 0.0
        val cantidad = txtCantidad.text.toString().trim().toIntOrNull() ?: 0
        val fotoPath = txtFotoPath.text.toString().trim()

        val product = Product(
            id = id,
            nombre = nombre,
            categoria = categoria,
            precio = precio,
            cantidad = cantidad,
            fotoPath = fotoPath
        )

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        productViewModel.saveProduct(product, isEditMode) { success, error ->
            progressDialog.dismiss()
            if (success) {
                cleanScreen()
                Toast.makeText(
                    this@ProductActivity,
                    getString(R.string.TextSaveSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@ProductActivity,
                    error ?: getString(R.string.TextSaveError),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteProduct() {
        val id = txtId.text.toString().trim().toIntOrNull() ?: 0
        if (id <= 0) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextDeleting))
        productViewModel.deleteProduct(id) { success, error ->
            progressDialog.dismiss()
            if (success) {
                cleanScreen()
                Toast.makeText(
                    this@ProductActivity,
                    getString(R.string.TextDeleteSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@ProductActivity,
                    error ?: getString(R.string.TextDeleteError),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
