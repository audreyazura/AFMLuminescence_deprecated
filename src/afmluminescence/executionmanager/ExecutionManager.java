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
import afmluminescence.guimanager.GUIManager;
import afmluminescence.guimanager.ObjectToDraw;
import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.GeneratorManager;
import afmluminescence.luminescencegenerator.ImageBuffer;
import afmluminescence.luminescencegenerator.QuantumDot;
import afmluminescence.luminescencegenerator.ResultHandler;
import com.sun.jdi.AbsentInformationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 *
 * @author audreyazura
 */
public class ExecutionManager implements ImageBuffer, ResultHandler
{
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    private final DrawingBuffer m_buffer;
    private final GUIManager m_gui;
    private final File m_luminescence;
    
    public ExecutionManager (GUIManager p_gui, DrawingBuffer p_buffer, List<String> p_filesPaths, BigDecimal p_sampleXSize, BigDecimal p_sampleYSize, BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
        
        m_luminescence = new File(p_filesPaths.get(0));
        
        m_gui = p_gui;
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
                (new Thread(new GeneratorManager(this, this, 10000, new File(qdsPath), new BigDecimal("300"), p_sampleXSize, p_sampleYSize))).start();
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
        List<BigDecimal> recombinationTimes = new ArrayList(p_result.values());
        List<BigDecimal> recombinationEnergies = new ArrayList<>();
        
        for (Electron el: p_result.keySet())
        {
            try
            {
                recombinationEnergies.add(el.getRecombinationEnergy());
            }
            catch (AbsentInformationException ex)
            {
                Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        SimulationSorter writer = new SimulationSorter(new ArrayList(recombinationTimes), new ArrayList(recombinationEnergies));
        try
        {
            writer.saveToFile(new File("Results/TimeResolved.dat"), new File("Results/Spectra.dat"));
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try
        {
            Runtime commandPrompt = Runtime.getRuntime();
            commandPrompt.exec("gnuplot");
            showResults(commandPrompt);
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.WARNING, "Gnuplot is missing.", ex);
        }
    }
    
    private void showResults (Runtime p_commandPrompt)
    {
        try
        {
            String spectraFile = "Results/Spectra.png";
            String timeResolvedFile = "Results/TimeResolved.png";
            
            BufferedWriter gnuplotWriter = new BufferedWriter(new FileWriter("Results/.gnuplotScript.gp"));
            gnuplotWriter.write("set terminal png");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set ylabel \"Intensity (arb. units.)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Wavelength (nm)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + spectraFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot \"Results/Spectra.dat\" u 1:2 w line t \"Luminescence\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + timeResolvedFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Time (ns)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot \"Results/TimeResolved.dat\" u ($1/1000):2 w points t \"Time Resolved Luminescence\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.flush();
            gnuplotWriter.close();

            p_commandPrompt.exec("gnuplot Results/.gnuplotScript.gp").waitFor();
            p_commandPrompt.exec("rm Results/.gnuplotScript.gp");

            Platform.runLater(() ->
            {
                try
                {
                    m_gui.showPicture(new Image(new FileInputStream(spectraFile)), "Spectra", "left");
                    m_gui.showPicture(new Image(new FileInputStream(timeResolvedFile)), "Time Resolved", "right");
                } catch (FileNotFoundException ex)
                {
                    Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } 
        catch (IOException|InterruptedException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
