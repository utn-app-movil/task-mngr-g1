# Práctica de Laboratorio: TaskManager - Depuración y Extensión de Funcionalidades

**Asignatura:** Desarrollo de Aplicaciones Móviles  
**Duración Estimada:** 4 Horas  
**Modalidad:** Individual o en Parejas  
**Nivel:** Intermedio (Kotlin, Android SDK, MVVM, Architecture Components)  

---

## 📋 Descripción General

En esta práctica de laboratorio, los estudiantes tomarán como base el proyecto **TaskManager** (una aplicación móvil para la gestión de tareas personales) para poner a prueba y fortalecer sus habilidades de **depuración de código (debugging)**, **arquitectura de software en Android (MVVM)**, **interacción con interfaz de usuario (UI)** y **colecciones/filtrado en Kotlin**.

---

## 🎯 Objetivos de Aprendizaje

1. **Aprender a depurar aplicaciones Android:** Utilizar las herramientas del IDE (**Logcat, Breakpoints, Inspect Variables y Stack Trace**) para diagnosticar y corregir errores en tiempo de ejecución (Runtime Exceptions) y errores de lógica.
2. **Implementar componentes de arquitectura:** Desarrollar nuevas actividades (`Activity`), manejar la navegación mediante `Intents`, conectar la capa de presentación con el `ViewModel` y actualizar componentes como `RecyclerView` y `Spinner`.
3. **Manipular datos y colecciones:** Aplicar transformaciones y filtrados dinámicos sobre colecciones de Kotlin (`filter`, `count`, `map`).
4. *(Opcional)* **Persistencia y UI Avanzada:** Implementar persistencia local con **Room** o interacciones gestuales con **ItemTouchHelper**.

---

## 🐞 Fase 0: Guía de Errores Intencionales de Código (Debugging - 45 min)

El proyecto base contiene **4 errores intencionales de código** que los estudiantes deben encontrar y solucionar utilizando el modo Debug de Android Studio antes de avanzar.

---

### 🔍 Error 1: Búsqueda y Eliminación Falla por Espacios en Blanco (`TaskMemoryManager.kt`)
* **Ubicación:** [`TaskMemoryManager.kt`](file:///D:/Git/Github/movil/task-mngr-g1/TaskManager/app/src/main/java/cr/ac/utn/taskmanager/data/memory/TaskMemoryManager.kt)
* **Comportamiento en la App:** Si el usuario guarda una tarea con ID que contiene espacios (ej. `"TASK-01 "`), al intentar buscarla o eliminarla la app no la encuentra y no muestra ningún error en Logcat.
* **Instrucción de Debugging:**
  1. Colocar un **Breakpoint** dentro de `getById(id)` o `deleteTask(id)`.
  2. Usar la ventana de inspección de variables (*Evaluate Expression* `Alt + F8`) para comparar el valor de `it.id` vs `id`.
* **Solución:**
  ```kotlin
  // Cambiar de:
  tasks.filter { it.id == id }
  
  // A:
  tasks.filter { it.id.trim().equals(id.trim(), ignoreCase = true) }
  ```

---

### 🔍 Error 2: El Clic en el RecyclerView Abre la Tarea Incorrecta (`TaskListAdapter.kt`)
* **Ubicación:** [`TaskListAdapter.kt`](file:///D:/Git/Github/movil/task-mngr-g1/TaskManager/app/src/main/java/cr/ac/utn/taskmanager/TaskListAdapter.kt)
* **Comportamiento en la App:** Al presionar un elemento en la lista de tareas, el listener recibe la tarea de la posición anterior (`position - 1`) en lugar de la tarea presionada.
* **Instrucción de Debugging:**
  1. Colocar un **Breakpoint** en el `setOnClickListener` de `onBindViewHolder`.
  2. Inspeccionar la variable `targetIndex` y observar la diferencia con `position`.
* **Solución:**
  ```kotlin
  // Cambiar de:
  val targetIndex = if (position > 0) position - 1 else position
  itemClickListener.onItemClicked(itemList[targetIndex])

  // A:
  itemClickListener.onItemClicked(item)
  ```

---

### 🔍 Error 3: Excepción de Formato de Fecha al Seleccionar Fechas con Día > 12 (`util.kt`)
* **Ubicación:** [`util.kt`](file:///D:/Git/Github/movil/task-mngr-g1/TaskManager/app/src/main/java/cr/ac/utn/taskmanager/util/util.kt)
* **Comportamiento en la App:** Al intentar guardar una tarea con fecha seleccionada donde el día es mayor a 12 (ej. `15/03/2025`), la aplicación se cierra con una `DateTimeParseException`.
* **Instrucción de Debugging:**
  1. Revisar en **Logcat** la excepción `DateTimeParseException: Invalid value for MonthOfYear`.
  2. Colocar un **Breakpoint** en `parseStringToDateModern` y verificar la cadena del patrón invertido `"MM/dd/yyyy"`.
* **Solución:**
  ```kotlin
  // Cambiar de:
  val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault())

  // A:
  val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
  ```

---

### 🔍 Error 5: La UI de la Lista de Tareas No Se Actualiza (`TaskListActivity.kt`)
* **Ubicación:** [`TaskListActivity.kt`](file:///D:/Git/Github/movil/task-mngr-g1/TaskManager/app/src/main/java/cr/ac/utn/taskmanager/TaskListActivity.kt)
* **Comportamiento en la App:** Al guardar o editar una tarea desde `TaskActivity` y regresar a `TaskListActivity`, la lista sigue mostrando los datos viejos.
* **Instrucción de Debugging:**
  1. Poner un **Breakpoint** en `onCreate` y `onResume` de `TaskListActivity`.
  2. Observar que el adaptador nunca notifica los cambios de la lista (`notifyDataSetChanged()`).
* **Solución:**
  ```kotlin
  // Agregar en onResume() o tras cargar los datos:
  override fun onResume() {
      super.onResume()
      customAdapter.updateTasks(taskViewModel.getTasks())
      customAdapter.notifyDataSetChanged()
  }
  ```

---

## 🚀 Fase 1: Nueva Funcionalidad 1 - Pantalla de Estadísticas y Resumen (`TaskStatsActivity`) (75 min)

Desarrollar una nueva pantalla que presente métricas y estadísticas sobre las tareas registradas.

### Requisitos Técnicos:

1. **Creación de la Activity:**
   - Crear la pantalla `TaskStatsActivity` y declararla en `AndroidManifest.xml`.
   - Agregar un botón en `MainActivity` o en el menú de la aplicación que permita navegar a esta pantalla mediante un `Intent`.

2. **Diseño de la Interfaz (`activity_task_stats.xml`):**
   - Un `TextView` grande con el **Total de Tareas Registradas**.
   - Conteo de tareas desglosado por cada Estado:
     - **Pendientes** (`PENDING`)
     - **En Progreso** (`IN_PROGRESS`)
     - **Completadas** (`COMPLETED`)
     - **Canceladas** (`CANCELLED`)
   - Una barra de progreso (`ProgressBar` horizontal) que muestre visualmente el porcentaje de tareas completadas respecto al total.
   - Botón para **Volver al Menú Principal**.

3. **Lógica de Negocio en ViewModel:**
   - Obtener la lista de tareas desde `TaskViewModel`.
   - Calcular los totales utilizando funciones de orden superior de Kotlin (`count`, `filter`).

---

## 🔍 Fase 2: Nueva Funcionalidad 2 - Búsqueda Dinámica y Filtros por Estado en `TaskListActivity` (75 min)

Mejorar la pantalla de lista de tareas (`TaskListActivity`) para permitir al usuario buscar y filtrar tareas en tiempo real.

### Requisitos Técnicos:

1. **Componentes de UI en `activity_task_list.xml`:**
   - Agregar un `SearchView` (o `EditText` con listener de cambio de texto) en la parte superior del `RecyclerView`.
   - Agregar un `Spinner` o grupo de botones (`ChipGroup` / `RadioGroup`) con las opciones: `TODAS`, `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

2. **Lógica de Filtrado Combinado:**
   - Implementar un método que reciba el texto de búsqueda y el estado seleccionado para filtrar en tiempo real.

---

## 🔥 Fase 3: Retos Adicionales (Para Estudiantes Avanzados) (45 min)

Elegir **UNO** de los siguientes retos para obtener puntos extra en la evaluación:

### 🏆 Reto A: Eliminar Tareas con Gestos (Swipe to Delete) y Opción Undo
- Implementar un `ItemTouchHelper.SimpleCallback` en el `RecyclerView` de `TaskListActivity`.
- Al eliminar, mostrar un `Snackbar` con la opción **"DESHACER"** para restaurar la tarea.

### 🏆 Reto B: Persistencia Local con Room Database
- Migrar el repositorio temporal `TaskMemoryManager` a una base de datos local **Room**.

---

## 📊 Criterios de Evaluación

| Criterio | Porcentaje | Descripción |
| :--- | :---: | :--- |
| **1. Depuración (Fase 0)** | **20%** | Solución correcta de los 4 errores intencionales usando el Debugger. |
| **2. Pantalla de Estadísticas (Fase 1)** | **30%** | Diseño adecuado en XML, cálculo correcto de métricas y navegación funcional. |
| **3. Búsqueda y Filtros (Fase 2)** | **30%** | Filtrado combinado en tiempo real por texto y estado. |
| **4. Calidad del Código y Buenas Prácticas** | **10%** | Uso de arquitectura MVVM y nombres limpios. |
| **5. Reto Opcional (Fase 3)** | **10% (Extra)** | Implementación correcta de Swipe-to-Delete o Persistencia con Room. |
| **TOTAL** | **100% (+10%)** | |
