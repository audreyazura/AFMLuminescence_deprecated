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
import afmluminescence.luminescencegenerator.ResultHandler;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javafx.scene.paint.Color;

/**
 *
 * @author audreyazura
 */
public class ExecutionManager implements ImageBuffer, ResultHandler
{
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    private final File m_luminescence;
    private final DrawingBuffer m_buffer;
    
    public ExecutionManager (DrawingBuffer p_buffer, List<String> p_filesPaths, BigDecimal p_sampleXSize, BigDecimal p_sampleYSize, BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
        
        m_luminescence = new File(p_filesPaths.get(0));
        
        m_buffer = p_buffer;
        
        String qdsPath = p_filesPaths.get(1);
        if (qdsPath.equals(""))
        {
            (new Thread(new GeneratorManager(this, this, 1000, 350, new BigDecimal("300"), p_sampleXSize, p_sampleYSize))).start();
        }
        else
        {
            try
            {
                (new Thread(new GeneratorManager(this, this, 1000, new File(qdsPath), new BigDecimal("300"), p_sampleXSize, p_sampleYSize))).start();
            }
            catch (DataFormatException|IOException ex)
            {
                Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
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
    
    @Override
    public void sendResults(HashMap<Electron, BigDecimal> p_result)
    {
        for (Electron el: p_result.keySet())
        {
            System.out.println(el.hashCode() + " => " + p_result.get(el));
        }
    }
}
