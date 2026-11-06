import java.util.ArrayList;
import java.util.List;

import modelo.Agente;
import modelo.RedSocial;

public class Main {
    public static void main(String[] args) {
    
    
    
    //Leemos la red social del archivo
    try {
        RedSocial redSocial = LectorArchivo.leer("Bateria de pruebas/Prueba1.txt");
    } catch (Exception e) {
        //Excepción
    }
    
    
}}