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
    private final ContinuousFunction m_energyFunction;
    private final HashMap<BigDecimal, BigDecimal> m_times = new HashMap<>();
    private final HashMap<BigDecimal, BigDecimal> m_energies = new HashMap<>();
    
    public SimulationSorter (BigDecimal p_energyIntervalSize, List<BigDecimal> p_timesList, List<BigDecimal> p_energiesList)
    {
        p_timesList.sort(null);
        p_energiesList.sort(null);
        
        //cutting the timespan of the experiment into a given number of intervals (here 100) and puting the number of recombined electrons during each intervals
        BigDecimal maxTime = p_timesList.get(p_timesList.size() - 1);
        BigDecimal timeInterval = maxTime.divide(new BigDecimal("4000"), MathContext.DECIMAL128);
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
        BigDecimal minEnergy = p_energiesList.get(0);
        BigDecimal maxEnergy = p_energiesList.get(p_energiesList.size() - 1);
        BigDecimal maxCounts = BigDecimal.ZERO;
        
        for (BigDecimal currentEnergy = minEnergy ; currentEnergy.compareTo(maxEnergy) == -1 ; currentEnergy = currentEnergy.add(p_energyIntervalSize))
        {
            BigDecimal currentMax = currentEnergy.add(p_energyIntervalSize);
            int nEnergy = 0;
            
            while (p_energiesList.size() > 0 && p_energiesList.get(0).compareTo(currentMax) <= 0)
            {
                nEnergy += 1;
                p_energiesList.remove(0);
            }
            
            BigDecimal nEnergyBig = new BigDecimal(nEnergy);
            m_energies.put(currentEnergy, new BigDecimal(nEnergy));
            
            if (nEnergyBig.compareTo(maxCounts) > 0)
            {
                maxCounts = nEnergyBig;
            }
        }
        
        //normalisation
        for (BigDecimal energy: m_energies.keySet())
        {
            m_energies.put(energy, m_energies.get(energy).divide(maxCounts, MathContext.DECIMAL128));
        }
        
        m_energyFunction = new ContinuousFunction(m_energies);
    }
    
    static public SimulationSorter sorterWithNoIntervalGiven(List<BigDecimal> p_timesList, List<BigDecimal> p_energiesList)
    {
        //guessing a good energy interval size: separating the energy span into 
        p_energiesList.sort(null);
        BigDecimal minEnergy = p_energiesList.get(0);
        BigDecimal maxEnergy = p_energiesList.get(p_energiesList.size() - 1);
        BigDecimal energyInterval = (maxEnergy.subtract(minEnergy)).divide(new BigDecimal("150"), MathContext.DECIMAL128);
        
        return new SimulationSorter(energyInterval, p_timesList, p_energiesList);
    }
    
    public void saveToFile(File timeFile, File energyFile) throws IOException
    {
        if (!timeFile.getParentFile().isDirectory())
        {
            timeFile.getParentFile().mkdirs();
        }
        if (!energyFile.getParentFile().isDirectory())
        {
            timeFile.getParentFile().mkdirs();
        }
        
        //writing times
        Set<BigDecimal> timeSet = new TreeSet(m_times.keySet());
        BufferedWriter timeWriter = new BufferedWriter(new FileWriter(timeFile));
        timeWriter.write("Time (ps)\tIntensity (cps)");
        for (BigDecimal time: timeSet)
        {
            timeWriter.newLine();
            timeWriter.write(time.divide(PhysicsTools.UnitsPrefix.PICO.getMultiplier()).toPlainString() + "\t" + m_times.get(time));
        }
        timeWriter.flush();
        timeWriter.close();
        
        //writing wavelength calculated from energies
        Set<BigDecimal> energySet = new TreeSet(m_energies.keySet());
        BufferedWriter energyWriter = new BufferedWriter(new FileWriter(energyFile));
        energyWriter.write("Wavelength (nm)\tIntensity (cps)");
        for (BigDecimal energy: energySet)
        {
            BigDecimal wavelengthNano = ((PhysicsTools.h.multiply(PhysicsTools.c)).divide(energy, MathContext.DECIMAL128)).divide(PhysicsTools.UnitsPrefix.NANO.getMultiplier(), MathContext.DECIMAL128);
            
            energyWriter.newLine();
            energyWriter.write(wavelengthNano.toPlainString() + "\t" + m_energies.get(energy));
        }
        energyWriter.flush();
        energyWriter.close();
    }
    
    public ContinuousFunction getLuminescence()
    {
        return new ContinuousFunction(m_energyFunction);
    }
}