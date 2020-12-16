/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.GeneratorManager;
import java.math.BigDecimal;

/**
 *
 * @author audreyazura
 */
public class Launcher
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        GeneratorManager luminescenceGenerator = new GeneratorManager();
        luminescenceGenerator.start(1000, new BigDecimal("300"));
    }
    
}
