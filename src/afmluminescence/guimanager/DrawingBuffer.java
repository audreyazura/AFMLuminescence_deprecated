/*
 * Copyright (C) 2020-2021 Alban Lafuente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package afmluminescence.guimanager;

import com.github.audreyazura.commonutils.PhysicsTools;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alban Lafuente
 */
public class DrawingBuffer
{
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    
    private volatile boolean m_reinitialisationRequested = false;
    private volatile List<ObjectToDraw> m_listMoving = new ArrayList<>();
    private volatile List<ObjectToDraw> m_listFixed = new ArrayList<>();
    private volatile String m_timePassed = "0";
    
    private static Object m_fixedObjectsLock = new Object();
    private static Object m_movingObjectsLock = new Object();
    private static Object m_reinitialisationRequestLock = new Object();
    private static Object m_timeLock = new Object();
    
    public DrawingBuffer (BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
    }
    
    public ArrayList<ObjectToDraw> downloadMoving()
    {
        synchronized(m_movingObjectsLock)
        {
            if (m_listMoving.size() > 0)
            {
                return new ArrayList<>(m_listMoving);
            }
            else
            {
                return new ArrayList<>();
            }
        }
    }
    
    public ArrayList<ObjectToDraw> downloadFixed()
    {
        synchronized(m_fixedObjectsLock)
        {
            if (m_listFixed.size() > 0)
            {
                return new ArrayList<>(m_listFixed);
            }
            else
            {
                return new ArrayList<>();
            }
        }
    }
    
    public boolean hasToReinitialize()
    {
        synchronized(m_reinitialisationRequestLock)
        {
            boolean answer = m_reinitialisationRequested;
            m_reinitialisationRequested = false;
            return answer;
        }
    }
    
    public String getTimePassed()
    {
        synchronized(m_timeLock)
        {
            return m_timePassed;
        }
    }
    
    public void logMoving(List<ObjectToDraw> p_listToDraw)
    {
        synchronized(m_movingObjectsLock)
        {
            m_listMoving = new ArrayList<>(p_listToDraw);
        }
        
    }
    
    //WARNING: ONLY THE LAST QDS TO HAVE RECOMBINED SHOWN AT THE MOMENT
    public void logFixed (List<ObjectToDraw> p_listToDraw)
    {
        synchronized(m_fixedObjectsLock)
        {
            m_listFixed = new ArrayList<>(p_listToDraw);
        }
    }
    
    public void logTime(BigDecimal p_time)
    {
        synchronized(m_timeLock)
        {
            m_timePassed = (p_time.divide(PhysicsTools.UnitsPrefix.PICO.getMultiplier())).stripTrailingZeros().toPlainString();
        }
    }
    
    public void requestReinitialisation()
    {
        synchronized(m_reinitialisationRequestLock)
        {
            m_reinitialisationRequested = true;
        }
    }
}
