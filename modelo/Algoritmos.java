package modelo;

import java.util.Arrays;

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
    /**
     * Voraz: estrategia de la mochila 0/1 con escogencia por razón beneficio/costo.
     *
     * Moderar al agente i "cuesta" esfuerzo(i) y "ahorra" pesoExtremismo(i) = o^2 * inf
     * (lo que dejaría de sumar en Ext). Minimizar Ext equivale a maximizar el ahorro total
     * sin pasarse de R_max, que es la mochila 0/1 con
     *      peso = esfuerzo(i),  beneficio = pesoExtremismo(i),  capacidad = R_max.
     *
     * Estrategia voraz: moderar primero a los agentes que más extremismo ahorran por cada
     * unidad de esfuerzo, es decir, de mayor razón  pesoExtremismo / esfuerzo.
     *
     *  1. Se calcula la razón de cada agente. Si su esfuerzo es 0 (opinión 0 o receptividad 1),
     *     moderarlo es gratis y la razón se toma como infinita: siempre se modera.
     *  2. Se ordenan los agentes por razón de mayor a menor (desempate: mayor ahorro primero).
     *  3. Se recorre el orden: si el agente cabe en el presupuesto que queda se modera, y si no
     *     cabe se SALTA y se sigue con el siguiente (uno más barato todavía puede caber).
     *
     * A diferencia de la mochila fraccionaria, aquí no se puede partir un agente, por lo que
     * esta estrategia no siempre es óptima (ver el informe). En la batería de pruebas queda
     * a menos de 0.02% del óptimo para n >= 300.
     *
     * Complejidad: O(n log n) en tiempo (el ordenamiento domina; el recorrido es O(n))
     *              y O(n) en espacio.
     */
    public static Resultado modexV(RedSocial red) {
        int n = red.cantidadAgentes();
 
        if (n == 0) {
            return new Resultado(new int[0], 0, 0.0);
        }
 
        // 1. Precalcular costo, ahorro y razón ahorro/costo de cada agente
        int[] costo = new int[n];
        double[] peso = new double[n];
        double[] razon = new double[n];
        for (int i = 0; i < n; i++) {
            costo[i] = red.getAgente(i).esfuerzo();
            peso[i] = red.getAgente(i).pesoExtremismo();
            // costo 0 => moderar es gratis => razón infinita (queda de primero en el orden)
            razon[i] = (costo[i] == 0) ? Double.POSITIVE_INFINITY : peso[i] / costo[i];
        }
 
        // 2. Ordenar los índices de los agentes por razón descendente (desempate: mayor peso)
        Integer[] orden = new Integer[n];
        for (int i = 0; i < n; i++) {
            orden[i] = i;
        }
        Arrays.sort(orden, (a, b) -> {
            int porRazon = Double.compare(razon[b], razon[a]);
            return (porRazon != 0) ? porRazon : Double.compare(peso[b], peso[a]);
        });
 
        // 3. Recorrer en ese orden moderando mientras quepa en el presupuesto
        int[] e = new int[n];
        int presupuesto = red.getEsfuerzoMaximo();   // esfuerzo que todavía se puede gastar
        for (int k = 0; k < n; k++) {
            int i = orden[k];
            if (costo[i] <= presupuesto) {           // cabe: se modera
                e[i] = 1;
                presupuesto -= costo[i];
            }                                        // no cabe: se salta y se sigue con el siguiente
        }
 
        return new Resultado(e, red.esfuerzo(e), red.extremismo(e));
    }
    
    
    /** Máximo de celdas (n * (R_max + 1)) de la tabla de decisiones de la PD: unos 200 MB de memoria. */
    public static final long MAX_CELDAS_PD = 200_000_000L;

    /**
     * Programación dinámica: mochila 0/1 con el mismo planteamiento de modexV.
     *
     * Moderar al agente i "cuesta" esfuerzo(i) y "ahorra" pesoExtremismo(i) = o^2 * inf.
     * Minimizar Ext equivale a maximizar el ahorro total sin pasarse de R_max, que es la
     * mochila 0/1 con  peso = esfuerzo(i),  beneficio = pesoExtremismo(i),  capacidad = R_max.
     *
     * 1. Subestructura óptima: sea g[j][c] el máximo ahorro usando solo los agentes 0..j-1
     *    con un presupuesto de c. Para el agente j-1 hay dos escogencias:
     *      - no moderarlo:  g[j-1][c]
     *      - moderarlo:     g[j-1][c - esfuerzo(j-1)] + pesoExtremismo(j-1)   (si cabe)
     *    y se escoge la mejor:   g[j][c] = max(g[j-1][c], g[j-1][c - costo] + peso)
     *    con g[0][c] = 0. La respuesta al problema original es g[n][R_max].
     *
     * 2. Cálculo (bottom-up): se llena la tabla agente por agente. Como la fila j solo
     *    necesita la fila j-1, basta un arreglo g de tamaño R_max + 1 que se actualiza con c
     *    de mayor a menor (así g[c - costo] todavía es el valor de la fila anterior).
     *
     * 3. Construcción de la solución: en tomar[j][c] se guarda si en el subproblema (j, c)
     *    convino moderar al agente j. Luego se recorre desde (n-1, R_max) hacia atrás: si
     *    tomar[j][c] es true se modera al agente j y el presupuesto baja a c - costo[j].
     *
     * Subproblemas: n * (R_max + 1).
     * Complejidad: O(n * R_max) en tiempo.
     *              O(n * R_max) en espacio (la tabla tomar; el arreglo g solo usa O(R_max)).
     * Es pseudo-polinomial: crece con el VALOR de R_max, no solo con n.
     */
    public static Resultado modexPD(RedSocial red) {
        int n = red.cantidadAgentes();
        int rMax = red.getEsfuerzoMaximo();

        if (n == 0) {
            return new Resultado(new int[0], 0, 0.0);
        }
        if ((long) n * (rMax + 1) > MAX_CELDAS_PD) {
            throw new IllegalArgumentException(
                    "Programación dinámica no viable para n = " + n + " y R_max = " + rMax
                            + ": la tabla tendría " + ((long) n * (rMax + 1)) + " celdas (máximo " + MAX_CELDAS_PD + ").");
        }

        // 1. Precalcular costo y ahorro (aporte al extremismo) de cada agente
        int[] costo = new int[n];
        double[] peso = new double[n];
        for (int i = 0; i < n; i++) {
            costo[i] = red.getAgente(i).esfuerzo();
            peso[i] = red.getAgente(i).pesoExtremismo();
        }

        // 2. Llenar la tabla. g[c] = máximo ahorro con presupuesto c (con 0 agentes vale 0)
        double[] g = new double[rMax + 1];
        boolean[][] tomar = new boolean[n][rMax + 1];
        for (int j = 0; j < n; j++) {
            for (int c = rMax; c >= costo[j]; c--) {          // si costo[j] > c no cabe: g[c] no cambia
                double moderando = g[c - costo[j]] + peso[j];
                if (moderando > g[c]) {                       // moderar al agente j mejora el ahorro
                    g[c] = moderando;
                    tomar[j][c] = true;
                }
            }
        }

        // 3. Recuperar la estrategia recorriendo la tabla desde (n-1, R_max) hacia atrás
        int[] e = new int[n];
        int c = rMax;
        for (int j = n - 1; j >= 0; j--) {
            if (tomar[j][c]) {
                e[j] = 1;
                c -= costo[j];
            }
        }

        return new Resultado(e, red.esfuerzo(e), red.extremismo(e));
    }
}
