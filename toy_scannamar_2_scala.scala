import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.{FileInputStream, File, FileOutputStream, PrintWriter, StringWriter}
import scala.jdk.CollectionConverters._
// scala.io.StdIn no se usará directamente, se usará el input handler
import scala.annotation.tailrec

object toy_scannamar_2 {

  // Define tipos para los manejadores que vendrán de la GUI
  type OutputHandler = String => Unit
  type InputHandler = String => Option[String] // El String es el prompt, Option[String] la entrada

  // Variables para almacenar los manejadores (con implementaciones por defecto para pruebas en CLI si fuera necesario)
  var out: OutputHandler = println // Por defecto, imprime a la consola
  var in: InputHandler = (prompt: String) => { print(prompt + " "); Some(scala.io.StdIn.readLine()) } // Por defecto, lee de la consola

  // Declaración de variables globales
  var sesionRetiroIniciada: Boolean = false
  var animalActual: Option[Animal] = None

  // Ruta al archivo Excel - ASEGÚRATE QUE ESTA RUTA SEA CORRECTA EN TU SISTEMA
  // O considera pasarla como parámetro o leerla de una configuración.
  val rutaExcel: String = "src\\main\\resources\\toy_excel_scannamar.xlsx"

  // Definición de las clases de datos
  case class Animal(nombre: String)

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

  // Función auxiliar para obtener el valor de una celda (usada internamente)
  private def getCellValue(cell: Cell): String = {
    if (cell == null) return ""
    cell.getCellType match {
      case CellType.NUMERIC =>
        val num = cell.getNumericCellValue
        if (num == num.floor) num.toInt.toString else num.toString
      case CellType.STRING => cell.getStringCellValue.trim
      case CellType.BLANK => ""
      case CellType.FORMULA => // Manejo básico de fórmulas
        try {
          cell.getStringCellValue.trim // Intenta obtener como string
        } catch {
          case _: IllegalStateException => // Si falla (ej. es numérica)
            try {
              val num = cell.getNumericCellValue
              if (num == num.floor) num.toInt.toString else num.toString
            } catch {
              case _: Exception => "" // Fallback final
            }
        }
      case _ => ""
    }
  }

  // 1) FUNCIÓN BUSCAR ANIMAL
  def buscarAnimal(nombreAnimal: String): Option[Animal] = {
    try {
      out(" ") // Espaciado intencional

      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        out(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return None
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal")

      if (sheet == null) {
        out("Error: No se encuentra la hoja 'Animal' en el archivo Excel")
        workbook.close()
        fis.close()
        return None
      }

      val animalOpt = sheet.iterator().asScala.drop(0).find { row =>
        val cell = row.getCell(0) // Columna A
        if (cell != null) {
          val valorCelda = getCellValue(cell)
          val coincide = valorCelda.equalsIgnoreCase(nombreAnimal)
          if (coincide) {
            out(s"\nEncontrado el animal en la columna A y en la fila ${row.getRowNum + 1}")
          }
          coincide
        } else false
      }.map { row =>
        val animal = Animal(nombre = getCellValue(row.getCell(0)))
        out("\nDatos del animal encontrado:")
        out(s"Nombre: ${animal.nombre}")
        animal
      }

      workbook.close()
      fis.close()

      if (animalOpt.isEmpty) {
        out("\nError: No se encontró ningún animal con ese nombre")
      }
      animalOpt
    } catch {
      case e: Exception =>
        out(s"Error: No se pudo buscar animal: ${e.getMessage}")
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        out(sw.toString())
        None
    }
  }

  // 2) FUNCIÓN CORTAR Y PEGAR NÚMEROS
  def cortarPegarNumeros(): Option[Boolean] = {
    try {
      out(s"Iniciando selección de números para cortar y pegar")

      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        out(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return None
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheetInicial = workbook.getSheet("Inicial")
      val sheetFinal = workbook.getSheet("Final")

      if (sheetInicial == null || sheetFinal == null) {
        out("Error: No se encuentran las hojas 'Inicial' o 'Final' en el archivo Excel")
        workbook.close()
        fis.close()
        return None
      }

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
        out("No se encontraron filas con contenido en la columna A de la hoja 'Inicial'")
        workbook.close()
        fis.close()
        return Some(false)
      }

      val numerosEncontrados = scala.collection.mutable.ListBuffer[(Int, String, Int)]()
      for (numFila <- 0 to ultimaFila) {
        val filaActual = sheetInicial.getRow(numFila)
        if (filaActual != null) {
          val cellA = filaActual.getCell(0)
          if (cellA != null) {
            val valorCelda = getCellValue(cellA)
            if (valorCelda.forall(_.isDigit) && valorCelda.nonEmpty) { // Asegurar que no esté vacío
              numerosEncontrados += ((numerosEncontrados.size + 1, valorCelda, numFila))
            }
          }
        }
      }

      if (numerosEncontrados.isEmpty) {
        out("No se encontraron números para cortar y pegar en la hoja 'Inicial'.")
        workbook.close()
        fis.close()
        return Some(false)
      }

      out("\nNúmeros encontrados en hoja 'Inicial':")
      numerosEncontrados.foreach { case (indice, numero, fila) =>
        out(s"$indice. Número $numero (Fila ${fila + 1})")
      }

      out("\nIngrese el número de la lista que desea cortar y pegar (o 0 para cancelar):")
      val seleccionStrOpt = in("Selección: ")
      val seleccionOpt = seleccionStrOpt.flatMap(s => scala.util.Try(s.toInt).toOption)


      seleccionOpt match {
        case Some(seleccion) if seleccion > 0 && seleccion <= numerosEncontrados.size =>
          val (_, numeroSeleccionado, filaOrigen) = numerosEncontrados(seleccion - 1)
          var filaDestinoIdx = 0 // Índice de fila para sheetFinal
          var encontrado = false
          var columnaDestinoLetra = ""


          // Encontrar la primera fila con columna A vacía o la siguiente fila si no hay ninguna.
          var primeraFilaVaciaA = -1
          var r = 0
          while (r <= sheetFinal.getLastRowNum +1 && primeraFilaVaciaA == -1) {
            val fila = Option(sheetFinal.getRow(r)).getOrElse(sheetFinal.createRow(r))
            if(getCellValue(fila.getCell(0)).isEmpty) {
              primeraFilaVaciaA = r
            }
            r += 1
          }
          if(primeraFilaVaciaA == -1) primeraFilaVaciaA = sheetFinal.getLastRowNum + 1 // Nueva fila si todo está lleno


          // Pegar en la hoja Final
          val filaAPegar = Option(sheetFinal.getRow(primeraFilaVaciaA)).getOrElse(sheetFinal.createRow(primeraFilaVaciaA))
          var colIdxDestino = 1 // Empezar en columna B
          while(colIdxDestino < 200 && getCellValue(filaAPegar.getCell(colIdxDestino)).nonEmpty) { // Busca hasta la columna ~GR
            colIdxDestino +=1
          }
          filaAPegar.createCell(colIdxDestino).setCellValue(numeroSeleccionado)
          columnaDestinoLetra = numberToExcelColumn(colIdxDestino)
          encontrado = true
          filaDestinoIdx = primeraFilaVaciaA


          if (encontrado) {
            val filaOri = sheetInicial.getRow(filaOrigen)
            val cellOri = filaOri.getCell(0) // Asume que el número está en la columna A (índice 0)
            if (cellOri != null) { // Solo remover si existe
              filaOri.removeCell(cellOri)
            }


            val fos = new FileOutputStream(rutaExcel)
            workbook.write(fos)
            fos.close()
            out(s"\nNúmero $numeroSeleccionado cortado de la fila ${filaOrigen + 1} de 'Inicial' y pegado en 'Final' en la columna $columnaDestinoLetra de la fila ${filaDestinoIdx + 1}")
            Some(true)
          } else {
            out("No se encontró un lugar adecuado para pegar el número en 'Final'")
            Some(false)
          }

        case Some(0) =>
          out("Operación cancelada por el usuario.")
          Some(false)
        case _ =>
          out("Selección inválida.")
          Some(false)
      }
    } catch {
      case e: Exception =>
        out(s"Error: No se pudo cortar y pegar números: ${e.getMessage}")
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        out(sw.toString())
        None
    } finally {
      // Asegurar que workbook y fis se cierren si se abrieron, aunque parte de la lógica ya lo hace.
      // Esta es una simplificación; un manejo más robusto usaría try-with-resources o similar.
    }
  }


  // 3) FUNCIÓN SELECCIONAR ANIMAL
  def seleccionarAnimalP(): Unit = { // Renombrada para evitar choque con la case class, y P por Principal
    try {
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        out(s"Error: No se encuentra el archivo Excel en: $rutaExcel")
        return
      }

      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal")

      if (sheet == null) {
        out("Error: No se encuentra la hoja 'Animal' en el archivo Excel")
        workbook.close()
        fis.close()
        return
      }

      val animales = scala.collection.mutable.ListBuffer[String]()
      for (numFila <- 0 to sheet.getLastRowNum) {
        val filaActual = sheet.getRow(numFila)
        if (filaActual != null) {
          val cellA = filaActual.getCell(0)
          if (cellA != null) {
            val valorCelda = getCellValue(cellA)
            if (valorCelda.nonEmpty) {
              animales += valorCelda
            }
          }
        }
      }
      workbook.close()
      fis.close()

      if (animales.isEmpty) {
        out("No se encontraron animales en la hoja 'Animal'.")
        return
      }

      out("\nLista de Animales Disponibles:")
      animales.zipWithIndex.foreach { case (animal, index) =>
        out(s"${index + 1}. $animal")
      }

      out("\nIngrese el número del animal que desea seleccionar (o 0 para cancelar):")
      val seleccionStrOpt = in("Selección: ")
      val seleccionOpt = seleccionStrOpt.flatMap(s => scala.util.Try(s.toInt).toOption)

      seleccionOpt match {
        case Some(seleccion) if seleccion > 0 && seleccion <= animales.size =>
          val nombreAnimal = animales(seleccion - 1)
          buscarAnimal(nombreAnimal) match {
            case Some(animalObj) => // Cambiado de 'animal' a 'animalObj' para evitar shadowing
              animalActual = Some(animalObj)
              out(s"Animal ${animalObj.nombre} seleccionado para la sesión.")
            case None =>
              out("No se pudo seleccionar el animal (no encontrado tras selección).")
              animalActual = None
          }
        case Some(0) =>
          out("Selección de animal cancelada.")
          animalActual = None
        case _ =>
          out("Selección de animal inválida.")
          animalActual = None
      }
    } catch {
      case e: Exception =>
        out(s"Error: No se pudo listar animales: ${e.getMessage}")
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        out(sw.toString())
        animalActual = None
    }
  }

  // 4) FUNCIÓN INICIAR SESIÓN DE RETIRO
  def iniciarSesionRetiro(): Option[Boolean] = {
    try {
      out(s"Intentando inicializar sesión de retiro...")
      if (animalActual.isEmpty) {
        out("Error: No hay animal identificado para inicializar sesión de retiro.")
        return Some(false)
      }

      sesionRetiroIniciada = true
      out("\nEstado de la sesión de retiro:")
      out(s"Animal identificado: ${animalActual.get.nombre}")
      out("Estado: INICIALIZADA")
      Some(true)
    } catch {
      case e: Exception =>
        out(s"Error: No se pudo inicializar sesión de retiro: ${e.getMessage}")
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        out(sw.toString())
        None
    }
  }

  // 5) FUNCIÓN FINALIZAR SESIÓN DE RETIRO
  def finalizarSesionRetiro(): Option[Boolean] = {
    try {
      if (!sesionRetiroIniciada) {
        out("Error: No hay una sesión de retiro activa para finalizar.")
        return Some(false)
      }
      if (animalActual.isEmpty) {
        out("Error: No hay animal identificado para finalizar la sesión de retiro.")
        return Some(false)
      }

      out("\nFinalizando sesión de retiro:")
      out(s"Animal en sesión: ${animalActual.get.nombre}")

      val excelFile = new File(rutaExcel)
      val fis = new FileInputStream(excelFile)
      val workbook = WorkbookFactory.create(fis)
      val sheetFinal = workbook.getSheet("Final")

      if (sheetFinal == null) {
        out("Error: No se encuentra la hoja 'Final' en el archivo Excel.")
        workbook.close()
        fis.close()
        return Some(false)
      }

      var filaDestinoIdx = 0
      var pegado = false
      // Busca la primera fila donde la columna A (índice 0) esté vacía.
      // Itera hasta una fila más allá de la última fila con contenido para permitir añadir al final.
      while (filaDestinoIdx <= sheetFinal.getLastRowNum + 1 && !pegado) {
        val filaActual = Option(sheetFinal.getRow(filaDestinoIdx)).getOrElse(sheetFinal.createRow(filaDestinoIdx))
        val celdaA = filaActual.getCell(0)

        if (celdaA == null || getCellValue(celdaA).trim.isEmpty) {
          celdaA match {
            case null => filaActual.createCell(0).setCellValue(animalActual.get.nombre)
            case c => c.setCellValue(animalActual.get.nombre)
          }
          pegado = true
        }
        if (!pegado) filaDestinoIdx += 1
      }


      if (pegado) {
        val fos = new FileOutputStream(rutaExcel)
        workbook.write(fos)
        fos.close()
        out(s"Nombre del animal ('${animalActual.get.nombre}') agregado a la hoja 'Final' en la fila ${filaDestinoIdx + 1}, columna A.")
        sesionRetiroIniciada = false
        animalActual = None
        workbook.close()
        fis.close()
        Some(true)
      } else {
        out("No se encontró espacio en la columna A de la hoja 'Final' para agregar el nombre del animal.")
        workbook.close()
        fis.close()
        Some(false)
      }

    } catch {
      case e: Exception =>
        out(s"Error al finalizar sesión de retiro: ${e.getMessage}")
        val sw = new StringWriter()
        e.printStackTrace(new PrintWriter(sw))
        out(sw.toString())
        None
    }
  }


  //CÓDIGO PRINCIPAL A SER LLAMADO DESDE LA GUI:
  def ejecutarToyLogicaPrincipal(): Unit = {
    out("--- Iniciando Proceso Toy Scannamar ---")

    // Paso 1: Seleccionar animal
    out("\n--- Paso 1: Selección de Animal ---")
    seleccionarAnimalP()

    if (animalActual.isEmpty) {
      out("No se seleccionó ningún animal. Terminando proceso.")
      out("--- Proceso Toy Scannamar Finalizado ---")
      return
    }

    // Paso 2: Iniciar sesión de retiro
    out("\n--- Paso 2: Iniciar Sesión ---")
    iniciarSesionRetiro() match {
      case Some(true) =>
        out("Sesión de retiro iniciada correctamente.")
      case _ =>
        out("No se pudo iniciar la sesión de retiro. Terminando proceso.")
        out("--- Proceso Toy Scannamar Finalizado ---")
        return
    }

    // Paso 3: Cortar y pegar números
    out("\n--- Paso 3: Cortar y Pegar Números ---")
    var continuarCortarPegar = true
    while (continuarCortarPegar) {
      cortarPegarNumeros() match {
        case Some(true) =>
          out("Proceso de cortar y pegar para este número completado exitosamente.")
        case Some(false) =>
          out("Proceso de cortar y pegar para este número completado con advertencias o cancelado.")
        case None =>
          out("Proceso de cortar y pegar para este número falló.")
      }

      out("\n¿Desea cortar y pegar otro número? (s/n)")
      val respuestaOpt = in("Respuesta (s/n): ")
      continuarCortarPegar = respuestaOpt.map(_.trim.toLowerCase).getOrElse("n") == "s"
    }

    // Paso 4: Finalizar sesión de retiro
    out("\n--- Paso 4: Finalizar Sesión ---")
    finalizarSesionRetiro() match {
      case Some(true) =>
        out("Sesión de retiro finalizada correctamente.")
      case Some(false) =>
        out("Hubo problemas al finalizar la sesión de retiro.")
      case None =>
        out("Error crítico al finalizar la sesión de retiro.")
    }
    out("--- Proceso Toy Scannamar Finalizado ---")
  }

  // Método main original (puede ser útil para pruebas directas en Scala sin GUI)
  def main(args: Array[String]): Unit = {
    // Configurar handlers para salida a consola (ya son los default, pero explícito)
    out = println
    in = (prompt: String) => { print(prompt); Some(scala.io.StdIn.readLine()) }
    ejecutarToyLogicaPrincipal()
  }
}