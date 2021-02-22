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
    
    private volatile List<ObjectToDraw> m_listMoving = new ArrayList<>();
    private volatile List<ObjectToDraw> m_listFixed = new ArrayList<>();
    private volatile String m_timePassed = "0";
    
    private static Object m_movingLock = new Object();
    private static Object m_fixedLock = new Object();
    private static Object m_timeLock = new Object();
    
    public DrawingBuffer (BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
    }
    
    public ArrayList<ObjectToDraw> downloadMoving()
    {
        synchronized(m_movingLock)
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
        synchronized(m_fixedLock)
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
    
    public String getTimePassed()
    {
        synchronized(m_timeLock)
        {
            return m_timePassed;
        }
    }
    
    public void logMoving(List<ObjectToDraw> p_listToDraw)
    {
        synchronized(m_movingLock)
        {
            m_listMoving = new ArrayList<>();
            
            for (ObjectToDraw object: p_listToDraw)
            {
                object.rescale(m_scaleX, m_scaleY, 1);
                m_listMoving.add(object);
            }
        }
        
    }
    
    //WARNING: ONLY THE LAST QDS TO HAVE RECOMBINED SHOWN AT THE MOMENT
    public void logFixed (List<ObjectToDraw> p_listToDraw)
    {
        synchronized(m_fixedLock)
        {
            m_listFixed = new ArrayList<>();
            
            for (ObjectToDraw object: p_listToDraw)
            {
                object.rescale(m_scaleX, m_scaleY, m_scaleX.doubleValue());
                m_listFixed.add(object);
            }
        }
    }
    
    public void logTime(BigDecimal p_time)
    {
        synchronized(m_timeLock)
        {
            m_timePassed = (p_time.divide(PhysicsTools.UnitsPrefix.FEMTO.getMultiplier())).stripTrailingZeros().toPlainString();
        }
    }
}
