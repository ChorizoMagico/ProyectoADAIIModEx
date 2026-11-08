package modelo;

public class Algoritmos {

    /** Máximo n para fuerza bruta: 2^25 * 25 ≈ 8*10^8 operaciones. */
    public static final int MAX_N_FUERZA_BRUTA = 25;

    /**
     * Fuerza bruta: genera las 2^n estrategias posibles y se queda con la
     * aplicable (esfuerzo <= R_max) de menor extremismo.
     *
     * Cada estrategia se representa con un número "mascara" entre 0 y 2^n - 1:
     * el bit i de la máscara es e[i] (1 = se modera al agente i).
     *
     * Complejidad: O(n * 2^n) en tiempo, O(n) en espacio.
     */
    public static Resultado modexFB(RedSocial red) {
        int n = red.cantidadAgentes();

        if (n > MAX_N_FUERZA_BRUTA) {
            throw new IllegalArgumentException(
                    "Fuerza bruta no viable para n = " + n
                            + " (máximo " + MAX_N_FUERZA_BRUTA + "): habría que revisar 2^" + n + " estrategias.");
        }
        if (n == 0) {
            return new Resultado(new int[0], 0, 0.0);
        }

        // 1. Precalcular costo y aporte al extremismo de cada agente
        int[] costo = new int[n];
        double[] peso = new double[n];
        double sumaTotal = 0;
        for (int i = 0; i < n; i++) {
            costo[i] = red.getAgente(i).esfuerzo();
            peso[i] = red.getAgente(i).pesoExtremismo();
            sumaTotal += peso[i];
        }

        int rMax = red.getEsfuerzoMaximo();
        long totalEstrategias = 1L << n;   // 2^n (long para no desbordar)

        // 2. Mejor inicial: no moderar a nadie (máscara 0). Siempre es aplicable (esfuerzo 0).
        long mejorMascara = 0;
        double mejorSuma = sumaTotal;

        // 3. Recorrer todas las demás estrategias
        for (long mascara = 1; mascara < totalEstrategias; mascara++) {
            int esfuerzo = 0;
            double suma = 0;          // suma de o^2 * inf de los NO moderados
            boolean aplicable = true;

            for (int i = 0; i < n; i++) {
                if (((mascara >> i) & 1) == 1) {      // agente i moderado
                    esfuerzo += costo[i];
                    if (esfuerzo > rMax) {            // ya se pasó del presupuesto
                        aplicable = false;
                        break;
                    }
                } else {                              // agente i no moderado
                    suma += peso[i];
                }
            }

            // 4. Comparar sumas (equivale a comparar Ext, porque Ext = sqrt(suma)/n)
            if (aplicable && suma < mejorSuma) {
                mejorSuma = suma;
                mejorMascara = mascara;
            }
        }

        // 5. Convertir la mejor máscara en la estrategia E
        int[] e = new int[n];
        for (int i = 0; i < n; i++) {
            e[i] = (int) ((mejorMascara >> i) & 1);
        }

        return new Resultado(e, red.esfuerzo(e), red.extremismo(e));
    }
    //public static Resultado modexV(RedSocial red)  { ... }
    //public static Resultado modexPD(RedSocial red) { ... }
}
