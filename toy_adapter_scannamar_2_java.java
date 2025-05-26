import javax.swing.SwingUtilities;

import scala.Option;
import scala.Function1;
import scala.runtime.BoxedUnit;

// Asumiendo que toy_scannamar_2.scala se compila y está accesible
// Si está en un paquete, ej. com.example, el import sería:
// import com.example.toy_scannamar_2$;

public class toy_adapter_scannamar_2 {
    private final toy_gui_scannamar_2 gui;

    public toy_adapter_scannamar_2(toy_gui_scannamar_2 gui) {
        this.gui = gui;
    }

    public void ejecutarScannamar() {
        // 1. Definir el OutputHandler para la GUI
        Function1<String, BoxedUnit> outputToGui = message -> {
            SwingUtilities.invokeLater(() -> gui.appendToMensajes(message));
            return BoxedUnit.UNIT; // Retorno para funciones Scala Unit
        };

        // 2. Definir el InputHandler para la GUI
        Function1<String, Option<String>> inputFromGui = prompt -> {
            // JOptionPane.showInputDialog se ejecuta en el EDT y es modal.
            // Esto pausará el hilo de Scannamar hasta que el usuario responda.
            String input = gui.showInputDialog(prompt); // Llama al método de la GUI
            return Option.apply(input); // Convierte a Scala Option (null se vuelve None)
        };

        // 3. Configurar los handlers en el objeto toy_scannamar_2 de Scala
        // (Asegúrate que `out` e `in` sean `var` públicas en el objeto Scala)
        // Si toy_scannamar_2 está en el paquete por defecto:
        toy_scannamar_2$.MODULE$.out_$eq(outputToGui);
        toy_scannamar_2$.MODULE$.in_$eq(inputFromGui);


        // 4. Ejecutar la lógica principal de Scannamar en un nuevo hilo
        new Thread(() -> {
            try {
                gui.clearMensajes(); // Limpiar mensajes anteriores en la GUI
                // Llamar a la función principal de la lógica de Scala
                toy_scannamar_2$.MODULE$.ejecutarToyLogicaPrincipal();
            } catch (Exception e) {
                // Enviar cualquier excepción inesperada a la GUI también
                SwingUtilities.invokeLater(() -> {
                    gui.appendToMensajes("ERROR INESPERADO EN toy_scannamar_2: " + e.getMessage());
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    gui.appendToMensajes(sw.toString());
                });
            }
        }).start();
    }
}