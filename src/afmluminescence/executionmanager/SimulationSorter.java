/*
 * Copyright (C) 2021 alafuente
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

import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author alafuente
 */
public class SimulationSorter
{
    private final ContinuousFunction m_spectra;
    private final HashMap<BigDecimal, Integer> m_densityOfStates = new HashMap<>();
    private final HashMap<BigDecimal, BigDecimal> m_times = new HashMap<>();
    private final HashMap<BigDecimal, BigDecimal> m_wavelengths = new HashMap<>();
    
    public SimulationSorter (BigDecimal p_wavelengthIntervalSize, List<BigDecimal> p_timesList, List<BigDecimal> p_wavelengthList, List<BigDecimal> p_energyLevels)
    {
        p_timesList.sort(null);
        p_wavelengthList.sort(null);
        p_energyLevels.sort(null);
        
        //INTERVAL CHOICE TO BE REWORKED, DOESN'T WORK WELL AT THE MOMENT

        //cutting the timespan of the experiment into a given number of intervals (here 5000) and puting the number of recombined electrons during each intervals
        BigDecimal maxTime = p_timesList.get(p_timesList.size() - 1);
        BigDecimal timeInterval = maxTime.divide(new BigDecimal("5000"), MathContext.DECIMAL128);
        for (BigDecimal currentTime = BigDecimal.ZERO ; currentTime.compareTo(maxTime) == -1 ; currentTime = currentTime.add(timeInterval))
        {
            BigDecimal currentMax = currentTime.add(timeInterval);
            int nRecomb = 0;
            
            while (p_timesList.size() > 0 && p_timesList.get(0).compareTo(currentMax) <= 0)
            {
                nRecomb += 1;
                p_timesList.remove(0);
            }
            
            m_times.put(currentTime, new BigDecimal(nRecomb));
        }
        
        //the interval for the energy is given in the constructor
        BigDecimal minWavelength = p_wavelengthList.get(0);
        BigDecimal maxWavelength = p_wavelengthList.get(p_wavelengthList.size() - 1);
        BigDecimal maxCounts = BigDecimal.ZERO;
        
        for (BigDecimal currentWavelength = minWavelength ; currentWavelength.compareTo(maxWavelength) == -1 ; currentWavelength = currentWavelength.add(p_wavelengthIntervalSize))
        {
            BigDecimal currentMax = currentWavelength.add(p_wavelengthIntervalSize);
            int nWavelengths = 0;
            
            while (p_wavelengthList.size() > 0 && p_wavelengthList.get(0).compareTo(currentMax) <= 0)
            {
                nWavelengths += 1;
                p_wavelengthList.remove(0);
            }
            
            BigDecimal nWavelengthsBig = new BigDecimal(nWavelengths);
            m_wavelengths.put(currentWavelength, new BigDecimal(nWavelengths));
            
            if (nWavelengthsBig.compareTo(maxCounts) > 0)
            {
                maxCounts = nWavelengthsBig;
            }
        }
        
        //normalisation
        for (BigDecimal wavelength: m_wavelengths.keySet())
        {
            m_wavelengths.put(wavelength, m_wavelengths.get(wavelength).divide(maxCounts, MathContext.DECIMAL128));
        }
        
        m_spectra = new ContinuousFunction(m_wavelengths);
        
        //density of state calculation
        BigDecimal minEnergy = p_energyLevels.get(0);
        BigDecimal maxEnergy = p_energyLevels.get(p_energyLevels.size() - 1);
        BigDecimal DOSInterval = (new BigDecimal("0.001")).multiply(PhysicsTools.EV);
        
        for (BigDecimal lowestBound = minEnergy ; lowestBound.compareTo(maxEnergy) == -1 ; lowestBound = lowestBound.add(DOSInterval))
        {
            BigDecimal currentMax = lowestBound.add(DOSInterval);
            int nLevels = 0;
            
            while (p_energyLevels.size() > 0 && p_energyLevels.get(0).compareTo(currentMax) <= 0)
            {
                nLevels += 1;
                p_energyLevels.remove(0);
            }
            
            m_densityOfStates.put(lowestBound, nLevels);
        }
    }
    
    static public SimulationSorter sorterWithNoIntervalGiven(List<BigDecimal> p_timesList, List<BigDecimal> p_energiesList, List<BigDecimal> p_energyLevels)
    {
        //guessing a good energy interval size: separating the energy span into 
        p_energiesList.sort(null);
        BigDecimal minWavelength = p_energiesList.get(0);
        BigDecimal maxWavelength = p_energiesList.get(p_energiesList.size() - 1);
        BigDecimal wavelengthInterval = (maxWavelength.subtract(minWavelength)).divide(new BigDecimal("150"), MathContext.DECIMAL128);
        
        return new SimulationSorter(wavelengthInterval, p_timesList, p_energiesList, p_energyLevels);
    }
    
    public void saveToFile(File timeFile, File energyFile, File DOSFile) throws IOException
    {
        //writing times
        Set<BigDecimal> timeSet = new TreeSet(m_times.keySet());
        BufferedWriter timeWriter = new BufferedWriter(new FileWriter(timeFile));
        timeWriter.write("Time (ps)\tIntensity (cps)");
        for (BigDecimal time: timeSet)
        {
            timeWriter.newLine();
            timeWriter.write(time.divide(PhysicsTools.UnitsPrefix.PICO.getMultiplier(), MathContext.DECIMAL128).toPlainString() + "\t" + m_times.get(time));
        }
        timeWriter.flush();
        timeWriter.close();
        
        //writing wavelength calculated from energies
        Set<BigDecimal> wavelengthSet = new TreeSet(m_wavelengths.keySet());
        BufferedWriter spectraWriter = new BufferedWriter(new FileWriter(energyFile));
        spectraWriter.write("Wavelength (nm)\tIntensity");
        for (BigDecimal wavelength: wavelengthSet)
        {
//            BigDecimal wavelengthNano = ((PhysicsTools.h.multiply(PhysicsTools.c)).divide(energy, MathContext.DECIMAL128)).divide(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);
            BigDecimal wavelengthNano = wavelength.divide(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);
            
            spectraWriter.newLine();
            spectraWriter.write(wavelengthNano.toPlainString() + "\t" + m_wavelengths.get(wavelength));
        }
        spectraWriter.flush();
        spectraWriter.close();
        
        //writing DOS
        Set<BigDecimal> statesSet = new TreeSet<>(m_densityOfStates.keySet());
        BufferedWriter DOSwriter = new BufferedWriter(new FileWriter(DOSFile));
        DOSwriter.write("Energy (eV)\tNumber of states");
        for (BigDecimal state: statesSet)
        {
            BigDecimal stateToWrite = state.divide(PhysicsTools.EV, MathContext.DECIMAL128).setScale(state.scale() - state.precision() + 4, RoundingMode.HALF_UP);
            
            DOSwriter.newLine();
            DOSwriter.write(stateToWrite.toPlainString() + "\t" + m_densityOfStates.get(state));
        }
        DOSwriter.flush();
        DOSwriter.close();
    }
    
    public ContinuousFunction getLuminescence()
    {
        return new ContinuousFunction(m_spectra);
    }
}