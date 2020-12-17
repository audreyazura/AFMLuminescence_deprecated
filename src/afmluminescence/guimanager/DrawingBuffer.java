/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class DrawingBuffer implements ImageBuffer
{
    private List<Electron> m_listToDraw;
    private final static Object m_lock = new Object();
    
    public void download()
    {
        long startingTime = System.nanoTime();
        
        synchronized(m_lock)
        {
            long passedTime = (System.nanoTime() - startingTime) / 1000000000;
            System.out.println(Thread.currentThread().getName() + " accessed the download after " + passedTime + "s.");
        }
    }
    
    @Override
    public void log()
    {
        synchronized(m_lock)
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException ex)
            {
                System.out.println("Still sleeping.");
            }
        }
    }
}
