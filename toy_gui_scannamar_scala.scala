import org.apache.poi.ss.usermodel.{Cell, CellType, WorkbookFactory}
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{BorderLayout, Color}
import java.io.{File, FileInputStream, FileOutputStream}
import javax.swing.border.LineBorder
import javax.swing.text.{Style, StyleConstants, StyledDocument}
import javax.swing.{JFrame, JPanel, JScrollPane, JTextField, JTextPane}
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Try, Success, Failure} // Usar Try para manejar conversiones y errores

object toy_gui_scannamar {
  // --- Variables Globales de Estado ---
  var sesionRetiroIniciada: Boolean = false
  var animalActual: Option[Animal] = None

  // Ruta al archivo Excel (Considera hacerlo configurable)
  val rutaExcel = "src\\main\\resources\\toy_excel_scannamar.xlsx"

  // Clase de datos
  case class Animal(nombre: String)

  // --- Función Principal ---
  def main(args: Array[String]): Unit = {
    val marco = new Marco()
    marco.setVisible(true)
  }

  // --- Funciones de Utilidad ---

  // Función auxiliar para obtener el valor de una celda como String
  def getCellValue(cell: Cell): String = {
    if (cell == null) return ""
    Try { // Envuelve en Try para manejar errores potenciales de POI
      cell.getCellType match {
        case CellType.NUMERIC =>
          val num = cell.getNumericCellValue
          if (num == num.floor && !org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) num.toInt.toString else num.toString
        case CellType.STRING => cell.getStringCellValue.trim
        case CellType.FORMULA => // Evaluar fórmula si es posible (simple ejemplo)
          Try(cell.getSheet.getWorkbook.getCreationHelper.createFormulaEvaluator().evaluate(cell).getStringValue).getOrElse("")
        case CellType.BOOLEAN => cell.getBooleanCellValue.toString
        case CellType.BLANK => ""
        case _ => ""
      }
    }.getOrElse("") // Devuelve "" si hay algún error al obtener el valor
  }

  // Convierte índice de columna (0-based) a Letra Excel (A, B, ...)
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

  // --- Lógica de Negocio Refactorizada ---

  // 1a. MUESTRA la lista de animales disponibles (Lee de Excel)
  def mostrarAnimalesDisponibles(doc: StyledDocument, color: Style): Unit = {
    doc.insertString(doc.getLength(), "\n\nLista de Animales Disponibles:", color)
    var fis: FileInputStream = null
    var workbook: org.apache.poi.ss.usermodel.Workbook = null
    try {
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        doc.insertString(doc.getLength(), s"\nError: No se encuentra el archivo Excel en: $rutaExcel", color)
        return
      }
      fis = new FileInputStream(excelFile)
      workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal")
      if (sheet == null) {
        doc.insertString(doc.getLength(), "\nError: No se encuentra la hoja 'Animal'.", color)
        return
      }

      val animales = sheet.iterator().asScala.zipWithIndex.flatMap { case (row, idx) =>
        Option(row.getCell(0)).map(getCellValue).filter(_.nonEmpty).map(nombre => (idx + 1, nombre))
      }.toList

      if (animales.isEmpty) {
        doc.insertString(doc.getLength(), "\nNo se encontraron animales en la hoja.", color)
      } else {
        animales.foreach { case (index, animal) =>
          doc.insertString(doc.getLength(), s"\n$index. $animal", color)
        }
      }
      doc.insertString(doc.getLength(), "\n\nIngrese el número del animal que desea seleccionar:", color)

    } catch {
      case e: Exception =>
        doc.insertString(doc.getLength(), s"\nError al listar animales: ${e.getMessage}", color)
        e.printStackTrace()
    } finally {
      Try(workbook.close())
      Try(fis.close())
    }
  }

  // 1b. PROCESA la selección del animal (usa buscarAnimal)
  def seleccionarAnimalDesdeInput(userInput: String, doc: StyledDocument, color: Style): Option[Animal] = {
    Try(userInput.toInt) match {
      case Failure(_) =>
        doc.insertString(doc.getLength(), "\nEntrada inválida. Debe ingresar un número.", color)
        None
      case Success(seleccionIndex) =>
        // Necesitamos obtener la lista de animales nuevamente para mapear el índice al nombre
        var fis: FileInputStream = null
        var workbook: org.apache.poi.ss.usermodel.Workbook = null
        try {
          fis = new FileInputStream(rutaExcel)
          workbook = WorkbookFactory.create(fis)
          val sheet = workbook.getSheet("Animal")
          if (sheet == null) return None // Error ya reportado en mostrarAnimalesDisponibles

          val animales = sheet.iterator().asScala
            .map(row => Option(row.getCell(0)).map(getCellValue).filter(_.nonEmpty))
            .collect { case Some(nombre) => nombre }
            .toList

          if (seleccionIndex > 0 && seleccionIndex <= animales.size) {
            val nombreAnimalSeleccionado = animales(seleccionIndex - 1)
            // Usar buscarAnimal para confirmar y obtener el objeto (aunque ya tenemos el nombre)
            buscarAnimal(nombreAnimalSeleccionado, doc, color) match {
              case Some(animal) =>
                animalActual = Some(animal) // Actualiza estado global
                doc.insertString(doc.getLength(), s"\nAnimal '${animal.nombre}' seleccionado.", color)
                Some(animal)
              case None =>
                doc.insertString(doc.getLength(), s"\nError: No se pudo encontrar el animal '$nombreAnimalSeleccionado' aunque estaba en la lista.", color)
                animalActual = None
                None
            }
          } else {
            doc.insertString(doc.getLength(), s"\nNúmero de selección '$seleccionIndex' fuera de rango.", color)
            animalActual = None
            None
          }
        } catch {
          case e: Exception =>
            doc.insertString(doc.getLength(), s"\nError al procesar selección de animal: ${e.getMessage}", color)
            e.printStackTrace()
            animalActual = None
            None
        } finally {
          Try(workbook.close())
          Try(fis.close())
        }
    }
  }

  // Función interna buscarAnimal (modificada ligeramente para no imprimir tanto)
  def buscarAnimal(nombreAnimal: String, doc: StyledDocument, color : Style): Option[Animal] = {
    var fis: FileInputStream = null
    var workbook: org.apache.poi.ss.usermodel.Workbook = null
    try{
      val excelFile = new File(rutaExcel)
      if(!excelFile.exists()){
        // Error ya manejado por las funciones que llaman
        return None
      }
      fis = new FileInputStream(excelFile)
      workbook = WorkbookFactory.create(fis)
      val sheet = workbook.getSheet("Animal")
      if (sheet == null) return None // Error ya manejado

      sheet.iterator().asScala.drop(0).find { row =>
        Option(row.getCell(0)).map(getCellValue).exists(_.equalsIgnoreCase(nombreAnimal))
      }.map { row =>
        Animal(getCellValue(row.getCell(0)))
      }
    } catch {
      case e: Exception =>
        doc.insertString(doc.getLength(), s"\nError interno buscando animal: ${e.getMessage}", color)
        e.printStackTrace()
        None
    } finally {
      Try(workbook.close())
      Try(fis.close())
    }
  }


  // 2. Intenta iniciar la sesión de retiro
  def iniciarSesionRetiroSimple(doc: StyledDocument, color: Style): Boolean = {
    if (animalActual.isEmpty) {
      doc.insertString(doc.getLength(), "\nError: No hay animal identificado para iniciar sesión.", color)
      false
    } else {
      sesionRetiroIniciada = true
      doc.insertString(doc.getLength(), s"\nSesión de retiro iniciada para: ${animalActual.get.nombre}.", color)
      true
    }
  }

  // 3a. OBTIENE la lista de números disponibles de la hoja "Inicial"
  def obtenerNumerosDisponibles(doc: StyledDocument, color: Style): List[(Int, String, Int)] = {
    var fis: FileInputStream = null
    var workbook: org.apache.poi.ss.usermodel.Workbook = null
    val numerosEncontrados = scala.collection.mutable.ListBuffer[(Int, String, Int)]()
    try {
      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        doc.insertString(doc.getLength(), s"\nError: No se encuentra el archivo Excel en: $rutaExcel", color)
        return List()
      }
      fis = new FileInputStream(excelFile)
      workbook = WorkbookFactory.create(fis)
      val sheetInicial = workbook.getSheet("Inicial")
      if (sheetInicial == null) {
        doc.insertString(doc.getLength(), "\nError: No se encuentra la hoja 'Inicial'.", color)
        return List()
      }

      // Encontrar la última fila con contenido en la columna A.
      var ultimaFila = -1
      sheetInicial.iterator().asScala.foreach { currentRow =>
        val cellA = currentRow.getCell(0)
        if (cellA != null && getCellValue(cellA).nonEmpty) {
          ultimaFila = currentRow.getRowNum
        }
      }

      if (ultimaFila != -1) {
        // Recorrer desde la primera fila hasta la última fila con contenido.
        for (numFila <- 0 to ultimaFila) {
          val filaActual = sheetInicial.getRow(numFila)
          if (filaActual != null) {
            val cellA = filaActual.getCell(0)
            if (cellA != null) {
              val valorCelda = getCellValue(cellA)
              // Si el valor es un número (puede tener decimales, ajusta si solo quieres enteros)
              if (Try(valorCelda.toDouble).isSuccess) {
                numerosEncontrados += ((numerosEncontrados.size + 1, valorCelda, numFila))
              }
            }
          }
        }
      } else {
        doc.insertString(doc.getLength(), "\nNo se encontraron filas con contenido en la columna A de la hoja 'Inicial'.", color)
      }

    } catch {
      case e: Exception =>
        doc.insertString(doc.getLength(), s"\nError al obtener números disponibles: ${e.getMessage}", color)
        e.printStackTrace()
    } finally {
      Try(workbook.close())
      Try(fis.close())
    }
    numerosEncontrados.toList
  }

  // 3b. MUESTRA la lista de números y el prompt
  def mostrarNumerosYPrompt(numerosDisponibles: List[(Int, String, Int)], doc: StyledDocument, color: Style): Unit = {
    if (numerosDisponibles.isEmpty) {
      doc.insertString(doc.getLength(), "\nNo hay números disponibles para cortar y pegar en la hoja 'Inicial'.", color)
      // ¿Qué hacer aquí? El listener debería manejar este caso.
    } else {
      doc.insertString(doc.getLength(), "\n\nNúmeros disponibles en hoja 'Inicial':", color)
      numerosDisponibles.foreach { case (indice, numero, fila) =>
        doc.insertString(doc.getLength(), s"\n$indice. Número $numero (Fila original ${fila + 1})", color)
      }
      doc.insertString(doc.getLength(), "\n\nIngrese el número de la lista que desea cortar y pegar (o 0 para cancelar):", color)
    }
  }

  // 3c. PROCESA el corte y pegado basado en la selección del usuario
  def procesarCorteYPegado(userInput: String, numerosDisponibles: List[(Int, String, Int)], doc: StyledDocument, color: Style): Option[Boolean] = {
    Try(userInput.toInt) match {
      case Failure(_) =>
        doc.insertString(doc.getLength(), "\nEntrada inválida. Debe ingresar un número.", color)
        None // Indica error
      case Success(seleccion) =>
        if (seleccion == 0) {
          doc.insertString(doc.getLength(), "\nOperación cancelada por el usuario.", color)
          Some(false) // Indica cancelación/advertencia
        } else if (seleccion > 0 && seleccion <= numerosDisponibles.size) {
          val (_, numeroSeleccionado, filaOrigen) = numerosDisponibles(seleccion - 1)

          var fis: FileInputStream = null
          var fos: FileOutputStream = null
          var workbook: org.apache.poi.ss.usermodel.Workbook = null
          var exito = false
          try {
            fis = new FileInputStream(rutaExcel)
            workbook = WorkbookFactory.create(fis)
            val sheetInicial = workbook.getSheet("Inicial")
            val sheetFinal = workbook.getSheet("Final")

            if (sheetInicial == null || sheetFinal == null) {
              doc.insertString(doc.getLength(), "\nError: No se encuentran las hojas 'Inicial' o 'Final'.", color)
              return None // Error grave
            }

            // --- Lógica para encontrar celda destino en Hoja "Final" ---
            var filaDestinoIdx = 0
            var columnaDestinoIdx = -1 // -1 indica no encontrada

            while(columnaDestinoIdx == -1 && filaDestinoIdx < 65536) { // Limite arbitrario
              val filaActualFinal = Option(sheetFinal.getRow(filaDestinoIdx)).getOrElse(sheetFinal.createRow(filaDestinoIdx))
              val celdaA = filaActualFinal.getCell(0) // Celda A de la fila actual en "Final"

              // Si celda A está vacía O si es la primera fila (fila 0)
              if (celdaA == null || getCellValue(celdaA).isEmpty) {
                // Buscar primera columna vacía desde B (índice 1) hasta K (índice 10)
                var colIdx = 1
                var foundEmptyCol = false
                while(colIdx <= 10 && !foundEmptyCol) {
                  val celdaActual = filaActualFinal.getCell(colIdx)
                  if (celdaActual == null || getCellValue(celdaActual).isEmpty) {
                    columnaDestinoIdx = colIdx
                    foundEmptyCol = true
                  }
                  colIdx += 1
                }
                // Si no encontramos columna vacía en esta fila, probaremos la siguiente fila
              }
              if (columnaDestinoIdx == -1) filaDestinoIdx += 1 // Avanzar a la siguiente fila si no se encontró destino
            }
            // --- Fin Lógica encontrar celda destino ---


            if (columnaDestinoIdx != -1) {
              // Pegar en Hoja Final
              val filaDest = Option(sheetFinal.getRow(filaDestinoIdx)).getOrElse(sheetFinal.createRow(filaDestinoIdx))
              filaDest.createCell(columnaDestinoIdx).setCellValue(numeroSeleccionado)

              // Borrar de Hoja Inicial (Obtener la celda A de la fila original y ponerla en blanco)
              val filaOri = sheetInicial.getRow(filaOrigen)
              if (filaOri != null) {
                val cellOri = filaOri.getCell(0) // Asumimos que siempre está en columna A (índice 0)
                if (cellOri != null) {
                  filaOri.removeCell(cellOri) // Opcionalmente: cellOri.setBlank()
                }
              }

              // Guardar cambios
              Try(fis.close()) // Cerrar FIS antes de abrir FOS sobre el mismo archivo
              fos = new FileOutputStream(rutaExcel)
              workbook.write(fos)
              exito = true
              doc.insertString(doc.getLength(), s"\n\nÉxito: Número '$numeroSeleccionado' movido de Fila ${filaOrigen + 1} (Inicial) a Fila ${filaDestinoIdx + 1}, Columna ${numberToExcelColumn(columnaDestinoIdx)} (Final).", color)

            } else {
              doc.insertString(doc.getLength(), "\nError: No se encontró una celda destino adecuada en la hoja 'Final' (Columnas B-K).", color)
              // No se considera error grave, sino advertencia/fallo operativo
            }

          } catch {
            case e: Exception =>
              doc.insertString(doc.getLength(), s"\nError durante el corte y pegado: ${e.getMessage}", color)
              e.printStackTrace()
              return None // Indica error grave
          } finally {
            Try(workbook.close())
            Try(fis.close()) // Asegurar cierre si no se cerró antes
            Try(fos.close())
          }
          Some(exito) // Devuelve true si se guardó, false si no se encontró destino

        } else {
          doc.insertString(doc.getLength(), s"\nNúmero de selección '$seleccion' fuera de rango.", color)
          None // Indica error
        }
    }
  }

  // 4) FUNCIÓN FINALIZAR SESIÓN DE RETIRO (Modificada para GUI)
  def finalizarSesionRetiro(doc: StyledDocument, color: Style): Option[Boolean] = {
    var fis: FileInputStream = null
    var fos: FileOutputStream = null
    var workbook: org.apache.poi.ss.usermodel.Workbook = null
    var success = false // Para controlar el retorno

    try {
      // Verificar si hay una sesión iniciada
      if (!sesionRetiroIniciada) {
        doc.insertString(doc.getLength(), "\nError: No hay una sesión de retiro activa para finalizar.", color)
        return Some(false)
      }

      // Verificar que hay un animal seleccionado
      if (animalActual.isEmpty) {
        doc.insertString(doc.getLength(), "\nError: No hay animal identificado para finalizar la sesión.", color)
        return Some(false)
      }

      // Preparar para finalizar la sesión
      doc.insertString(doc.getLength(), s"\n\nFinalizando sesión de retiro para: ${animalActual.get.nombre}...", color)

      val excelFile = new File(rutaExcel)
      if (!excelFile.exists()) {
        doc.insertString(doc.getLength(), s"\nError: No se encuentra el archivo Excel en: $rutaExcel", color)
        return None // Error grave si el archivo desapareció
      }

      fis = new FileInputStream(excelFile)
      workbook = WorkbookFactory.create(fis)
      val sheetFinal = workbook.getSheet("Final")

      if (sheetFinal == null) {
        doc.insertString(doc.getLength(), "\nError: No se encuentra la hoja 'Final' en el archivo Excel.", color)
        return Some(false) // No es un error crítico de la app, pero la operación falla
      }

      // Encontrar la primera fila con columna A vacía
      var filaDestinoIdx = -1
      var checkRow = 0
      while (filaDestinoIdx == -1 && checkRow <= sheetFinal.getLastRowNum + 1) { // Buscar una fila más allá de la última usada
        val filaActual = Option(sheetFinal.getRow(checkRow)).getOrElse(sheetFinal.createRow(checkRow))
        val celdaA = filaActual.getCell(0)
        if (celdaA == null || getCellValue(celdaA).isEmpty) {
          filaDestinoIdx = checkRow
        }
        checkRow += 1
      }

      if (filaDestinoIdx != -1) {
        // Crear celda y establecer el nombre del animal
        val filaDest = Option(sheetFinal.getRow(filaDestinoIdx)).getOrElse(sheetFinal.createRow(filaDestinoIdx))
        filaDest.createCell(0).setCellValue(animalActual.get.nombre) // Pega en Col A (índice 0)

        // Guardar cambios (cerrar fis ANTES de abrir fos)
        Try(fis.close())
        fis = null // Marcar como cerrado

        fos = new FileOutputStream(rutaExcel)
        workbook.write(fos)

        doc.insertString(doc.getLength(), s"\nNombre del animal agregado exitosamente en Fila ${filaDestinoIdx + 1} de Hoja 'Final'.", color)

        // Resetear variables GLOBALES solo si la escritura fue exitosa
        sesionRetiroIniciada = false
        animalActual = None
        success = true

      } else {
        // Si no se encontró fila vacía (muy improbable si buscamos +1)
        doc.insertString(doc.getLength(), "\nAdvertencia: No se encontró una fila vacía en Columna A de Hoja 'Final' para agregar el nombre del animal.", color)
        success = false // La operación no se completó como se esperaba
      }

      Some(success) // Devolver true si se escribió, false si no

    } catch {
      case e: Exception =>
        doc.insertString(doc.getLength(), s"\nError crítico al finalizar sesión de retiro: ${e.getMessage}", color)
        e.printStackTrace()
        None // Indica error grave
    } finally {
      // Asegurar cierre de todos los recursos
      Try(fos.close())
      Try(workbook.close())
      if (fis != null) Try(fis.close()) // Cerrar fis si no se cerró antes
    }
  }


  // --- Clases de la GUI ---

  // Clase Marco (JFrame) - Sin cambios
  class Marco extends JFrame {
    val VERSION = "v1.1.0-Refactored" // Versión actualizada
    setTitle("Toy Scannamar " + VERSION)
    setSize(800, 500)
    setResizable(false)
    setLocationRelativeTo(null)
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val lamina = new Lamina()
    add(lamina)
  }

  // Clase Lamina (JPanel) - Con estado y listener modificado
  class Lamina extends JPanel {
    setLayout(new BorderLayout())

    // Componentes GUI
    val entrada = new JTextField()
    entrada.setBackground(Color.BLACK)
    entrada.setForeground(Color.WHITE)
    entrada.setBorder(new LineBorder(Color.BLACK))

    val terminal = new JTextPane()
    terminal.setBackground(Color.BLACK)
    terminal.setForeground(Color.WHITE)
    terminal.setEditable(false)

    val doc: StyledDocument = terminal.getStyledDocument()
    val verde: Style = terminal.addStyle("Verde", null)
    StyleConstants.setForeground(verde, Color.GREEN)
    val blanco: Style = terminal.addStyle("Blanco", null)
    StyleConstants.setForeground(blanco, Color.WHITE)

    val barra = new JScrollPane(terminal)
    barra.setBorder(new LineBorder(Color.BLACK))

    // --- Estado de la Interfaz ---
    var esperandoInputPara: String = "animal" // "animal", "numero", "continuar"
    var numerosDisponiblesActuales: List[(Int, String, Int)] = List()

    // --- Inicialización ---
    add(entrada, BorderLayout.SOUTH)
    add(barra, BorderLayout.CENTER)
    entrada.addActionListener(new entradaListener)

    // Mostrar el primer prompt
    terminal.setText("Iniciando Toy Scannamar...") // Limpiar texto inicial
    mostrarAnimalesDisponibles(doc, blanco)
    entrada.requestFocusInWindow() // Poner foco en el campo de entrada


    // --- Listener Refactorizado ---
    class entradaListener extends ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        val userInput = entrada.getText()

        // Mostrar entrada del usuario en la terminal
        Try(doc.insertString(doc.getLength(), "\n> " + userInput, verde))

        // Limpiar campo de entrada para el siguiente input
        entrada.setText(null)

        // --- Lógica basada en el estado `esperandoInputPara` ---
        if (esperandoInputPara == "animal") {
          seleccionarAnimalDesdeInput(userInput, doc, blanco) match {
            case Some(_) => // Animal seleccionado correctamente
              if (iniciarSesionRetiroSimple(doc, blanco)) {
                // Obtener y mostrar números
                numerosDisponiblesActuales = obtenerNumerosDisponibles(doc, blanco)
                if (numerosDisponiblesActuales.nonEmpty) {
                  mostrarNumerosYPrompt(numerosDisponiblesActuales, doc, blanco)
                  esperandoInputPara = "numero" // Avanzar al siguiente estado
                } else {
                  doc.insertString(doc.getLength(), "\nNo hay números para procesar. Volviendo a selección de animal.", blanco)
                  // Volver al estado inicial
                  animalActual = None
                  sesionRetiroIniciada = false
                  mostrarAnimalesDisponibles(doc, blanco)
                  esperandoInputPara = "animal"
                }
              } else {
                // Falló inicio de sesión, volver a pedir animal
                animalActual = None
                mostrarAnimalesDisponibles(doc, blanco)
                esperandoInputPara = "animal"
              }
            case None => // Falló la selección del animal
              // Mensaje de error ya mostrado por seleccionarAnimalDesdeInput
              // Volver a pedir animal
              mostrarAnimalesDisponibles(doc, blanco)
              esperandoInputPara = "animal"
          }

        } else if (esperandoInputPara == "numero") {
          procesarCorteYPegado(userInput, numerosDisponiblesActuales, doc, blanco) match {
            case Some(_) => // Operación intentada (éxito o cancelación/fallo operativo)
              doc.insertString(doc.getLength(), "\n\n¿Desea procesar otro número? (s/n)", blanco)
              esperandoInputPara = "continuar" // Avanzar al siguiente estado
            case None => // Error grave durante el procesamiento
              doc.insertString(doc.getLength(), "\nError grave. Volviendo a selección de animal.", blanco)
              // Resetear estado
              animalActual = None
              sesionRetiroIniciada = false
              mostrarAnimalesDisponibles(doc, blanco)
              esperandoInputPara = "animal"
          }

        } else if (esperandoInputPara == "continuar") {
          userInput.toLowerCase match {
            case "s" =>
              // Volver a obtener y mostrar números
              numerosDisponiblesActuales = obtenerNumerosDisponibles(doc, blanco)
              if (numerosDisponiblesActuales.nonEmpty) {
                mostrarNumerosYPrompt(numerosDisponiblesActuales, doc, blanco)
                esperandoInputPara = "numero" // Volver al estado de selección de número
              } else {
                doc.insertString(doc.getLength(), "\nNo quedan números para procesar. Volviendo a selección de animal.", blanco)
                // Volver al estado inicial
                animalActual = None
                sesionRetiroIniciada = false
                mostrarAnimalesDisponibles(doc, blanco)
                esperandoInputPara = "animal"
              }
            case "n" =>
              // --- >>> INICIO: Bloque Modificado para 'n' <<< ---
              // 1. Intentar finalizar la sesión actual
              finalizarSesionRetiro(doc, blanco) match {
                case Some(true) =>
                  doc.insertString(doc.getLength(), "\nSesión de retiro finalizada correctamente.", blanco)
                // El estado global (animalActual, sesionRetiroIniciada) ya fue reseteado DENTRO de finalizarSesionRetiro
                case Some(false) =>
                  doc.insertString(doc.getLength(), "\nLa sesión no pudo ser finalizada correctamente (ver mensajes anteriores).", blanco)
                  // AUN ASÍ, reseteamos el estado local para permitir seleccionar nuevo animal
                  animalActual = None
                  sesionRetiroIniciada = false
                case None =>
                  doc.insertString(doc.getLength(), "\nOcurrió un error crítico al finalizar la sesión.", blanco)
                  // AUN ASÍ, reseteamos el estado local para permitir seleccionar nuevo animal
                  animalActual = None
                  sesionRetiroIniciada = false
              }

              // 2. Volver al inicio (selección de animal) independientemente del resultado de finalizar
              doc.insertString(doc.getLength(), "\n\nSeleccione un nuevo animal:", blanco)
              mostrarAnimalesDisponibles(doc, blanco) // Usa la función refactorizada
              esperandoInputPara = "animal" // Volver al estado inicial
            // --- >>> FIN: Bloque Modificado para 'n' <<< ---

            case _ =>
              // Entrada inválida, seguir esperando 's' o 'n'
              doc.insertString(doc.getLength(), "\nEntrada inválida. Por favor ingrese 's' o 'n'.", blanco)
            // No cambiar estado, seguir en "continuar"
          }
        }
        terminal.setCaretPosition(doc.getLength());
      }
    } // Fin entradaListener
  } // Fin Lamina
} // Fin ToyScannamar