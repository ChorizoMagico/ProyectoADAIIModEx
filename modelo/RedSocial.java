package modelo;
import java.util.ArrayList;
import java.util.List;


public class RedSocial{
    private List<Agente> agentes;
    private int esfuerzoMaximo;


    public RedSocial(List<Agente> agentes, int esfuerzoMaximo){
        this.agentes = agentes;
        this.esfuerzoMaximo = esfuerzoMaximo;
    }

    public int cantidadAgentes() { return agentes.size(); }
    public int getEsfuerzoMaximo() { return esfuerzoMaximo; }
    public Agente getAgente(int indice){ return agentes.get(indice); }

    //Revisar
    /** Esfuerzo total de la estrategia e (e[i] = 1 => se modera al agente i). */
    public int esfuerzo(int[] e) {
        int total = 0;
        for (int i = 0; i < cantidadAgentes(); i++)
            if (e[i] == 1) total += agentes.get(i).esfuerzo();
        return total;
    }

    /** Extremismo de la red después de aplicar e. */
    public double extremismo(int[] e) {
        double suma = 0;
        for (int i = 0; i < cantidadAgentes(); i++)
            if (e[i] == 0) suma += agentes.get(i).pesoExtremismo();
        return Math.sqrt(suma) / cantidadAgentes();
    }

    public boolean esAplicable(int[] e) {
        return esfuerzo(e) <= esfuerzoMaximo;
    }




}