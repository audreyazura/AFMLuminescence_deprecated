/*
 * Copyright (C) 2021 audreyazura
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
package afmluminescence.executionmanager;

import afmluminescence.guimanager.DrawingBuffer;
import afmluminescence.guimanager.ObjectToDraw;
import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.ImageBuffer;
import afmluminescence.luminescencegenerator.QuantumDot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

/**
 *
 * @author audreyazura
 */
public class ImageInterpretator implements ImageBuffer
{
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    private final DrawingBuffer m_buffer;
    
    public ImageInterpretator (BigDecimal p_scaleX, BigDecimal p_scaleY, DrawingBuffer p_buffer)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
        m_buffer = p_buffer;
    }
    
    @Override
    public void logElectrons(List<Electron> p_listToDraw)
    {
        ArrayList<ObjectToDraw> objectList = new ArrayList();
            
        for (Electron currentElectron: p_listToDraw)
        {
            if (currentElectron.isFree())
            {
                BigDecimal radius = new BigDecimal("2");
                
                objectList.add(new ObjectToDraw(currentElectron.getX().multiply(m_scaleX).subtract(radius), currentElectron.getY().multiply(m_scaleY).subtract(radius), Color.BLACK, radius.doubleValue()));
            }
        }
        
        m_buffer.logMoving(objectList);
    }
    
    @Override
    public void logQDs(List<QuantumDot> p_listToDraw)
    {
        ArrayList<ObjectToDraw> objectList = new ArrayList();
            
        for (QuantumDot currentQD: p_listToDraw)
        {
            BigDecimal radius = currentQD.getRadius().multiply(m_scaleX);

            Color toPaint;
            if (currentQD.hasRecombined())
            {
                toPaint = Color.RED;
            }
            else
            {
                toPaint = Color.GREEN;
            }

            objectList.add(new ObjectToDraw(currentQD.getX().multiply(m_scaleX).subtract(radius), currentQD.getY().multiply(m_scaleY).subtract(radius), toPaint, radius.doubleValue()));
        }
        
        m_buffer.logFixed(objectList);
    }
    
    @Override
    public void logTime(BigDecimal p_time)
    {
        m_buffer.logTime(p_time);
    }
}
