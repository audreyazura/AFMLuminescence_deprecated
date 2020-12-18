/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public class DrawingBuffer implements ImageBuffer
{
    private volatile List<ObjectToDraw> m_listElectron = new ArrayList<>();
    private volatile List<ObjectToDraw> m_listQDs = new ArrayList<>();
    private volatile String m_timePassed = "0";
    
    private static Object m_electronLock = new Object();
    private static Object m_QDLOck = new Object();
    private static Object m_timeLock = new Object();
    
    public ArrayList<ObjectToDraw> downloadElectron()
    {
        synchronized(m_electronLock)
        {
            if (m_listElectron.size() > 0)
            {
                return new ArrayList<>(m_listElectron);
            }
            else
            {
                return new ArrayList<>();
            }
        }
    }
    
    public String getTimePassed()
    {
        synchronized(m_timeLock)
        {
            return m_timePassed;
        }
    }
    
    @Override
    public void logElectrons(List<Electron> p_lisToDraw)
    {
        synchronized(m_electronLock)
        {
            m_listElectron = new ArrayList();
            
            for (Electron currentElectron: p_lisToDraw)
            {
                m_listElectron.add(new ObjectToDraw(currentElectron.getX(), currentElectron.getY(), 2));
            }
        }
        
    }
    
    @Override
    public void logTime(BigDecimal p_time)
    {
        synchronized(m_timeLock)
        {
            m_timePassed = (p_time.divide(PhysicsTools.UnitsPrefix.FEMTO.getMultiplier())).stripTrailingZeros().toPlainString();
        }
    }
}
