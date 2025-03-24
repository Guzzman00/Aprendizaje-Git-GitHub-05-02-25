import javax.swing._
import javax.swing.text._
import java.awt._

object ParesEImparesTerminal {
  def main(args: Array[String]): Unit = {
    SwingUtilities.invokeLater(() => {
      val terminal = new ParesEImparesTerminal()
      terminal.setVisible(true)
    })
  }
}

class ParesEImparesTerminal extends JFrame {
  // Declaración de variables de instancia al inicio de la clase
  private val styleContext: StyleContext = new StyleContext()
  private val document: DefaultStyledDocument = new DefaultStyledDocument(styleContext)
  private val outputPane: JTextPane = new JTextPane(document)
  private val inputField: JTextField = new JTextField()

  // Estilos
  private val inputStyle: Style = styleContext.addStyle("Input", null)
  private val outputStyle: Style = styleContext.addStyle("Output", null)
  private val errorStyle: Style = styleContext.addStyle("Error", null)

  // Variables de estado
  private var awaitingResponse: Boolean = false
  private var continuar: Boolean = true

  // Constructor
  {
    // Configuración del frame
    setTitle("Pares e Impares - Terminal")
    setSize(800, 600)
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    // Configuración de estilos
    StyleConstants.setForeground(inputStyle, Color.WHITE)
    StyleConstants.setBackground(inputStyle, Color.BLACK)

    StyleConstants.setForeground(outputStyle, Color.GREEN.brighter())
    StyleConstants.setBackground(outputStyle, Color.BLACK)

    StyleConstants.setForeground(errorStyle, Color.RED)
    StyleConstants.setBackground(errorStyle, Color.BLACK)

    // Configuración de la terminal
    outputPane.setBackground(Color.BLACK)
    outputPane.setForeground(Color.GREEN.brighter())
    outputPane.setFont(new Font("Monospaced", Font.PLAIN, 14))
    outputPane.setEditable(false)

    // Configuración del campo de entrada
    inputField.setBackground(Color.BLACK)
    inputField.setForeground(Color.WHITE)
    inputField.setCaretColor(Color.WHITE)
    inputField.setFont(new Font("Monospaced", Font.PLAIN, 14))

    // Layout
    setLayout(new BorderLayout())
    add(new JScrollPane(outputPane), BorderLayout.CENTER)
    add(inputField, BorderLayout.SOUTH)

    // Listener para entrada
    inputField.addActionListener(_ => {
      procesarEntrada(inputField.getText)
      inputField.setText("")
    })

    // Iniciar programa
    iniciarPrograma()
  }

  private def iniciarPrograma(): Unit = {
    appendOutput("Ingrese un número:", outputStyle)
    awaitingResponse = true
  }

  private def procesarEntrada(entrada: String): Unit = {
    if (!awaitingResponse) return

    appendOutput(entrada, inputStyle)

    try {
      if (continuar) {
        // Primera entrada (número)
        val numero = entrada.toInt
        val resultado = sumaSieteYVerificaParidad(numero)
        appendOutput(resultado, outputStyle)

        appendOutput("¿Desea realizar otra operación? (S/N)", outputStyle)
        awaitingResponse = true
        continuar = false
      } else {
        // Respuesta de continuación
        val respuesta = entrada.trim.toLowerCase
        if (respuesta == "s") {
          continuar = true
          appendOutput("Ingrese un número:", outputStyle)
        } else {
          appendOutput("Programa finalizado.", outputStyle)
          awaitingResponse = false
          inputField.setEnabled(false)
        }
      }
    } catch {
      case _: NumberFormatException =>
        appendOutput("Por favor ingrese un número válido", errorStyle)
        if (!continuar) {
          appendOutput("¿Desea realizar otra operación? (S/N)", outputStyle)
        }
    }
  }

  // Función equivalente a la de Scala
  private def sumaSieteYVerificaParidad(numero: Int): String = {
    val resultado = numero + 7
    if (resultado % 2 == 0) s"$resultado es par"
    else s"$resultado es impar"
  }

  // Método para agregar texto al área de salida
  private def appendOutput(text: String, style: Style): Unit = {
    try {
      document.insertString(document.getLength, text + "\n", style)
      // Scroll automático
      outputPane.setCaretPosition(document.getLength)
    } catch {
      case e: BadLocationException =>
        e.printStackTrace()
    }
  }
}