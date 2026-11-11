import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import modelo.Agente;
import modelo.Algoritmos;
import modelo.RedSocial;
import modelo.Resultado;

/**
 * Interfaz gráfica de ModEx.
 *
 *   1. Cargar un archivo de entrada (o generar un caso aleatorio) y verlo en una tabla.
 *   2. Elegir el algoritmo y resolver.
 *   3. Ver la estrategia, el esfuerzo y el extremismo, con una verificación del cálculo
 *      hecha de forma independiente al algoritmo.
 *   4. Guardar la salida en un archivo con el formato del enunciado.
 */
public class Interfaz extends JFrame {

    /** Algoritmos disponibles. Para agregar uno nuevo basta con añadir una línea aquí. */
    private static final Map<String, Function<RedSocial, Resultado>> ALGORITMOS = new LinkedHashMap<>();
    static {
        ALGORITMOS.put("Fuerza bruta (modexFB)", Algoritmos::modexFB);
        ALGORITMOS.put("Voraz (modexV)", Algoritmos::modexV);
        ALGORITMOS.put("Programación dinámica (modexPD)", Algoritmos::modexPD);
    }

    private RedSocial red;           // red cargada o generada
    private Resultado resultado;     // última solución calculada para esa red

    private final JLabel lblArchivo = new JLabel("Entrada: (ninguna)");
    private final JLabel lblRed = new JLabel(" ");
    private final JLabel lblEstado = new JLabel("Carga un archivo o genera un caso para empezar.");

    private final JButton btnCargar = new JButton("Cargar entrada…");
    private final JButton btnGenerar = new JButton("Generar caso…");
    private final JButton btnResolver = new JButton("Resolver");
    private final JButton btnGuardar = new JButton("Guardar salida…");
    private final JComboBox<String> cmbAlgoritmo =
            new JComboBox<>(ALGORITMOS.keySet().toArray(new String[0]));

    private final DefaultTableModel modeloEntrada = crearModelo(
            "Agente", "Opinión", "Receptividad", "Influencia", "Esfuerzo");
    private final DefaultTableModel modeloSalida = crearModelo(
            "Agente", "Moderado", "Opinión final", "Esfuerzo pagado");
    private final JTextArea txtVerificacion = new JTextArea();

    public Interfaz() {
        super("ModEx - Moderación del extremismo en una red social");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        construirVentana();
        setSize(1150, 680);
        setLocationRelativeTo(null);
        actualizarControles(true);
    }

    //Construcción

    private static DefaultTableModel crearModelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private void construirVentana() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barra.add(btnCargar);
        barra.add(btnGenerar);
        barra.add(Box.createHorizontalStrut(16));
        barra.add(new JLabel("Algoritmo:"));
        barra.add(cmbAlgoritmo);
        barra.add(btnResolver);
        barra.add(btnGuardar);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));
        info.add(lblArchivo);
        info.add(lblRed);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(barra, BorderLayout.NORTH);
        norte.add(info, BorderLayout.CENTER);

        // Panel izquierdo: la entrada
        JScrollPane panelEntrada = new JScrollPane(new JTable(modeloEntrada));
        panelEntrada.setBorder(BorderFactory.createTitledBorder("Entrada: agentes (Esfuerzo = costo de moderar al agente)"));

        // Panel derecho: tabla de salida arriba, verificación abajo
        JScrollPane panelSalida = new JScrollPane(new JTable(modeloSalida));
        panelSalida.setBorder(BorderFactory.createTitledBorder("Salida: estrategia de moderación"));

        txtVerificacion.setEditable(false);
        txtVerificacion.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane panelVerificacion = new JScrollPane(txtVerificacion);
        panelVerificacion.setBorder(BorderFactory.createTitledBorder("Verificación del esfuerzo y del extremismo"));

        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSalida, panelVerificacion);
        derecha.setResizeWeight(0.4);
        derecha.setDividerLocation(230);

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEntrada, derecha);
        centro.setResizeWeight(0.4);
        centro.setDividerLocation(470);

        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        setLayout(new BorderLayout());
        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);

        btnCargar.addActionListener(ev -> cargar());
        btnGenerar.addActionListener(ev -> generarCaso());
        btnResolver.addActionListener(ev -> resolver());
        btnGuardar.addActionListener(ev -> guardar());
    }

    /** Habilita o deshabilita los botones según el estado (y mientras se calcula). */
    private void actualizarControles(boolean libre) {
        btnCargar.setEnabled(libre);
        btnGenerar.setEnabled(libre);
        cmbAlgoritmo.setEnabled(libre);
        btnResolver.setEnabled(libre && red != null);
        btnGuardar.setEnabled(libre && resultado != null);
    }

    // Acciones

    private void cargar() {
        File archivo = elegirArchivo(true, null);
        if (archivo == null) {
            return;
        }
        try {
            RedSocial leida = LectorArchivo.leer(archivo.getPath());
            mostrarRed(leida, "Entrada: " + archivo.getName());
        } catch (Exception ex) {
            error("No se pudo leer el archivo. Revisa que tenga el formato del enunciado.\n\n" + ex);
        }
    }

    private void generarCaso() {
        JSpinner spnN = new JSpinner(new SpinnerNumberModel(10, 1, 100000, 1));
        JSpinner spnPorcentaje = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        JSpinner spnSemilla = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));

        JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Número de agentes (n):"));
        panel.add(spnN);
        panel.add(new JLabel("R_max (% del esfuerzo total):"));
        panel.add(spnPorcentaje);
        panel.add(new JLabel("Semilla:"));
        panel.add(spnSemilla);

        int opcion = JOptionPane.showConfirmDialog(this, panel, "Generar caso de prueba",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        int n = (Integer) spnN.getValue();
        int porcentaje = (Integer) spnPorcentaje.getValue();
        int semilla = (Integer) spnSemilla.getValue();
        try {
            RedSocial generada = GeneradorCasos.generar(n, porcentaje / 100.0, semilla);
            mostrarRed(generada, String.format(Locale.US,
                    "Entrada: caso generado (n = %d, R_max = %d%% del esfuerzo total, semilla = %d)",
                    n, porcentaje, semilla));

            int guardar = JOptionPane.showConfirmDialog(this,
                    "¿Quieres guardar este caso en un archivo de entrada?",
                    "Guardar caso", JOptionPane.YES_NO_OPTION);
            if (guardar == JOptionPane.YES_OPTION) {
                File destino = elegirArchivo(false, "caso_n" + n + "_s" + semilla + ".txt");
                if (destino != null) {
                    GeneradorCasos.escribirEntrada(destino.getPath(), generada);
                    estado("Caso guardado en " + destino.getName());
                }
            }
        } catch (Exception ex) {
            error("No se pudo generar el caso.\n\n" + ex);
        }
    }

    private void resolver() {
        String nombre = (String) cmbAlgoritmo.getSelectedItem();
        Function<RedSocial, Resultado> algoritmo = ALGORITMOS.get(nombre);
        RedSocial redActual = red;

        estado("Calculando con " + nombre + "…");
        actualizarControles(false);

        // En segundo plano para que la ventana no se congele con n grande
        new SwingWorker<Resultado, Void>() {
            private double milisegundos;

            @Override
            protected Resultado doInBackground() {
                long inicio = System.nanoTime();
                Resultado r = algoritmo.apply(redActual);
                milisegundos = (System.nanoTime() - inicio) / 1e6;
                return r;
            }

            @Override
            protected void done() {
                try {
                    resultado = get();
                    mostrarResultado(redActual, resultado, nombre, milisegundos);
                    estado(String.format(Locale.US, "Listo: %s en %.3f ms.", nombre, milisegundos));
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    resultado = null;
                    limpiarSalida();
                    estado("El algoritmo no pudo resolver este caso.");
                    error(causa.getMessage() != null ? causa.getMessage() : causa.toString());
                }
                actualizarControles(true);
            }
        }.execute();
    }

    private void guardar() {
        File destino = elegirArchivo(false, "salida.txt");
        if (destino == null) {
            return;
        }
        try {
            EscritorArchivo.escribir(destino.getPath(), resultado);
            estado("Salida guardada en " + destino.getName());
        } catch (Exception ex) {
            error("No se pudo guardar la salida.\n\n" + ex);
        }
    }

    // Mostrar datos

    private void mostrarRed(RedSocial nueva, String descripcion) {
        red = nueva;
        resultado = null;

        modeloEntrada.setRowCount(0);
        for (int i = 0; i < red.cantidadAgentes(); i++) {
            Agente a = red.getAgente(i);
            modeloEntrada.addRow(new Object[] {
                i, a.getOpinion(),
                String.format(Locale.US, "%.2f", a.getReceptividad()),
                String.format(Locale.US, "%.2f", a.getInfluencia()),
                a.esfuerzo()
            });
        }
        limpiarSalida();

        lblArchivo.setText(descripcion);
        lblRed.setText(String.format(Locale.US, "n = %d agentes   |   R_max = %d   |   Extremismo sin moderar = %.4f",
                red.cantidadAgentes(), red.getEsfuerzoMaximo(), red.extremismo(new int[red.cantidadAgentes()])));
        estado("Entrada lista. Elige un algoritmo y pulsa Resolver.");
        actualizarControles(true);
    }

    private void limpiarSalida() {
        modeloSalida.setRowCount(0);
        txtVerificacion.setText("");
    }

    /**
     * Muestra la solución y la verifica recalculando el esfuerzo y el extremismo a partir de la
     * estrategia E, sin usar los valores que devolvió el algoritmo.
     */
    private void mostrarResultado(RedSocial r, Resultado sol, String nombre, double ms) {
        int n = r.cantidadAgentes();
        int[] e = sol.estrategia();

        modeloSalida.setRowCount(0);
        StringBuilder sb = new StringBuilder();
        sb.append(f("Algoritmo: %s   |   Tiempo: %.3f ms\n\n", nombre, ms));
        sb.append("Esfuerzo de cada agente moderado: ceil(|o| * (1 - r) * inf)\n");

        int esfuerzoTotal = 0;
        double sumaNoModerados = 0;   // suma de o^2 * inf de los agentes que no se moderan

        for (int i = 0; i < n; i++) {
            Agente a = r.getAgente(i);
            boolean moderado = e[i] == 1;
            int pagado = moderado ? a.esfuerzo() : 0;

            modeloSalida.addRow(new Object[] { i, moderado ? "Sí" : "No", moderado ? 0 : a.getOpinion(), pagado });

            if (moderado) {
                double costoReal = Math.abs(a.getOpinion()) * (1 - a.getReceptividad()) * a.getInfluencia();
                sb.append(f("  Agente %d: ceil(|%d| * (1 - %.2f) * %.2f) = ceil(%.4f) = %d\n",
                        i, a.getOpinion(), a.getReceptividad(), a.getInfluencia(), costoReal, pagado));
                esfuerzoTotal += pagado;
            } else {
                sumaNoModerados += a.pesoExtremismo();
            }
        }

        boolean aplicable = esfuerzoTotal <= r.getEsfuerzoMaximo();
        sb.append(f("\nEsfuerzo total = %d   (R_max = %d)   ->   %s\n", esfuerzoTotal, r.getEsfuerzoMaximo(),
                aplicable ? "la estrategia ES aplicable" : "la estrategia NO es aplicable"));
        sb.append(f("Esfuerzo reportado por el algoritmo = %d   ->   %s\n", sol.esfuerzo(),
                sol.esfuerzo() == esfuerzoTotal ? "coincide" : "NO COINCIDE"));

        double extremismo = n == 0 ? 0 : Math.sqrt(sumaNoModerados) / n;
        sb.append(f("\nS = suma de o^2 * inf de los agentes NO moderados = %.4f\n", sumaNoModerados));
        sb.append(f("Ext = sqrt(S) / n = sqrt(%.4f) / %d = %.4f\n", sumaNoModerados, n, extremismo));
        sb.append(f("Extremismo reportado por el algoritmo = %.4f   ->   %s\n", sol.extremismo(),
                Math.abs(sol.extremismo() - extremismo) < 1e-9 ? "coincide" : "NO COINCIDE"));
        sb.append(f("Extremismo sin moderar a nadie = %.4f\n", r.extremismo(new int[n])));

        txtVerificacion.setText(sb.toString());
        txtVerificacion.setCaretPosition(0);
    }

    // Utilidades

    private static String f(String formato, Object... argumentos) {
        return String.format(Locale.US, formato, argumentos);
    }

    private void estado(String texto) {
        lblEstado.setText(texto);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Abre un selector de archivos. Al guardar, pide confirmación si el archivo ya existe. */
    private File elegirArchivo(boolean abrir, String nombreSugerido) {
        JFileChooser selector = new JFileChooser(new File("."));
        if (nombreSugerido != null) {
            selector.setSelectedFile(new File(selector.getCurrentDirectory(), nombreSugerido));
        }
        int opcion = abrir ? selector.showOpenDialog(this) : selector.showSaveDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File archivo = selector.getSelectedFile();
        if (!abrir && archivo.exists()) {
            int sobrescribir = JOptionPane.showConfirmDialog(this,
                    "El archivo " + archivo.getName() + " ya existe. ¿Sobrescribirlo?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (sobrescribir != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return archivo;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // si falla, se usa el aspecto por defecto de Java
        }
        SwingUtilities.invokeLater(() -> new Interfaz().setVisible(true));
    }
}
