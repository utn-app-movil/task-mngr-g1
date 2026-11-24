package cr.ac.utn.taskmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Product

interface OnProductItemClickListener {
    fun onProductClick(product: Product)
}

class ProductAdapter(
    private var products: List<Product>,
    private val itemClickListener: OnProductItemClickListener
) : RecyclerView.Adapter<ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_product_item, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product, itemClickListener)
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}

class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val tvNombre: TextView = view.findViewById(R.id.txtProductNombre_Item)
    private val tvCategoria: TextView = view.findViewById(R.id.txtProductCategoria_Item)
    private val tvPrecio: TextView = view.findViewById(R.id.txtProductPrecio_Item)
    private val tvCantidad: TextView = view.findViewById(R.id.txtProductCantidad_Item)

    fun bind(product: Product, itemClickListener: OnProductItemClickListener) {
        tvNombre.text = product.nombre
        tvCategoria.text = "Categoría: ${product.categoria}"
        tvPrecio.text = "$${product.precio}"
        tvCantidad.text = "Stock: ${product.cantidad}"
        itemView.setOnClickListener {
            itemClickListener.onProductClick(product)
        }
    }
}
