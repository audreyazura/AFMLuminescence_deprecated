/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public class DrawingBuffer implements ImageBuffer
{
    private volatile List<ObjectToDraw> m_listToDraw = new ArrayList<>();
    
    synchronized public ArrayList<ObjectToDraw> download()
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
    synchronized public void logElectrons(List<Electron> p_lisToDraw)
    {
        m_listToDraw = new ArrayList();
//        System.out.println(p_lisToDraw.size());
        
        for (Electron currentElectron: p_lisToDraw)
        {
            m_listToDraw.add(new ObjectToDraw(currentElectron.getX(), currentElectron.getY(), new BigDecimal("5"), ObjectToDraw.AbsorberObjectType.Electron));
        }
    }
}
