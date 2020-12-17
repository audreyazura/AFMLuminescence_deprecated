/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class DrawingBuffer implements ImageBuffer
{
    private volatile List<Electron> m_listToDraw = new ArrayList<>();
    
    synchronized public ArrayList<Electron> download()
    {
//        System.out.println(Thread.currentThread().getName() + " accessed the download.");
        if (m_listToDraw.size() > 0)
        {
            return new ArrayList<>(m_listToDraw);
        }
        else
        {
            return new ArrayList<>();
        }
    }
    
    @Override
    synchronized public void log(List<Electron> p_lisToDraw)
    {
        m_listToDraw = new ArrayList(p_lisToDraw);
//        try
//        {
//            Thread.sleep(2000);
//        }
//        catch (InterruptedException ex)
//        {
//            System.out.println("Still sleeping.");
//        }
    }
}
