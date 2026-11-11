package modelo;

public class Agente {
    private final int opinion;
    private final double receptividad;
    private final double influencia;

    public Agente(int opinion, double receptividad, double influencia){
        this.opinion = opinion;
        this.receptividad = receptividad;
        this.influencia = influencia;
    }


    public int getOpinion() { return opinion; }
    public double getReceptividad() { return receptividad; }
    public double getInfluencia() { return influencia; }


    // Esfuerzo de llevar la opinión de un agente individual a 0
    //Es valor absoluto(opinion de agente antes de moderar) y no valor absoluto (opinion  de agente
    //antes de moderar - opinion de agente después de moderar) porque la opinión de un agente después
    //de ser moderado es 0.
    public int esfuerzo() {
        // el -1e-9 evita que errores de punto flotante (9.000000000000002) suban el ceil
        return (int) Math.ceil(Math.abs(opinion) * (1 - receptividad) * influencia - 1e-9);
    }

    /** Aporte al extremismo si no se modera: o^2 * inf. */
    public double pesoExtremismo() {
        return (double) opinion * opinion * influencia;
    }

}
