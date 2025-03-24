object toy_scannamar {
  // Importar librerías necesarias
  import org.apache.poi.ss.usermodel._
  import org.apache.poi.ss.usermodel.WorkbookFactory
  import java.io.{FileInputStream, File, FileOutputStream}
  import scala.jdk.CollectionConverters._
  import scala.io.StdIn
  import scala.annotation.tailrec

  // Declaración de variables globales
  var sesionRetiroIniciada: Boolean = false
  var animalActual: Option[Animal] = None
  //var materialesRetirados: List[Material] = List() -> código vestigial en Scannamar

  // Ruta al archivo Excel
  val rutaExcel: String = "C:\\Users\\guzzm\\Desktop\\XProyecto_Henry_Pizarro_Scala\\src\\main\\resources\\toy_excel_scannamar.xlsx"

  // Definición de las clases de datos
  case class Animal(
                     nombre: String, // Columna A (índice 0)
                   )

  // FUNCIÓN DE UTILIDAD ASIGNAR la respectiva letra de la columna
  def numberToExcelColumn(n: Int): String = {
    @tailrec
    def loop(num: Int, acc: String): String = {
      if (num < 0) acc
      else {
        val remainder = num % 26
        val nextNum = num / 26 - 1
        loop(nextNum, ('A' + remainder).toChar.toString + acc)
      }
    }

    loop(n, "")
  }

  // 1) FUNCIÓN BUSCAR ANIMAL
  def buscarAnimal(nombreAnimal: String): Option[Animal] = {
    try {
      println(s" ")

      // Abrir archivo Excel
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        println(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return None
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal") // Asume que es la primera hoja, ajusta si es necesario

      if (sheet == null) {
        println("Error: No se encuentra la hoja en el archivo Excel")
        workbook.close()
        fis.close()
        return None
      }

      // Función auxiliar para obtener el valor de una celda
      def getCellValue(cell: Cell): String = {
        if (cell == null) return ""
        cell.getCellType match {
          case CellType.NUMERIC =>
            val num = cell.getNumericCellValue
            if (num == num.floor) num.toInt.toString else num.toString
          case CellType.STRING => cell.getStringCellValue.trim
          case _ => ""
        }
      }

      // Buscar animal en la columna A
      val animalOpt = sheet.iterator().asScala.drop(0).find { row =>
        val cell = row.getCell(0) // Columna A (índice 0)
        if (cell != null) {
          val valorCelda = getCellValue(cell)
          val coincide = valorCelda.equalsIgnoreCase(nombreAnimal)
          if (coincide) {
            println(s"\nEncontrado el animal en la columna A y en la fila ${row.getRowNum + 1}")
          }
          coincide
        } else false
      }.map { row =>
        // Crear objeto Animal
        val animal = Animal(
          nombre = getCellValue(row.getCell(0))
          // Puedes agregar más campos si es necesario, por ejemplo:
          // descripcion = Option(getCellValue(row.getCell(1)))
        )

        // Imprimir detalles del animal encontrado
        println("\nDatos del animal encontrado:")
        println(s"Nombre: ${animal.nombre}")

        animal
      }

      workbook.close()
      fis.close()

      if (animalOpt.isEmpty) {
        println("\nError: No se encontró ningún animal con ese nombre")
      }

      animalOpt

    } catch {
      case e: Exception =>
        println(s"Error: No se pudo buscar animal: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  }

  // 2) FUNCIÓN CORTAR Y PEGAR NÚMEROS
  def cortarPegarNumeros(): Option[Boolean] = {
    try {
      println(s"Iniciando selección de números para cortar y pegar")

      // Abrir archivo Excel
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        println(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return None
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)

      // Obtener la hoja Inicial
      val sheetInicial = workbook.getSheet("Inicial")
      val sheetFinal = workbook.getSheet("Final")

      if (sheetInicial == null || sheetFinal == null) {
        println("Error: No se encuentran las hojas 'Inicial' o 'Final' en el archivo Excel")
        workbook.close()
        fis.close()
        return None
      }

      // Función auxiliar para obtener el valor de una celda
      def getCellValue(cell: Cell): String = {
        if (cell == null) return ""
        cell.getCellType match {
          case CellType.NUMERIC =>
            val num = cell.getNumericCellValue
            if (num == num.floor) num.toInt.toString else num.toString
          case CellType.STRING => cell.getStringCellValue.trim
          case _ => ""
        }
      }

      // Encontrar la última fila con contenido en la columna A
      var ultimaFila = -1
      val iteratorUltima = sheetInicial.iterator().asScala
      while (iteratorUltima.hasNext) {
        val currentRow = iteratorUltima.next()
        val cellA = currentRow.getCell(0)
        if (cellA != null && getCellValue(cellA).nonEmpty) {
          ultimaFila = currentRow.getRowNum
        }
      }

      if (ultimaFila == -1) {
        println("No se encontraron filas con contenido en la columna A")
        workbook.close()
        fis.close()
        return Some(false)
      }

      // Recopilar números encontrados
      val numerosEncontrados = scala.collection.mutable.ListBuffer[(Int, String, Int)]()

      // Recorrer desde la primera fila hasta la última fila con contenido
      for (numFila <- 0 to ultimaFila) {
        val filaActual = sheetInicial.getRow(numFila)

        if (filaActual != null) {
          val cellA = filaActual.getCell(0)

          if (cellA != null) {
            val valorCelda = getCellValue(cellA)

            // Si el valor es un número
            if (valorCelda.forall(_.isDigit)) {
              numerosEncontrados += ((numerosEncontrados.size + 1, valorCelda, numFila))
            }
          }
        }
      }

      // Mostrar lista de números
      println("\nNúmeros encontrados:")
      numerosEncontrados.foreach { case (indice, numero, fila) =>
        println(s"$indice. Número $numero (Fila ${fila + 1})")
      }

      // Solicitar selección de número
      println("\nIngrese el número de la lista que desea cortar y pegar (o 0 para cancelar):")
      val seleccion = StdIn.readLine().toInt

      if (seleccion > 0 && seleccion <= numerosEncontrados.size) {
        val (_, numeroSeleccionado, filaOrigen) = numerosEncontrados(seleccion - 1)

        // Encontrar la primera fila con columna A vacía
        var filaDestino = 0
        var encontrado = false
        var columnaDestino = 1 // Comenzar en columna B

        while (filaDestino < sheetFinal.getLastRowNum + 2 && !encontrado) {
          val filaActual = Option(sheetFinal.getRow(filaDestino)).getOrElse(sheetFinal.createRow(filaDestino))

          // Si la columna A está vacía
          if (getCellValue(filaActual.getCell(0)).isEmpty) {
            // Verificar si hay que considerar la fila anterior
            val tieneContenidoAnterior = filaDestino > 0 &&
              getCellValue(sheetFinal.getRow(filaDestino - 1).getCell(0)).nonEmpty

            // Si hay contenido en la fila anterior
            if (tieneContenidoAnterior) {
              // Buscar la primera columna vacía desde B
              columnaDestino = 1
              while (columnaDestino <= 10 &&
                filaActual.getCell(columnaDestino) != null &&
                getCellValue(filaActual.getCell(columnaDestino)).nonEmpty) {
                columnaDestino += 1
              }

              // Si encontramos una columna vacía
              if (columnaDestino <= 10) {
                filaActual.createCell(columnaDestino).setCellValue(numeroSeleccionado)
                encontrado = true
              }
            }
            // Si no hay contenido en la fila anterior o es la primera fila
            else {
              // Pegar en la primera columna vacía desde B
              columnaDestino = 1
              while (columnaDestino <= 10 &&
                filaActual.getCell(columnaDestino) != null &&
                getCellValue(filaActual.getCell(columnaDestino)).nonEmpty) {
                columnaDestino += 1
              }

              // Si encontramos una columna vacía
              if (columnaDestino <= 10) {
                filaActual.createCell(columnaDestino).setCellValue(numeroSeleccionado)
                encontrado = true
              }
            }
          }

          filaDestino += 1
        }

        if (encontrado) {
          // Borrar el número de la hoja Inicial
          val filaOri = sheetInicial.getRow(filaOrigen)
          val cellOri = filaOri.getCell(0)
          filaOri.removeCell(cellOri)

          // Guardar cambios
          val fos = new FileOutputStream(rutaExcel)
          workbook.write(fos)
          fos.close()
          workbook.close()
          fis.close()

          println(s"\nNúmero $numeroSeleccionado cortado de la fila ${filaOrigen + 1} y pegado en la columna ${numberToExcelColumn(columnaDestino)} de la fila $filaDestino")
          Some(true)
        } else {
          workbook.close()
          fis.close()
          println("No se encontró un lugar adecuado para pegar el número")
          Some(false)
        }
      } else {
        workbook.close()
        fis.close()
        println("Operación cancelada")
        Some(false)
      }

    } catch {
      case e: Exception =>
        println(s"Error: No se pudo cortar y pegar números: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  }

  // 3) FUNCIÓN SELECCIONAR ANIMAL
  def seleccionarAnimal(): Unit = {
    try {
      // Abrir archivo Excel
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        println(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal")

      if (sheet == null) {
        println("Error: No se encuentra la hoja 'Animal' en el archivo Excel")
        workbook.close()
        fis.close()
        return
      }

      // Función auxiliar para obtener el valor de una celda
      def getCellValue(cell: Cell): String = {
        if (cell == null) return ""
        cell.getCellType match {
          case CellType.NUMERIC =>
            val num = cell.getNumericCellValue
            if (num == num.floor) num.toInt.toString else num.toString
          case CellType.STRING => cell.getStringCellValue.trim
          case _ => ""
        }
      }

      // Recopilar nombres de animales
      val animales = scala.collection.mutable.ListBuffer[String]()

      // Recorrer desde la primera fila hasta la última
      for (numFila <- 0 to sheet.getLastRowNum) {
        val filaActual = sheet.getRow(numFila)

        if (filaActual != null) {
          val cellA = filaActual.getCell(0)

          if (cellA != null) {
            val valorCelda = getCellValue(cellA)

            // Si el valor no está vacío
            if (valorCelda.nonEmpty) {
              animales += valorCelda
            }
          }
        }
      }

      // Cerrar recursos
      workbook.close()
      fis.close()

      // Mostrar lista de animales
      if (animales.isEmpty) {
        println("No se encontraron animales en la hoja.")
        return
      }

      println("\nLista de Animales Disponibles:")
      animales.zipWithIndex.foreach { case (animal, index) =>
        println(s"${index + 1}. $animal")
      }

      // Solicitar selección de animal
      println("\nIngrese el número del animal que desea seleccionar (o 0 para cancelar):")
      val seleccion = StdIn.readLine().toInt

      if (seleccion > 0 && seleccion <= animales.size) {
        val nombreAnimal = animales(seleccion - 1)

        buscarAnimal(nombreAnimal) match {
          case Some(animal) =>
            animalActual = Some(animal)
            println(s"Animal ${animal.nombre} seleccionado para la sesión de retiro")
          case None =>
            println("No se pudo seleccionar el animal")
            animalActual = None
        }
      } else {
        println("Selección cancelada")
        animalActual = None
      }

    } catch {
      case e: Exception =>
        println(s"Error: No se pudo listar animales: ${e.getMessage}")
        e.printStackTrace()
        animalActual = None
    }
  }

  // 4) FUNCIÓN INICIAR SESIÓN DE RETIRO
  def iniciarSesionRetiro(): Option[Boolean] = {
    try {
      println(s"Inicializando sesión de retiro")

      // F8A: INICIAR sesión de retiro
      // %% Activa el estado de 'sesión de retiro' en el sistema %%
      // Verificar que hay un animal seleccionado
      if (animalActual.isEmpty) {
        println("Error: No hay animal identificado para inicializar sesión de retiro")
        return Some(false)
      }

      // Inicializar la sesión
      sesionRetiroIniciada = true

      // F8B: RETORNAR resultado de la inicialización
      // %% Confirma si la sesión de retiro se inició correctamente o no %%
      println("\nEstado de la sesión de retiro:")
      println(s"Animal identificado: ${animalActual.get.nombre}")
      println("Estado: INICIALIZADA")

      Some(true)

    } catch {
      case e: Exception =>
        println(s"Error: No se pudo inicializar sesión de retiro: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  }

  // 5) FUNCIÓN FINALIZAR SESIÓN DE RETIRO
  def finalizarSesionRetiro(): Option[Boolean] = {
    try {
      // Verificar si hay una sesión iniciada
      if (!sesionRetiroIniciada) {
        println("Error: No hay una sesión de retiro activa para finalizar")
        return Some(false)
      }

      // Verificar que hay un animal seleccionado
      if (animalActual.isEmpty) {
        println("Error: No hay animal identificado para finalizar la sesión de retiro")
        return Some(false)
      }

      // Preparar para finalizar la sesión
      println("\nFinalizando sesión de retiro:")
      println(s"Animal: ${animalActual.get.nombre}")

      // Pegar el nombre del animal en una ubicación específica
      val excelFile = new File(rutaExcel)
      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheetFinal = workbook.getSheet("Final")

      if (sheetFinal == null) {
        println("Error: No se encuentra la hoja 'Final' en el archivo Excel")
        workbook.close()
        fis.close()
        return Some(false)
      }

      // Encontrar la primera fila vacía en la columna A
      var filaDestino = 0
      while (filaDestino <= sheetFinal.getLastRowNum) {
        val filaActual = Option(sheetFinal.getRow(filaDestino)).getOrElse(sheetFinal.createRow(filaDestino))

        if (filaActual.getCell(0) == null || filaActual.getCell(0).getStringCellValue.trim.isEmpty) {
          // Crear celda y establecer el nombre del animal
          filaActual.createCell(0).setCellValue(animalActual.get.nombre)

          // Guardar cambios
          val fos = new FileOutputStream(rutaExcel)
          workbook.write(fos)
          fos.close()

          println("Nombre del animal agregado exitosamente")

          // Resetear variables
          sesionRetiroIniciada = false
          animalActual = None

          workbook.close()
          fis.close()

          return Some(true)
        }

        filaDestino += 1
      }

      // Si no se encontró fila vacía
      println("No se encontró espacio para agregar el nombre del animal")
      workbook.close()
      fis.close()
      Some(false)

    } catch {
      case e: Exception =>
        println(s"Error al finalizar sesión de retiro: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  }


  //CÓDIGO PRINCIPAL:
  def main(args: Array[String]): Unit = {
    // Paso 1: Seleccionar animal
    println("Selección de Animal:")
    seleccionarAnimal()

    // Verificar si se seleccionó correctamente un animal
    if (animalActual.isEmpty) {
      println("No se seleccionó ningún animal. Terminando programa.")
      return
    }

    // Paso 2: Iniciar sesión de retiro
    iniciarSesionRetiro() match {
      case Some(true) =>
        println("Sesión de retiro iniciada correctamente")
      case _ =>
        println("No se pudo iniciar la sesión de retiro")
        return
    }

    // Paso 3: Cortar y pegar números
    var continuarCortarPegar = true
    while (continuarCortarPegar) {
      cortarPegarNumeros() match {
        case Some(true) =>
          println("Proceso de cortar y pegar completado exitosamente")
        case Some(false) =>
          println("Proceso de cortar y pegar completado con advertencias")
        case None =>
          println("Proceso de cortar y pegar fallido")
      }

      // Preguntar si desea cortar y pegar otro número
      println("\n¿Desea cortar y pegar otro número? (s/n)")
      val respuesta = StdIn.readLine().toLowerCase
      continuarCortarPegar = respuesta == "s"
    }

    // Paso 4: Finalizar sesión de retiro
    finalizarSesionRetiro() match {
      case Some(true) =>
        println("Sesión de retiro finalizada correctamente")
      case Some(false) =>
        println("Hubo problemas al finalizar la sesión de retiro")
      case None =>
        println("Error crítico al finalizar la sesión de retiro")
    }
  }
}