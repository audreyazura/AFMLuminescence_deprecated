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

import afmluminescence.luminescencegenerator.GeneratorManager;
import static afmluminescence.luminescencegenerator.GeneratorManager.formatBigDecimal;
import afmluminescence.luminescencegenerator.QuantumDot;
import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.Material;
import com.github.audreyazura.commonutils.Metamaterial;
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
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    private final BigDecimal m_timeStep;
    private final boolean m_gnuplotInstalled;
    private final boolean m_isFittingMode;
    private final ContinuousFunction m_luminescence;
    private final File m_luminescenceFile;
    private final GUIUpdater m_gui;
    private final int m_maxLoop;
    private final int m_nElectron;
    private final Metamaterial m_sampleMaterial;
    private final PcgRSFast m_RNGenerator = new PcgRSFast();
    private final String m_resultDirectory = "Results/";
    private final String m_calculatedSpectraDirectory = m_resultDirectory + "Spectra/";
    private final String m_calculatedTimeResolvedPLDirectory = m_resultDirectory + "TRPL/";
    private final String m_fittedQDListsDirectory = m_resultDirectory + "QDLists/";
    private int m_loopCounter = 0;
    private List<QuantumDot> m_QDList = new ArrayList<>();
    
    private Instant startTime;
    
    public ExecutionManager (GUIUpdater p_gui, Properties p_configuration, BigDecimal p_sampleXSize, BigDecimal p_sampleYSize)
    {
        //testing if the property file has the correct fields
        Set<String> configKeys = p_configuration.stringPropertyNames();
        Pattern timeStepKeyPattern = Pattern.compile("simulation_timestep_.{0,1}s");
        if (!configKeys.contains("execution_mode"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("execution mode property not defined"));
        }
        if (!configKeys.contains("luminescence"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("luminescence property not defined"));
        }
        if (!configKeys.contains("QDs_distribution"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("QD File property not defined"));
        }
        if (!configKeys.contains("material"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("material property not defined"));
        }
        if (!configKeys.contains("number_simulated_electron"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("number of simulated electron not defined"));
        }
        if (!configKeys.contains("maximum_fitting_repetition"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("maximum number of fitting loop not defined"));
        }
        boolean containsTimestepKey = false;
        String timeStepKey = "";
        for (String key: configKeys)
        {
            if (containsTimestepKey = timeStepKeyPattern.matcher(key).matches())
            {
                timeStepKey = key;
                break;
            }
        }
        
        if (!containsTimestepKey)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("timestep not defined"));
        }
        
        m_sampleXSize = p_sampleXSize;
        m_sampleYSize = p_sampleYSize;
        
        m_gui = p_gui;
        
        //select an execution mode
        String executionMode = p_configuration.getProperty("execution_mode");
        m_isFittingMode = executionMode.equals("fitting");
        if (!m_isFittingMode && !executionMode.equals("simulation"))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new IOException("Please select an execution mode between \"fitting\" and \"simulation\""));
        }

        //initializing timestep
        BigDecimal timeStepUnitMultiplier = PhysicsTools.UnitsPrefix.selectPrefix(timeStepKey.split("_")[2]).getMultiplier();
        BigDecimal tempTimestep = BigDecimal.ZERO;
        try
        {
            tempTimestep = (new BigDecimal(p_configuration.getProperty(timeStepKey))).multiply(timeStepUnitMultiplier);
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, "timestep is not a number", ex);
        }
        m_timeStep = tempTimestep;
        
        //initializing the number of electron
        int tempnElectron = 0;
        try
        {
            tempnElectron = Integer.parseInt(p_configuration.getProperty("number_simulated_electron"));
        }
        catch(NumberFormatException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, "number of electrons has to be an integer", ex);
        }
        m_nElectron = tempnElectron;
        
        //initializing the maximum number of loops
        if (m_isFittingMode)
        {
            int tempnLoops = 0;
            try
            {
                tempnLoops = Integer.parseInt(p_configuration.getProperty("maximum_fitting_repetition"));
            }
            catch(NumberFormatException ex)
            {
                Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, "maximum number of fitting loop has to be an integer", ex);
            }
            m_maxLoop = tempnLoops;
        }
        else
        {
            m_maxLoop = 1;
        }

        //creating needed directory to store result
        File spectraDirectory = new File(m_calculatedSpectraDirectory);
        File TRPLDirectory = new File(m_calculatedTimeResolvedPLDirectory);
        File QDListDirectory = new File(m_fittedQDListsDirectory);
        if(!spectraDirectory.isDirectory())
        {
            spectraDirectory.mkdirs();
        }
        if(!TRPLDirectory.isDirectory())
        {
            TRPLDirectory.mkdirs();
        }
        if(!QDListDirectory.isDirectory())
        {
            QDListDirectory.mkdirs();
        }
        
        //testing if gnuplot is installed
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

        //getting the luminescence as a function if in fitting mode, else the object is initialized to null
        if (m_isFittingMode)
        {
            HashMap<BigDecimal, BigDecimal> lumValues = new HashMap<>();
            BigDecimal maxCounts = BigDecimal.ZERO;
            try
            {
                BufferedReader lumReader = new BufferedReader(new FileReader(new File(p_configuration.getProperty("luminescence"))));
                Pattern numberRegex = Pattern.compile("^\\-?\\d+(\\.\\d+(e(\\+|\\-)\\d+)?)?");
                String line;
                while (((line = lumReader.readLine()) != null))
                {	    
                    String[] lineSplit = line.strip().split(";");

                    if(numberRegex.matcher(lineSplit[0]).matches())
                    {
                        BigDecimal wavelength = (new BigDecimal(lineSplit[0])).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier());
                        BigDecimal counts = new BigDecimal(lineSplit[1]);

                        lumValues.put(wavelength, counts);

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
            
            //if gnuplot is installed, we will use the luminescence to generate spectra image, so we save it in a format we know is readable by gnuplot
            m_luminescenceFile = new File(m_calculatedSpectraDirectory + "/Luminescence.dat");
            if (m_gnuplotInstalled)
            {
                try
                {
                    //creating the luminescence file for gnuplot
                    Set<BigDecimal> energySet = m_luminescence.getAbscissa();
                    BufferedWriter lumWriter = new BufferedWriter(new FileWriter(m_luminescenceFile));
                    lumWriter.write("Wavelength (nm)\tIntensity (cps)");
                    for (BigDecimal abscissa: energySet)
                    {
                        BigDecimal wavelengthNano = abscissa.divide(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);

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
        }
        else
        {
            m_luminescence = null;
            m_luminescenceFile = null;
        }
        
        //creating the material and metamaterial database
        String materialDirectoryPath = "ressources/materials/";
        Map<String, Material> materialList = new HashMap<>();
        File materialDirectory = new File(materialDirectoryPath);
        SCSVLoader functionLoader = new SCSVLoader();
        for (String fileName: materialDirectory.list())
        {
            String[] fileNameSplit = fileName.split("\\.");
            if (fileNameSplit.length > 0 && fileNameSplit[fileNameSplit.length-1].equals("mat"))
            {
                try
                {
                    FileReader parameterReader = new FileReader(new File(materialDirectoryPath + fileName));
                    Properties materialParameters = new Properties();
                    materialParameters.load(parameterReader);

                    materialList.put(materialParameters.getProperty("name"), new Material(materialParameters, functionLoader));
                }
                catch (IOException ex)
                {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        String metamaterialDirectoryPath = "ressources/metamaterials/";
        Map<String, Metamaterial> metamaterialList = new HashMap<>();
        File metamaterialDirectory = new File(metamaterialDirectoryPath);
        for (String fileName: metamaterialDirectory.list())
        {
            String[] fileNameSplit = fileName.split("\\.");
            if (fileNameSplit.length > 0 && fileNameSplit[fileNameSplit.length-1].equals("metamat"))
            {
                try
                {
                    FileReader parameterReader = new FileReader(new File(metamaterialDirectoryPath + fileName));
                    Properties metamaterialParameters = new Properties();
                    metamaterialParameters.load(parameterReader);

                    metamaterialList.put(metamaterialParameters.getProperty("name"), new Metamaterial(metamaterialParameters, materialList));
                }
                catch (IOException ex)
                {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        //Loading the material or matematerial in the sample
        String sampleMaterialID = p_configuration.getProperty("material");
        if (!metamaterialList.keySet().contains(sampleMaterialID))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new InvalidParameterException("Material(s) in the sample are not defined"));
        }
        if (sampleMaterialID.equals(""))
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, new InvalidParameterException("Sample material not given."));
        }
        m_sampleMaterial = metamaterialList.get(sampleMaterialID);
        
        //generating the QDs to be send
        String qdsPath = p_configuration.getProperty("QDs_distribution");
        try
        {
            //getting the functions giving the capture time, escape time and recombination time as a function of the size of the QD.
            //capture time reference: https://aip.scitation.org/doi/10.1063/1.1512694
            //escape time reference: https://aip.scitation.org/doi/10.1063/1.4824469
            
            //making the QD distribution
            if (qdsPath.equals(""))
            {
                //QDs are randomly generated with size following a normal distribution
                int nQDs = 300;
                for (int i = 0 ; i < nQDs ; i += 1)
                {
                    BigDecimal x, y, radius, height;
                    QuantumDot createdQD;

                    do
                    {
                        x = formatBigDecimal((new BigDecimal(m_RNGenerator.nextDouble())).multiply(p_sampleXSize));
                        y = formatBigDecimal((new BigDecimal(m_RNGenerator.nextDouble())).multiply(p_sampleYSize));

                        do
                        {
                            radius = formatBigDecimal((new BigDecimal(m_RNGenerator.nextGaussian() * 2.1 + 12)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));

                        }while (radius.compareTo(BigDecimal.ZERO) <= 0);
                        
                        do
                        {
                            height = formatBigDecimal((new BigDecimal(m_RNGenerator.nextGaussian() * 0.6 + 2.5)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                        }while(height.compareTo(BigDecimal.ZERO) <= 0);

                        createdQD = new QuantumDot(x, y, radius, height, m_timeStep, m_sampleMaterial);

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

                        QuantumDot currentQD = new QuantumDot(x, y, radius, height, m_timeStep, m_sampleMaterial);
                        m_QDList.add(currentQD);
                    }
                }
            }
        }
        catch (DataFormatException|IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void launchCalculation()
    {
        ImageInterpretator GUICommunicator = new ImageInterpretator(m_gui);
        
        try
        {
            GeneratorManager luminescenceGenerator = new GeneratorManager(GUICommunicator, m_nElectron, m_QDList, new BigDecimal("300"), m_timeStep, m_sampleXSize, m_sampleYSize);
            Thread generatorThread = new Thread(luminescenceGenerator);
            
            ResultMonitor monitor = new ResultMonitor(this, luminescenceGenerator, generatorThread);
            Thread monitorThread = new Thread(monitor);
            
            System.out.println("Starting simulation " + (m_loopCounter + 1));
            m_gui.sendMessage("Starting simulation " + (m_loopCounter + 1));
            m_gui.setProgressTitle("Simulation " + (m_loopCounter + 1) + " progress:");
            m_gui.updateProgress(0.0, "0 ns", "0/" + m_nElectron);
            startTime = Instant.now();
            generatorThread.start();
            monitorThread.start();
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
    
    private void createPictures (int p_fileIndex, String p_spectraFile, String p_TRPLFile)
    {
        try
        {
            String spectraFile = m_calculatedSpectraDirectory + "Spectra" + p_fileIndex + ".png";
            String timeResolvedFile = m_calculatedTimeResolvedPLDirectory + "TimeResolved" + p_fileIndex + ".png";
            String experimentalLumLine = "";
            if (m_isFittingMode)
            {
                experimentalLumLine = ","  + m_luminescenceFile.getCanonicalPath() + "\" u 1:2 w line t \"Experimental Lum\"";
            }
            
            BufferedWriter gnuplotWriter = new BufferedWriter(new FileWriter(m_resultDirectory + ".gnuplotScript.gp"));
            gnuplotWriter.write("set terminal png");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set ylabel \"Intensity (arb. units.)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Wavelength (nm)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + spectraFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot[*:*][0:1.1] \"" + p_spectraFile + "\" u 1:2 w line t \"Calculated Lum\"" + experimentalLumLine);
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set output \"" + timeResolvedFile + "\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("set xlabel \"Time (ns)\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("plot \"" + p_TRPLFile + "\" u ($1/1000):2 w points t \"Time Resolved Luminescence\"");
            gnuplotWriter.newLine();
            gnuplotWriter.write("unset output");
            gnuplotWriter.flush();
            gnuplotWriter.close();

            Runtime.getRuntime().exec("gnuplot " + m_resultDirectory + ".gnuplotScript.gp").waitFor();
            Runtime.getRuntime().exec("rm " + m_resultDirectory + ".gnuplotScript.gp");
        } 
        catch (IOException|InterruptedException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void computeResults(List<BigDecimal> p_recombinationEnergies, List<BigDecimal> p_recombinationTimes)
    {
        System.out.println("Simulation finished.");
        m_gui.sendMessage("Simulation finished.");
        
        //if there is a luminesence file (fitting case), we take its interval, otherwise we take a default 0. interval
        System.out.println("Sorting the results.");
        m_gui.sendMessage("Sorting the results.");
        BigDecimal spectraInterval = new BigDecimal("0.001").multiply(PhysicsTools.EV);
        if (m_isFittingMode)
        {
            spectraInterval = m_luminescence.getMeanIntervalSize();
        }
        SimulationSorter sorter = new SimulationSorter(spectraInterval, new ArrayList(p_recombinationTimes), new ArrayList(p_recombinationEnergies));
        
        QDFitter fit = new QDFitter();
        if (m_isFittingMode)
        {
            System.out.println("Trying to fit the luminescence.");
            m_gui.sendMessage("Trying to fit the luminescence.");
            fit = new QDFitter(m_QDList, m_timeStep, m_luminescence, sorter, m_gui, m_sampleMaterial);
        }
        
        m_loopCounter += 1;
        
        //saving the result to files and making pictures out of them if gnuplot is installed
        String spectraFilePath = m_calculatedSpectraDirectory + "Spectra" + m_loopCounter + ".dat";
        String TRPLFilePath = m_calculatedTimeResolvedPLDirectory + "TimeResolved" + m_loopCounter + ".dat";
        try
        {
            sorter.saveToFile(new File(TRPLFilePath), new File(spectraFilePath));
        }
        catch (IOException ex)
        {
            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (m_gnuplotInstalled)
        {
            createPictures(m_loopCounter, spectraFilePath, TRPLFilePath);
        }
        
        System.out.println("Saving fitted QD list.");
        m_gui.sendMessage("Saving fitted QD list.");
        try
        {
            BufferedWriter resultWriter = new BufferedWriter(new FileWriter(m_fittedQDListsDirectory + "FittedQDDistribution" + m_loopCounter + ".dat"));

            resultWriter.write("x (nm)\ty (nm)\tradius (nm)\theight (nm)\tenergy (eV)");
            resultWriter.newLine();
            for(QuantumDot qd: m_QDList)
            {
                resultWriter.write(qd.scaledString(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), PhysicsTools.EV));
                resultWriter.newLine();
            }

            resultWriter.flush();
            resultWriter.close();
        }
        catch (IOException ex)
        {
            System.out.println("x (nm)\ty (nm)\tradius (nm)\theight (nm)\tenergy (eV)");
            for (QuantumDot qd: m_QDList)
            {
                System.out.println(qd.scaledString(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), PhysicsTools.EV));
            }

            Logger.getLogger(ExecutionManager.class.getName()).log(Level.SEVERE, "Problem while writing the result file.", ex);
        }
        
        Instant endTime = Instant.now();
        System.out.println("Calculation time: " + Duration.between(startTime, endTime).toMinutes() + " min " + Duration.between(startTime, endTime).toSecondsPart() + " s");
        m_gui.sendMessage("Calculation time: " + Duration.between(startTime, endTime).toMinutes() + " min " + Duration.between(startTime, endTime).toSecondsPart() + " s");
        
        //testing if the simulation finished or has to continue
        if (!m_isFittingMode || fit.isGoodFit() || m_loopCounter >= m_maxLoop)
        {
            System.out.println("Ending the simulation.");
            m_gui.sendMessage("Ending the simulation.");
            
            if (m_gnuplotInstalled)
            {
                //creating a gif of the result if Image Magick is installed
                try
                {
                    if (m_maxLoop > 1)
                    {
                        Runtime.getRuntime().exec("convert -delay 500 " + m_calculatedSpectraDirectory + "Spectra*.png " + m_calculatedSpectraDirectory + "Spectra.gif");
                    }
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
                        m_gui.showPicture(new Image(new FileInputStream(m_calculatedSpectraDirectory + "Spectra" + m_loopCounter + ".png")), "Spectra", "left");
                        m_gui.showPicture(new Image(new FileInputStream(m_calculatedTimeResolvedPLDirectory + "TimeResolved" + m_loopCounter + ".png")), "Time Resolved", "right");
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
            //we can only enter there if we are in fitting mode, so no check needed
            m_QDList = fit.getFittedQDs();
            
            System.out.println("\nStarting a new simulation");
            m_gui.sendMessage("\nStarting a new simulation");
            launchCalculation();
        }
    }
    
    @Override
    public void run()
    {
        launchCalculation();
    }
}
