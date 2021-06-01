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

import afmluminescence.luminescencegenerator.Electron;
import afmluminescence.luminescencegenerator.QuantumDot;
import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.kilianB.pcg.fast.PcgRSFast;
import com.sun.jdi.AbsentInformationException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author audreyazura
 */
public class QDFitter
{
    private final boolean m_goodFit;
    private final List<QuantumDot> m_QDList = new ArrayList<>();
    
    public QDFitter (List<QuantumDot> p_QDList, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes, ContinuousFunction p_luminescence, SimulationSorter p_sorter)
    {
        SimulationJudge judge = new SimulationJudge(p_luminescence, p_sorter.getLuminescence());
        m_goodFit = judge.maximumMatch() && judge.shapeMatch();
        
        if (!m_goodFit)
        {
            if (!judge.maximumMatch())
            {
                ArrayList<QuantumDot> oldQDList = new ArrayList(p_QDList);
                
                BigDecimal multiplier = BigDecimal.ONE.divide(judge.maximumRatio(), MathContext.DECIMAL128);
                for (QuantumDot oldQD: oldQDList)
                {
                    m_QDList.add(oldQD.copyWithSizeChange(multiplier, p_timeStep, p_captureTimes, p_escapeTimes));
                }
            }
            
            if (!judge.shapeMatch())
            {
                //aussi mettre un max et un min dans la forme ?
                BigDecimal highEnergyDiff = judge.shapeDifferenceRatio();
                BigDecimal pivotEnergy = p_sorter.getLuminescence().maximum().get("abscissa");
                
                ArrayList<QuantumDot> highEnergyQDs = new ArrayList<>();
                ArrayList<QuantumDot> lowEnergyQDs = new ArrayList<>();
                ArrayList<QuantumDot> maxEnergyQDs = new ArrayList<>();
                for (QuantumDot qd: p_QDList)
                {
                    if (qd.getEnergy().compareTo(pivotEnergy) > 0)
                    {
                        highEnergyQDs.add(qd);
                    }
                    else
                    {
                        if (qd.getEnergy().compareTo(pivotEnergy) < 0)
                        {
                            lowEnergyQDs.add(qd);
                        }
                        else
                        {
                            maxEnergyQDs.add(qd);
                        }
                    }
                }
                
                if (highEnergyDiff.signum() == 1) //positive difference
                {
                    //take HE to LE
                    BigDecimal numberToSwap = highEnergyDiff.multiply(new BigDecimal(m_QDList.size()));
                    double swapProba = (numberToSwap.doubleValue())/highEnergyQDs.size();
                    
                    if (swapProba < 0 || swapProba > 1)
                    {
                        throw new ArithmeticException("Probability of swapping invalid.");
                    }
                    else
                    {
                        BigDecimal highEnergyExperimentalInterval = p_luminescence.end().subtract(p_luminescence.maximum().get("abscissa"));
                        highEnergyQDs = swapQD(highEnergyQDs, highEnergyExperimentalInterval, pivotEnergy, p_timeStep, p_captureTimes, p_escapeTimes, swapProba);
                    }
                }
                else //negative difference
                {
                    //take LE to HE
                    BigDecimal numberToSwap = (BigDecimal.ONE.subtract(highEnergyDiff.abs())).multiply(new BigDecimal(m_QDList.size()));
                    double swapProba = (numberToSwap.doubleValue())/lowEnergyQDs.size();
                    
                    if (swapProba < 0)
                    {
                        throw new ArithmeticException("Probability of swapping invalid.");
                    }
                    else
                    {
                        BigDecimal lowEnergyExperimentalInterval = p_luminescence.start().subtract(p_luminescence.maximum().get("abscissa"));
                        lowEnergyQDs = swapQD(lowEnergyQDs, lowEnergyExperimentalInterval, pivotEnergy, p_timeStep, p_captureTimes, p_escapeTimes, swapProba);
                    }
                }
                
                //we group all the new QDs in a single list (the low energy one) and save it in m_QDList
                m_QDList.addAll(lowEnergyQDs);
                m_QDList.addAll(maxEnergyQDs);
                m_QDList.addAll(highEnergyQDs);
            }
        }
    }
    
    private ArrayList<QuantumDot> swapQD (ArrayList<QuantumDot> p_qdToSwap, BigDecimal p_intervalSize, BigDecimal p_pivotEnergy, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes, double p_swapProba)
    {
        ArrayList<QuantumDot> swappedList = new ArrayList<>();
        PcgRSFast RNGenerator = new PcgRSFast();
        
        for (QuantumDot qd: p_qdToSwap)
        {
            if (RNGenerator.nextDouble() < p_swapProba)
            {
                //we select the new QD energy randomly in the interval ]maxEnergy, maxEnergy+intervalSize]. intervalSize can be negative.
                BigDecimal newQDEnergy = BigDecimal.ZERO;
                do
                {
                    newQDEnergy = p_pivotEnergy.add(p_intervalSize.multiply(new BigDecimal(RNGenerator.nextDouble(false, true))));
                }while(newQDEnergy.signum() < 0);
                
                BigDecimal sizeMultiplier = qd.getEnergy().divide(newQDEnergy, MathContext.DECIMAL128); //energy multiplier = newEnergy / oldEnergy, size multiplier = 1 / (energy multiplier)
                qd = qd.copyWithSizeChange(sizeMultiplier, p_timeStep, p_captureTimes, p_escapeTimes);
            }
            
            swappedList.add(qd);
        }
        
        return swappedList;
    }
    
    public ArrayList<QuantumDot> getFittedQDs()
    {
        return new ArrayList(m_QDList);
    }
}
