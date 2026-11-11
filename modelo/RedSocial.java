package modelo;
import java.util.ArrayList;
import java.util.List;

//e = Estrategia de moderación (lista de 0 o 1 por cada agente, 0 si no se moderó, 1 si sí se moderó)
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

    // Esfuerzo total de moderar todos los agentes. Si no se moderó, su esfuerzo es 0, entonces no suma nada.
    //Su esfuerzo es 0 porque como su opinión antes de ser moderado es igual a su opinión después de ser moderado,
    //el factor (opinión rs - opinión rs') se vuelve 0 y vuelve a toda la expresión 0.  
    public int esfuerzo(int[] e) {
        int total = 0;
        for (int i = 0; i < cantidadAgentes(); i++)
            if (e[i] == 1) total += agentes.get(i).esfuerzo();
        return total;
    }

    // Extremismo de la red después de aplicar la estrategia de moderación. 
    // Si se moderó, la opinión del agente es 0, y vuelve a toda la expresión 0, entonces no suma nada.
    public double extremismo(int[] e) {
        double suma = 0;
        for (int i = 0; i < cantidadAgentes(); i++)
            if (e[i] == 0) suma += agentes.get(i).pesoExtremismo();
        return Math.sqrt(suma) / cantidadAgentes();
    }

    //Si el esfuerzo total de una estrategia es menor al esfuerzo máximo de la red social, es aplicable.
    public boolean esAplicable(int[] e) {
        return esfuerzo(e) <= esfuerzoMaximo;
    }




}