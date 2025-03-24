import scala.io.StdIn

object PARES_E_IMPARES {

  //Función Sumar 7 y verificar paridad
  def sumaSieteYVerificaParidad(numero: Int): String = {
    val resultado = numero + 7

    if (resultado % 2 == 0) {
      s"$resultado es par"
    } else {
      s"$resultado es impar"
    }
  }


  // CÓDIGO PRINCIPAL:
  def main(args: Array[String]): Unit = {
    var continuar = true

    while (continuar) {
      println("Ingrese un número:")

      try {
        val numero = StdIn.readInt()

        println(sumaSieteYVerificaParidad(numero))

        println("¿Desea realizar otra operación? (S/N)")
        val respuesta = StdIn.readLine().trim.toLowerCase

        continuar = respuesta == "s"
      } catch {
        case _: NumberFormatException =>
          println("Por favor ingrese un número válido")
      }
    }

    println("Programa finalizado.")
  }
}