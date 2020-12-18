/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import afmluminescence.luminescencegenerator.QuantumDot;
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
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    
    private volatile List<ObjectToDraw> m_listElectron = new ArrayList<>();
    private volatile List<ObjectToDraw> m_listQDs = new ArrayList<>();
    private volatile String m_timePassed = "0";
    
    private static Object m_electronLock = new Object();
    private static Object m_QDLOck = new Object();
    private static Object m_timeLock = new Object();
    
    public DrawingBuffer (BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
    }
    
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
    
    public ArrayList<ObjectToDraw> downloadQDs()
    {
        synchronized(m_QDLOck)
        {
            if (m_listQDs.size() > 0)
            {
                return new ArrayList<>(m_listQDs);
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
                BigDecimal radius = new BigDecimal("2");
                m_listElectron.add(new ObjectToDraw((currentElectron.getX().multiply(m_scaleX)).subtract(radius), (currentElectron.getY().multiply(m_scaleY)).subtract(radius), radius.doubleValue()));
            }
        }
        
    }
    
    @Override
    public void logQDs (List<QuantumDot> p_listToDraw)
    {
        synchronized(m_QDLOck)
        {
            m_listQDs = new ArrayList<>();
            
            for (QuantumDot currentQD: p_listToDraw)
            {
                BigDecimal radius = currentQD.getRadius();
                m_listQDs.add(new ObjectToDraw(currentQD.getX().multiply(m_scaleX).subtract(radius), currentQD.getY().multiply(m_scaleY).subtract(radius), (radius.multiply(m_scaleX)).doubleValue()));
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
