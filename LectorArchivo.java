import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import modelo.Agente;
import modelo.RedSocial;

//Definición de la clase
public class LectorArchivo {

    //Método static (global) leer, recibe ruta y devuelve RedSocial
    public static RedSocial leer(String ruta) throws IOException {
    //Lee todas las líneas del archivo de prueba
    List<String> lineas = Files.readAllLines(Path.of(ruta));
    //Cantidad de agentes
    int n = Integer.parseInt(lineas.get(0).trim());
    List<Agente> agentes = new ArrayList<>(n);

    //Por cada agente
    for (int i = 1; i <= n; i++) {
        //Añade los strings separados por comas a un arreglo
        String[] p = lineas.get(i).split(",");

        //Añade el agente
        agentes.add(new Agente(
            Integer.parseInt(p[0].trim()),
            Double.parseDouble(p[1].trim()),
            Double.parseDouble(p[2].trim())));
    }

    //Esfuerzo máximo de la red social
    int esfuerzoMaximo = Integer.parseInt(lineas.get(n + 1).trim());
    return new RedSocial(agentes, esfuerzoMaximo);
}
}
