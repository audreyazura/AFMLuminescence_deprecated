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
import afmluminescence.luminescencegenerator.GeneratorManager;
import static afmluminescence.luminescencegenerator.GeneratorManager.formatBigDecimal;
import afmluminescence.luminescencegenerator.QuantumDot;
import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import javafx.application.Platform;
import javafx.scene.image.Image;

/**
 *
 * @author audreyazura
 */
public class ExecutionManager implements Runnable
{
    private final BigDecimal m_sampleXSize;
    private final BigDecimal m_sampleYSize;
    private final BigDecimal m_scaleX;
    private final BigDecimal m_scaleY;
    private final BigDecimal m_timeStep = new BigDecimal("1e-12");
    private final boolean m_gnuplotInstalled;
    private final ContinuousFunction m_luminescence;
    private final ContinuousFunction m_captureTimes;
    private final ContinuousFunction m_escapeTimes;
    private final DrawingBuffer m_buffer;
    private final File m_luminescenceFile;
    private final GUIManager m_gui;
    private final int m_maxLoop = 5;
    private final int m_nElectron = 100000;
    private final PcgRSFast m_RNGenerator = new PcgRSFast();
    private final ResultHandler m_resultHandler;
    private final Thread m_handlerThread;
    private int m_loopCounter = 0;
    private List<QuantumDot> m_QDList = new ArrayList<>();
    
    public ExecutionManager (GUIManager p_gui, DrawingBuffer p_buffer, List<String> p_filesPaths, BigDecimal p_sampleXSize, BigDecimal p_sampleYSize, BigDecimal p_scaleX, BigDecimal p_scaleY)
    {
        m_sampleXSize = p_sampleXSize;
        m_sampleYSize = p_sampleYSize;
        m_scaleX = p_scaleX;
        m_scaleY = p_scaleY;
        
        m_gui = p_gui;
        m_buffer = p_buffer;
        
        HashMap<BigDecimal, BigDecimal> lumValues = new HashMap<>();
        BigDecimal maxCounts = BigDecimal.ZERO;
        try
        {
            BufferedReader lumReader = new BufferedReader(new FileReader(new File(p_filesPaths.get(0))));
            Pattern numberRegex = Pattern.compile("^\\-?\\d+(\\.\\d+(e(\\+|\\-)\\d+)?)?");
            String line;
            while (((line = lumReader.readLine()) != null))
            {	    
                String[] lineSplit = line.strip().split(";");

                if(numberRegex.matcher(lineSplit[0]).matches())
                {
                    BigDecimal energy = PhysicsTools.h.multiply(PhysicsTools.c).divide((new BigDecimal(lineSplit[0])).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()), MathContext.DECIMAL128);
                    BigDecimal counts = new BigDecimal(lineSplit[1]);
                    
                    lumValues.put(energy, counts);
                    
                    if (counts.compareTo(maxCounts) > 0)
                    {
                        maxCounts = counts;
                    }
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (lumValues.isEmpty())
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("Luminescence file missing or badly formatted."));
        }
        
        //normalisation of the luminescence
        for (BigDecimal abscissa: lumValues.keySet())
        {
            lumValues.put(abscissa, lumValues.get(abscissa).divide(maxCounts, MathContext.DECIMAL128));
        }
        
        m_luminescence = new ContinuousFunction(lumValues);
        
        boolean gnuplotExist;
        try
        {
            //testing if gnuplot is on the computer
            Runtime.getRuntime().exec("gnuplot");
            gnuplotExist = true;
        }
        catch (IOException ex)
        {
            gnuplotExist = false;
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.FINEST, "Gnuplot is missing.", ex);
        }
        m_gnuplotInstalled = gnuplotExist;
        
        m_luminescenceFile = new File("Results/Luminescence.dat");
        if (m_gnuplotInstalled)
        {
            try
            {
                //creating the luminescence file for gnuplot
                Set<BigDecimal> energySet = m_luminescence.getAbscissa();
                if (!m_luminescenceFile.getParentFile().isDirectory())
                {
                    m_luminescenceFile.getParentFile().mkdirs();
                }
                BufferedWriter lumWriter = new BufferedWriter(new FileWriter(m_luminescenceFile));
                lumWriter.write("Wavelength (nm)\tIntensity (cps)");
                for (BigDecimal abscissa: energySet)
                {
                    BigDecimal wavelengthNano = ((PhysicsTools.h.multiply(PhysicsTools.c)).divide(abscissa, MathContext.DECIMAL128)).divide(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);

                    lumWriter.newLine();
                    lumWriter.write(wavelengthNano.toPlainString() + "\t" + m_luminescence.getValueAtPosition(abscissa));
                }
                lumWriter.flush();
                lumWriter.close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, "Impossible to create the luminescence file.", ex);
            }
        }
        
        //generating the QDs to be send and starting the simulation
        String qdsPath = p_filesPaths.get(1);
        ContinuousFunction tempCaptureTimes = new ContinuousFunction();
        ContinuousFunction tempEscapeTimes = new ContinuousFunction();
        try
        {
            //getting the functions giving the capture time, escape time and recombination time as a function of the size of the QD.
            //capture time reference: https://aip.scitation.org/doi/10.1063/1.1512694
            //escape time reference: https://aip.scitation.org/doi/10.1063/1.4824469
            tempCaptureTimes = (new SCSVLoader(new File("/home/audreyazura/Documents/Work/Simulation/AFMLuminescence/CaptureProba/ElectronCaptureTime.scsv"))).getFunction();
            tempEscapeTimes = (new SCSVLoader(new File("/home/audreyazura/Documents/Work/Simulation/AFMLuminescence/EscapeProba/EscapeTime-10^-17cm^-3.scsv"))).getFunction();
            
            //making the QD distribution
            /***********************************************************************************************************
             *                                                                                                         *
             * TO DO: STREAMLINE THE QD DISTRIBUTION BY USING THE LIMITS, SUCH AS WHEN CALCULATING THE RADOM VALUE LATER *
             *                                                                                                         *
             ***********************************************************************************************************/
            if (qdsPath.equals(""))
            {
                //QDs are randomly generated with size following a normal distribution
                int nQDs = 300;
                for (int i = 0 ; i < nQDs ; i += 1)
                {
                    BigDecimal x;
                    BigDecimal y;
                    BigDecimal radius;
                    QuantumDot createdQD;

                    do
                    {
                        x = formatBigDecimal((new BigDecimal(m_RNGenerator.nextDouble())).multiply(p_sampleXSize));
                        y = formatBigDecimal((new BigDecimal(m_RNGenerator.nextDouble())).multiply(p_sampleYSize));

                        do
                        {
                            radius = formatBigDecimal((new BigDecimal(m_RNGenerator.nextGaussian() * 2.1 + 12)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));

                        }while (radius.compareTo(BigDecimal.ZERO) <= 0);

                        createdQD = new QuantumDot(x, y, radius, radius, m_timeStep, tempCaptureTimes, tempEscapeTimes);

                    }while(!validPosition(createdQD, m_QDList));
                    
                    m_QDList.add(createdQD);
                }
            }
            else
            {
               //QDs are extracted from file
                String[] nameSplit = qdsPath.split("\\.");

                if (!nameSplit[nameSplit.length-1].equals("csv"))
                {
                    throw new DataFormatException();
                }

                BufferedReader fileReader = new BufferedReader(new FileReader(new File(qdsPath)));
                Pattern numberRegex = Pattern.compile("^\\-?\\d+(\\.\\d+(e(\\+|\\-)\\d+)?)?");

                String line;
                while (((line = fileReader.readLine()) != null))
                {	    
                    String[] lineSplit = line.strip().split(";");

                    if(numberRegex.matcher(lineSplit[0]).matches())
                    {
                        BigDecimal x = GeneratorManager.formatBigDecimal((new BigDecimal(lineSplit[0].strip())).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                        BigDecimal y = GeneratorManager.formatBigDecimal((new BigDecimal(lineSplit[1].strip())).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                        BigDecimal radius = GeneratorManager.formatBigDecimal(((new BigDecimal(lineSplit[2].strip())).divide(new BigDecimal("2"), MathContext.DECIMAL128)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                        BigDecimal height = GeneratorManager.formatBigDecimal((new BigDecimal(lineSplit[3].strip())).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));

                        QuantumDot currentQD = new QuantumDot(x, y, radius, height, m_timeStep, tempCaptureTimes, tempEscapeTimes);
                        m_QDList.add(currentQD);
                    }
                }
            }
        }
        catch (DataFormatException|IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        m_captureTimes = tempCaptureTimes;
        m_escapeTimes = tempEscapeTimes;
        
        //creating the handler that will check on the simulation
        m_resultHandler = new ResultHandler(this);
        m_handlerThread = new Thread(m_resultHandler);
    }
    
    private void launchCalculation()
    {
        ImageInterpretator GUICommunicator = new ImageInterpretator(m_scaleX, m_scaleY, m_buffer);
        GUICommunicator.logQDs(m_QDList);
        
        GeneratorManager luminescenceGenerator;
        try
        {
            luminescenceGenerator = new GeneratorManager(GUICommunicator, m_nElectron, new ArrayList(m_QDList), new BigDecimal("300"), m_timeStep, m_sampleXSize, m_sampleYSize);
            Thread generatorThread = new Thread(luminescenceGenerator);
            
            System.out.println("Starting simulation " + (m_loopCounter + 1));
            generatorThread.start();
            m_resultHandler.initializeTrackedGenerator(luminescenceGenerator);
        }
        catch (DataFormatException|IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean validPosition(QuantumDot p_testedQD, List<QuantumDot> p_existingQDs)
    {
        boolean valid = true;
        
        for (QuantumDot QD: p_existingQDs)
        {
            valid &= p_testedQD.getRadius().add(QD.getRadius()).compareTo(p_testedQD.getDistance(QD.getX(), QD.getY())) < 0;
        }
        
        return valid;
    }
    
    private void createPictures (int p_fileIndex)
    {
        try
        {
            String spectraFile = "Results/Spectra" + p_fileIndex + ".png";
            String timeResolvedFile = "Results/TimeResolved" + p_fileIndex + ".png";
            
            BufferedWriter gnuplotWriter = new BufferedWriter(new FileWriter("Results/.gnuplotScript.gp"));
            gnuplotWriter.write("set terminal png");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set ylabel \"Intensity (arb. units.)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Wavelength (nm)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + spectraFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot[*:*][0:1.1] \"Results/Spectra" + p_fileIndex + ".dat\" u 1:2 w line t \"Calculated Lum\", \"" + m_luminescenceFile.getCanonicalPath() + "\" u 1:2 w line t \"Experimental Lum\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + timeResolvedFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Time (ns)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot \"Results/TimeResolved" + p_fileIndex + ".dat\" u ($1/1000):2 w points t \"Time Resolved Luminescence\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.flush();
            gnuplotWriter.close();

            Runtime.getRuntime().exec("gnuplot Results/.gnuplotScript.gp").waitFor();
            Runtime.getRuntime().exec("rm Results/.gnuplotScript.gp");
        } 
        catch (IOException|InterruptedException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void computeResults(List<BigDecimal> p_recombinationEnergies, List<BigDecimal> p_recombinationTimes)
    {
        SimulationSorter sorter = new SimulationSorter(new ArrayList(p_recombinationTimes), new ArrayList(p_recombinationEnergies));
        QDFitter fit = new QDFitter(m_QDList, m_timeStep, m_captureTimes, m_escapeTimes, m_luminescence, sorter);
        
        m_loopCounter += 1;
        
        //saving the result to files and making pictures out of them if gnuplot is installed
        try
        {
            sorter.saveToFile(new File("Results/TimeResolved" + m_loopCounter + ".dat"), new File("Results/Spectra" + m_loopCounter + ".dat"));
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (m_gnuplotInstalled)
        {
            createPictures(m_loopCounter);
        }
        
        //testing if the simulation finished or has to continue
        if (fit.isGoodFit() || m_loopCounter >= m_maxLoop)
        {
            m_resultHandler.stopMonitoring();

            System.out.println("x (m)\ty (m)\tradius (m)\theight (m)\tenergy (J)");
            for (QuantumDot qd: m_QDList)
            {
                System.out.println(qd);
            }
            
            if (m_gnuplotInstalled)
            {
                //creating a gif of the result if Image Magick is installed
                try
                {
                    Runtime.getRuntime().exec("convert -delay 100 Results/Spectra*.png Results/Spectra.gif");
                }
                catch (IOException ex)
                {
                    Logger.getLogger(ExecutionManager.class.getName()).log(Level.FINE, "Image Magick not installed.", ex);
                }

                //showing the final result on screen
                Platform.runLater(() ->
                {
                    try
                    {
                        m_gui.showPicture(new Image(new FileInputStream("Results/Spectra" + m_loopCounter + ".png")), "Spectra", "left");
                        m_gui.showPicture(new Image(new FileInputStream("Results/TimeResolved" + m_loopCounter + ".png")), "Time Resolved", "right");
                    }
                    catch (FileNotFoundException ex)
                    {
                        Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        }
        else
        {
            m_QDList = fit.getFittedQDs();
            m_buffer.requestReinitialisation();
            launchCalculation();
        }
    }
    
    @Override
    public void run()
    {
        m_handlerThread.start();
        launchCalculation();
    }
}
