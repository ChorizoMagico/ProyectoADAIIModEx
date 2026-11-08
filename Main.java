import java.util.Arrays;
import modelo.*;

public class Main {
    public static void main(String[] args) {
        String[] pruebas  = {"Prueba1", "Prueba2", "Prueba3", "Prueba4", "Prueba5", "Prueba6"};
        double[] esperado = {14.069,    25.959,    16.779,    13.0,      13.48,     13.568};

        System.out.println("=== Fuerza bruta (modexFB) ===");
        for (int k = 0; k < pruebas.length; k++) {
            String ruta = "Bateria de pruebas/" + pruebas[k] + ".txt";
            try {
                RedSocial red = LectorArchivo.leer(ruta);

                //Medicion del tiempo que demora en encontrar la estrategia optima
                long inicio = System.nanoTime();
                Resultado r = Algoritmos.modexFB(red);
                double ms = (System.nanoTime() - inicio) / 1e6;

                // La tabla trae 2-3 decimales, así que se compara con tolerancia
                boolean ok = Math.abs(r.extremismo() - esperado[k]) < 0.01;

                System.out.printf("%s  n=%d  Ext=%.4f  (esperado %.3f) %s  Esf=%d/%d  %.1f ms%n",
                        pruebas[k], red.cantidadAgentes(), r.extremismo(), esperado[k],
                        ok ? "OK" : "DIFERENTE", r.esfuerzo(), red.getEsfuerzoMaximo(), ms);
                System.out.println("   E = " + Arrays.toString(r.estrategia()));

            } catch (Exception e) {
                System.out.println(pruebas[k] + "  ERROR: " + e.getMessage());
            }
        }
}}