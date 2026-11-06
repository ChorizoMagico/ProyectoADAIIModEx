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


    //Revisar:
    /** Esfuerzo de llevar su opinión a 0: ceil(|o| * (1 - r) * inf). */
    public int esfuerzo() {
        // el -1e-9 evita que errores de punto flotante (9.000000000000002) suban el ceil
        return (int) Math.ceil(Math.abs(opinion) * (1 - receptividad) * influencia - 1e-9);
    }

    /** Aporte al extremismo si NO se modera: o^2 * inf. */
    public double pesoExtremismo() {
        return (double) opinion * opinion * influencia;
    }

}
