import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class toy_gui_scannamar_2 extends JFrame {
    private JTextArea mensajesTextArea;
    private JTextArea animalInfoTextArea; // Panel superior izquierdo, ahora para "Animal"
    private JTextArea estadoHojaFinalTextArea;   // Panel superior derecho
    private JButton btnIniciarProceso;
    private toy_adapter_scannamar_2 scannamarAdapter;

    public toy_gui_scannamar_2() {
        setTitle("Toy Scannamar GUI v2.0"); // Nombre actualizado
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5)); // Añadido espaciado general
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5)); // Margen para la ventana

        // --- Paneles Superiores ---
        animalInfoTextArea = new JTextArea(5, 25); // Ajustar filas/columnas según necesidad
        animalInfoTextArea.setEditable(false);
        JScrollPane scrollAnimal = new JScrollPane(animalInfoTextArea);
        scrollAnimal.setBorder(BorderFactory.createTitledBorder("Animal Actual")); // Título cambiado

        estadoHojaFinalTextArea = new JTextArea(5, 25);
        estadoHojaFinalTextArea.setEditable(false);
        JScrollPane scrollHojaFinal = new JScrollPane(estadoHojaFinalTextArea);
        scrollHojaFinal.setBorder(BorderFactory.createTitledBorder("Estado Hoja Final")); // Título ejemplo

        // --- Panel "mensajes" (Burdeo) ---
        mensajesTextArea = new JTextArea(15, 50); // Ajustar filas/columnas
        mensajesTextArea.setEditable(false);
        mensajesTextArea.setBackground(new Color(128, 0, 0)); // Color vino (0x800000)
        mensajesTextArea.setForeground(Color.WHITE);
        mensajesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Ligeramente más grande
        mensajesTextArea.setLineWrap(true); // Para que el texto se ajuste
        mensajesTextArea.setWrapStyleWord(true); // Ajustar por palabras
        JScrollPane scrollMensajes = new JScrollPane(mensajesTextArea);
        scrollMensajes.setBorder(BorderFactory.createTitledBorder("Mensajes del Proceso"));

        // --- Layout con JSplitPane ---
        // Panel izquierdo: Animal arriba, Mensajes abajo
        JPanel panelIzquierdoContenido = new JPanel(new BorderLayout(0,5));
        panelIzquierdoContenido.add(scrollAnimal, BorderLayout.NORTH);
        panelIzquierdoContenido.add(scrollMensajes, BorderLayout.CENTER);

        // Panel derecho: Estado Hoja Final (o lo que decidas poner)
        JPanel panelDerechoContenido = new JPanel(new BorderLayout());
        panelDerechoContenido.add(scrollHojaFinal, BorderLayout.CENTER);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelIzquierdoContenido,
                panelDerechoContenido);
        splitPane.setResizeWeight(0.60); // Dar más espacio inicial al lado izquierdo
        // La posición del divisor se ajusta mejor después de que la ventana es visible
        // o tiene un tamaño definido (usaremos pack() o setSize).

        add(splitPane, BorderLayout.CENTER);

        // --- Botón de inicio ---
        btnIniciarProceso = new JButton("Iniciar Proceso Toy Scannamar");
        btnIniciarProceso.setFont(new Font("Arial", Font.BOLD, 14));
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBoton.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        panelBoton.add(btnIniciarProceso);
        add(panelBoton, BorderLayout.SOUTH);

        // Instanciar el Adapter
        scannamarAdapter = new toy_adapter_scannamar_2(this);

        btnIniciarProceso.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Limpiar áreas de texto antes de una nueva ejecución (opcional)
                animalInfoTextArea.setText("");
                estadoHojaFinalTextArea.setText("");
                // La limpieza de mensajesTextArea se hace en el Adapter antes de llamar a la lógica Scala
                scannamarAdapter.ejecutarScannamar();
            }
        });

        pack(); // Ajusta el tamaño de la ventana a los componentes preferidos
        setMinimumSize(new Dimension(700, 500)); // Un tamaño mínimo razonable
        setLocationRelativeTo(null); // Centrar ventana
        setVisible(true);

        // Ajustar el divisor después de que la ventana sea visible y tenga tamaño
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.60));
    }

    public void appendToMensajes(String texto) {
        mensajesTextArea.append(texto + "\n");
        mensajesTextArea.setCaretPosition(mensajesTextArea.getDocument().getLength());
    }

    public void clearMensajes() {
        mensajesTextArea.setText("");
    }

    public String showInputDialog(String prompt) {
        // Se puede personalizar el título del diálogo
        return JOptionPane.showInputDialog(this, prompt, "Entrada Requerida", JOptionPane.PLAIN_MESSAGE);
    }

    // Métodos para actualizar otros paneles (si los usas desde Scala)
    public void mostrarInfoAnimal(String info) {
        SwingUtilities.invokeLater(() -> animalInfoTextArea.setText(info));
    }

    public void mostrarEstadoHojaFinal(String info) {
        SwingUtilities.invokeLater(() -> estadoHojaFinalTextArea.setText(info));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Aplicar un Look and Feel más moderno si está disponible (Nimbus)
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Si Nimbus no está disponible, usa el L&F por defecto
                }
                new toy_gui_scannamar_2();
            }
        });
    }
}