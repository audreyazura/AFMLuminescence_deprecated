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
package afmluminescence.luminescencegenerator;

import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

/**
 *
 * @author Alban Lafuente
 */
public class GeneratorManager implements Runnable
{
    private final BigDecimal m_sampleXSize;
    private final BigDecimal m_sampleYSize;
    private final BigDecimal m_vth;
    private final ImageBuffer m_output;
    private final int m_nElectrons;
    private final List<QuantumDot> p_QDList = new ArrayList<>();
    
    //a map of the abscissa, separated in column, containing sets of QD present at that abscissa
    private final HashMap<BigInteger, Set<QuantumDot>> m_map = new HashMap<>();
    
    //this thread Random Generator
    private final PcgRSFast m_randomGenerator = new PcgRSFast();
    
    public GeneratorManager ()
    {
        m_sampleXSize = BigDecimal.ZERO;
        m_sampleYSize = BigDecimal.ZERO;
        m_vth = BigDecimal.ZERO;
        m_output = null;
        m_nElectrons = 0;
    }
    
    public GeneratorManager (ImageBuffer p_buffer, int p_nElectron, int p_nQDs, BigDecimal p_temperature, BigDecimal p_sampleX, BigDecimal p_sampleY)
    {
        m_output = p_buffer;
        m_nElectrons = p_nElectron;
        m_vth = formatBigDecimal((PhysicsTools.KB.multiply(p_temperature).divide(PhysicsTools.ME, MathContext.DECIMAL128)).sqrt(MathContext.DECIMAL128));
        
        m_sampleXSize = p_sampleX;
        m_sampleYSize = p_sampleY;

        //generating QDs
        for (int i = 0 ; i < p_nQDs ; i += 1)
        {
            QuantumDot currentQD = generateRandomQD(m_randomGenerator, p_QDList);
            p_QDList.add(currentQD);
            addToMap(currentQD);
        }
        m_output.logQDs(p_QDList);
    }
    
    public GeneratorManager (ImageBuffer p_buffer, int p_nElectron, File p_QDFile, BigDecimal p_temperature, BigDecimal p_sampleX, BigDecimal p_sampleY) throws DataFormatException, FileNotFoundException, IOException
    {
        m_output = p_buffer;
        m_nElectrons = p_nElectron;
        m_vth = formatBigDecimal((PhysicsTools.KB.multiply(p_temperature).divide(PhysicsTools.ME, MathContext.DECIMAL128)).sqrt(MathContext.DECIMAL128));
        
        m_sampleXSize = p_sampleX;
        m_sampleYSize = p_sampleY;

        //generating QDs
        String[] nameSplit = p_QDFile.getPath().split("\\.");
        
        if (!nameSplit[nameSplit.length-1].equals("csv"))
        {
            throw new DataFormatException();
        }
        
        BufferedReader fileReader = new BufferedReader(new FileReader(p_QDFile));
        Pattern numberRegex = Pattern.compile("^\\-?\\d+(\\.\\d+(e(\\+|\\-)\\d+)?)?");
	
	String line;
	while (((line = fileReader.readLine()) != null))
	{	    
	    String[] lineSplit = line.strip().split(";");
	    
	    if(numberRegex.matcher(lineSplit[0]).matches())
	    {
		BigDecimal radius = formatBigDecimal(((new BigDecimal(lineSplit[0].strip())).divide(new BigDecimal("2"))).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                BigDecimal x = formatBigDecimal((new BigDecimal(lineSplit[1].strip())).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                BigDecimal y = formatBigDecimal((new BigDecimal(lineSplit[2].strip())).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));
                
                QuantumDot currentQD = new QuantumDot(x, y, radius);
                p_QDList.add(currentQD);
                addToMap(currentQD);
	    }
        }
    }
    
    /**
     * Add the passed quantum dot to m_map at the right abscissa
     * @param p_QDToAdd 
     */
    private void addToMap(QuantumDot p_QDToAdd)
    {
        BigDecimal startAbscissa = (p_QDToAdd.getX().subtract(p_QDToAdd.getRadius())).scaleByPowerOfTen(PhysicsTools.UnitsPrefix.NANO.getScale());
        BigDecimal endAbscissa = (p_QDToAdd.getX().add(p_QDToAdd.getRadius())).scaleByPowerOfTen(PhysicsTools.UnitsPrefix.NANO.getScale());
        
        for (BigDecimal currentAbscissa = startAbscissa ; currentAbscissa.compareTo(endAbscissa) <= 0 ; currentAbscissa = currentAbscissa.add(BigDecimal.ONE))
        {
            BigInteger index = currentAbscissa.toBigInteger();
            
            Set<QuantumDot> currentSet = m_map.get(index);
            if (currentSet == null)
            {
                currentSet = new HashSet<>();
                currentSet.add(p_QDToAdd);
                m_map.put(index, currentSet);
            }
            else
            {
                currentSet.add(p_QDToAdd);
            }
        }
    }
    
    private QuantumDot generateRandomQD(PcgRSFast p_randomGenerator, List<QuantumDot> p_existingQDs)
    {
        BigDecimal x;
        BigDecimal y;
        BigDecimal radius;
        QuantumDot createdQD;
        
        x = formatBigDecimal((new BigDecimal(p_randomGenerator.nextDouble())).multiply(m_sampleXSize));
        y = formatBigDecimal((new BigDecimal(p_randomGenerator.nextDouble())).multiply(m_sampleYSize));

        do
        {
            radius = formatBigDecimal((new BigDecimal(p_randomGenerator.nextGaussian() * 2.1 + 12)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));

        }while (radius.compareTo(BigDecimal.ZERO) <= 0);

        createdQD = new QuantumDot(x, y, radius);
        
        while(!validPosition(createdQD, p_existingQDs))
        {
            x = formatBigDecimal((new BigDecimal(p_randomGenerator.nextDouble())).multiply(m_sampleXSize));
            y = formatBigDecimal((new BigDecimal(p_randomGenerator.nextDouble())).multiply(m_sampleYSize));
            
            do
            {
                radius = formatBigDecimal((new BigDecimal(p_randomGenerator.nextGaussian() * 2.1 + 12)).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()));

            }while (radius.compareTo(BigDecimal.ZERO) <= 0);
            
            createdQD = new QuantumDot(x, y, radius);
        }
        
        return createdQD;
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
    
    @Override
    public void run()
    {
        BigDecimal x;
        BigDecimal y;

        //generating electrons
        BigDecimal v_x;
        BigDecimal v_y;
        List<Electron> electronList = new ArrayList<>();
        for (int i = 0 ; i < m_nElectrons ; i += 1)
        {
            x = formatBigDecimal((new BigDecimal(m_randomGenerator.nextDouble())).multiply(m_sampleXSize));
            y = formatBigDecimal((new BigDecimal(m_randomGenerator.nextDouble())).multiply(m_sampleYSize));
            
            v_x = formatBigDecimal((new BigDecimal(m_randomGenerator.nextGaussian())).multiply(m_vth));
            v_y = formatBigDecimal((new BigDecimal(m_randomGenerator.nextGaussian())).multiply(m_vth));
            
            electronList.add(new Electron(x, y, v_x, v_y));
        }
        m_output.logElectrons(electronList);
        
        BigDecimal timeStep = new BigDecimal("1e-12");
//        BigDecimal timeStep = new BigDecimal("1e-14");
        
        //cutting calculation into chunks to distribute it between cores
        int numberOfChunks = Integer.min(Runtime.getRuntime().availableProcessors(), electronList.size());
        Iterator<Electron> electronIterator = electronList.iterator();
        ArrayList<Electron>[] electronChunks = new ArrayList[numberOfChunks];
        for (int i = 0 ; i < numberOfChunks ; i += 1)
        {
            electronChunks[i] = new ArrayList<>();
        }
        
        int nElectronTreated = 0;
        while (electronIterator.hasNext())
        {
            electronChunks[nElectronTreated%numberOfChunks].add(electronIterator.next());
            nElectronTreated += 1;
        }
        
        Thread[] workerArray = new Thread[numberOfChunks];
        ElectronMover[] moverArray = new ElectronMover[numberOfChunks];
        for (int i = 0 ; i < numberOfChunks ; i += 1)
        {
            moverArray[i] = new ElectronMover(m_sampleXSize, m_sampleYSize, timeStep, m_vth, electronChunks[i], m_map);
        }
        
        //calculation start!
        BigDecimal timePassed = BigDecimal.ZERO;
        m_output.logTime(timePassed);
        ArrayList<Electron> currentELectronList;
        boolean allFinished = false;
        try
        {
            while(!allFinished)
            {
                currentELectronList = new ArrayList<>();
                allFinished = true;
                for (int i = 0 ; i < numberOfChunks ; i += 1)
                {
                    workerArray[i] = new Thread(moverArray[i]);
                    workerArray[i].start();
                }
                for (int i = 0 ; i < numberOfChunks ; i += 1)
                {
                    workerArray[i].join();
                    currentELectronList.addAll(moverArray[i].getElectronList());
                    allFinished &= moverArray[i].allRecombined();
                }
                timePassed = timePassed.add(timeStep);
                m_output.logElectrons(currentELectronList);
                m_output.logTime(timePassed);
                m_output.logQDs(p_QDList);
                for (QuantumDot QD: p_QDList)
                {
                    QD.resetRecombine();
                }
            }
            
            //log the recombination profile here by taking the energy of the QD the electrons recombined.
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(GeneratorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static BigDecimal formatBigDecimal(BigDecimal p_toFormat)
    {
        return p_toFormat.stripTrailingZeros();
    }
}
