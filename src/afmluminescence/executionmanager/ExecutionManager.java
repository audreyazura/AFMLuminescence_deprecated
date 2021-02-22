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
import afmluminescence.luminescencegenerator.GeneratorManager;
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
public class ExecutionManager implements ImageBuffer
{
    private final DrawingBuffer m_buffer;
    private final GeneratorManager m_generator;
    
    public ExecutionManager (DrawingBuffer p_buffer, BigDecimal p_sampleXSize, BigDecimal p_sampleYSize)
    {
        m_buffer = p_buffer;
        m_generator = new GeneratorManager(this, 1000, 350, new BigDecimal("300"), p_sampleXSize, p_sampleYSize);
         
        (new Thread(m_generator)).start();
    }
    
    @Override
    public void logElectrons(List<Electron> p_listToDraw)
    {
        ArrayList<ObjectToDraw> objectList = new ArrayList();
            
        for (Electron currentElectron: p_listToDraw)
        {
            if (currentElectron.isFree())
            {
                objectList.add(new ObjectToDraw(currentElectron.getX(), currentElectron.getY(), Color.BLACK, 2));
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
            double radius = currentQD.getRadius().doubleValue();

            Color toPaint;
            if (currentQD.hasRecombined())
            {
                toPaint = Color.RED;
            }
            else
            {
                toPaint = Color.GREEN;
            }

            objectList.add(new ObjectToDraw(currentQD.getX(), currentQD.getY(), toPaint, radius));
        }
        
        m_buffer.logFixed(objectList);
    }
    
    @Override
    public void logTime(BigDecimal p_time)
    {
        m_buffer.logTime(p_time);
    }
}
