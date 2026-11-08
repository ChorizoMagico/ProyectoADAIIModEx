import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import modelo.Agente;
import modelo.RedSocial;

/**
 * Genera casos de prueba aleatorios y los escribe en el formato de entrada del enunciado.
 *
 * Rangos:
 *   opinión        entero en [-100, 100]
 *   receptividad   con 2 decimales en [0, 1]
 *   influencia     con 2 decimales en [1, 3]
 *   R_max          fracción de la suma de los esfuerzos de TODOS los agentes
 *                  (0.0 = no se puede moderar a nadie, 1.0 = se puede moderar a todos)
 *
 * La semilla permite repetir exactamente el mismo caso (útil para documentarlo en el informe).
 *
 * Uso por consola:  java GeneradorCasos <n> <fraccion 0..1> <semilla> <archivo_salida>
 */
public class GeneradorCasos {

    public static RedSocial generar(int n, double fraccionPresupuesto, long semilla) {
        if (n < 1) {
            throw new IllegalArgumentException("n debe ser al menos 1");
        }
        if (fraccionPresupuesto < 0 || fraccionPresupuesto > 1) {
            throw new IllegalArgumentException("La fracción del presupuesto debe estar entre 0 y 1");
        }

        Random azar = new Random(semilla);
        List<Agente> agentes = new ArrayList<>(n);
        long sumaEsfuerzos = 0;

        for (int i = 0; i < n; i++) {
            int opinion = azar.nextInt(201) - 100;               // -100 .. 100
            double receptividad = azar.nextInt(101) / 100.0;     // 0.00 .. 1.00
            double influencia = (100 + azar.nextInt(201)) / 100.0; // 1.00 .. 3.00
            // Se generan como enteros / 100.0 para que el valor sea exactamente el que se
            // lee después desde el archivo (por ejemplo, 1 + 0.72 no siempre da 1.72).

            Agente agente = new Agente(opinion, receptividad, influencia);
            agentes.add(agente);
            sumaEsfuerzos += agente.esfuerzo();
        }

        int rMax = (int) Math.round(fraccionPresupuesto * sumaEsfuerzos);
        return new RedSocial(agentes, rMax);
    }

    /** Escribe la red en el formato de entrada del enunciado. */
    public static void escribirEntrada(String ruta, RedSocial red) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(red.cantidadAgentes()).append('\n');
        for (int i = 0; i < red.cantidadAgentes(); i++) {
            Agente a = red.getAgente(i);
            sb.append(String.format(Locale.US, "%d,%.2f,%.2f",
                    a.getOpinion(), a.getReceptividad(), a.getInfluencia())).append('\n');
        }
        sb.append(red.getEsfuerzoMaximo()).append('\n');
        Files.writeString(Path.of(ruta), sb.toString(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Uso: java GeneradorCasos <n> <fraccion 0..1> <semilla> <archivo_salida>");
            return;
        }
        int n = Integer.parseInt(args[0]);
        double fraccion = Double.parseDouble(args[1]);
        long semilla = Long.parseLong(args[2]);
        RedSocial red = generar(n, fraccion, semilla);
        escribirEntrada(args[3], red);
        System.out.println("Caso escrito en " + args[3] + " (n = " + n + ", R_max = " + red.getEsfuerzoMaximo() + ")");
    }
}
