package cr.ac.utn.taskmanager.domain.model

data class Product(
    val id: Int = 0,
    val nombre: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val fotoPath: String = ""
)
