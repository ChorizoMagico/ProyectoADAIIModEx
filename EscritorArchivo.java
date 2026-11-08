import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import modelo.Resultado;

/**
 * Escribe la salida de un algoritmo en el formato del enunciado (n + 2 líneas):
 *
 *   Ext        extremismo de la red ya moderada
 *   Esf        esfuerzo de la estrategia
 *   mod0       1 si el agente 0 fue moderado, 0 si no
 *   ...
 *   mod(n-1)
 */
public class EscritorArchivo {

    /** Construye el texto de la salida (el mismo que se guarda en el archivo). */
    public static String formatear(Resultado r) {
        StringBuilder sb = new StringBuilder();
        // Locale.US: punto decimal siempre, sin importar la configuración del computador
        sb.append(String.format(Locale.US, "%.4f", r.extremismo())).append('\n');
        sb.append(r.esfuerzo()).append('\n');
        for (int moderado : r.estrategia()) {
            sb.append(moderado).append('\n');
        }
        return sb.toString();
    }

    public static void escribir(String ruta, Resultado r) throws IOException {
        Files.writeString(Path.of(ruta), formatear(r), StandardCharsets.UTF_8);
    }
}
