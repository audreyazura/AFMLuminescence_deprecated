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

import afmluminescence.luminescencegenerator.QuantumDot;
import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author audreyazura
 */
public class QDFitter
{
    private final boolean m_goodFit;
    private final List<QuantumDot> m_QDList;
    
    public QDFitter (List<QuantumDot> p_QDList, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes, ContinuousFunction p_luminescence, SimulationSorter p_sorter)
    {
        //include the fact that there is QD outside the range in the judge, and distribute it better in the fitting function
        SimulationJudge judge = new SimulationJudge(p_luminescence, p_sorter.getLuminescence());
        m_goodFit = judge.maximumMatch() && judge.shapeMatch();
        
        if (!m_goodFit)
        {
            m_QDList = new ArrayList<>();
            
            if (!judge.maximumMatch())
            {
                System.out.println("Adjusting the position of the maximum.");
                
                ArrayList<QuantumDot> oldQDList = new ArrayList(p_QDList);
                
                BigDecimal multiplier = BigDecimal.ONE.divide(judge.maximumRatio(), MathContext.DECIMAL128);
                for (QuantumDot oldQD: oldQDList)
                {
                    m_QDList.add(oldQD.copyWithSizeChange(multiplier, p_timeStep, p_captureTimes, p_escapeTimes));
                }
            }
            
            if (!judge.shapeMatch())
            {
                System.out.println("Adjusting the distribution of QD around the maximum.");
                
                BigDecimal highEnergyDiff = judge.shapeDifferenceRatio();
                BigDecimal pivotEnergy = p_sorter.getLuminescence().maximum().get("abscissa");
                BigDecimal highEnergyExperimentalInterval = p_luminescence.end().subtract(p_luminescence.maximum().get("abscissa"));
                BigDecimal lowEnergyExperimentalInterval = p_luminescence.start().subtract(p_luminescence.maximum().get("abscissa"));
                
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
                    
                    if (swapProba < 0)
                    {
                        throw new ArithmeticException("Probability of swapping invalid.");
                    }
                    else
                    {
                        highEnergyQDs = swapQDs(highEnergyQDs, highEnergyExperimentalInterval, pivotEnergy, p_timeStep, p_captureTimes, p_escapeTimes, swapProba);
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
                        lowEnergyQDs = swapQDs(lowEnergyQDs, lowEnergyExperimentalInterval, pivotEnergy, p_timeStep, p_captureTimes, p_escapeTimes, swapProba);
                    }
                }
                
                //putting the QD which energy are too far from the maximum in range
                BigDecimal energyLimit;
                //high ernergy case
                energyLimit = pivotEnergy.add(highEnergyExperimentalInterval);
                for (QuantumDot qd: highEnergyQDs)
                {
                    if (qd.getEnergy().compareTo(pivotEnergy.add(energyLimit)) > 0)
                    {
                        highEnergyQDs.remove(qd);
                        highEnergyQDs.add(getQDInEnergyRange(qd, pivotEnergy, highEnergyExperimentalInterval, p_timeStep, p_captureTimes, p_escapeTimes));
                    }
                }
                //low energy case
                energyLimit = (pivotEnergy.subtract(lowEnergyExperimentalInterval)).max(BigDecimal.ZERO);
                for (QuantumDot qd: lowEnergyQDs)
                {
                    if (qd.getEnergy().compareTo(energyLimit) < 0)
                    {
                        lowEnergyQDs.remove(qd);
                        //the only case where pivotEnergy < lowEnergyExperimentalRange is when the energyLimit get lower to 0 (and thus, is put to 0 by the max in its initialisation)
                        lowEnergyQDs.add(getQDInEnergyRange(qd, energyLimit, lowEnergyExperimentalInterval.min(pivotEnergy), p_timeStep, p_captureTimes, p_escapeTimes));
                    }
                }
                
                //we group all the new QDs in a single list (the low energy one) and save it in m_QDList
                m_QDList.addAll(lowEnergyQDs);
                m_QDList.addAll(maxEnergyQDs);
                m_QDList.addAll(highEnergyQDs);
            }
        }
        else
        {
            m_QDList = new ArrayList(p_QDList);
            System.out.println("The simulation is in agreement with the experiment, nothing to do.");
        }
    }
    
    private QuantumDot getQDInEnergyRange (QuantumDot p_originalQD, BigDecimal p_rangeMin, BigDecimal p_intervalSize, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes)
    {
        BigDecimal newQDEnergy = BigDecimal.ZERO;
        PcgRSFast RNGenerator = new PcgRSFast();
        QuantumDot newQD;
        
        //we select the new QD energy randomly in the interval ]minEnergy, maxEnergy+intervalSize]. intervalSize can be negative.
        do
        {
            do
            {
                newQDEnergy = p_rangeMin.add(p_intervalSize.multiply(new BigDecimal(RNGenerator.nextDouble(true, true))));
            }while(newQDEnergy.signum() < 0);

            BigDecimal sizeMultiplier = p_originalQD.getEnergy().divide(newQDEnergy, MathContext.DECIMAL128); //energy multiplier = newEnergy / oldEnergy, size multiplier = 1 / (energy multiplier)
            newQD = p_originalQD.copyWithSizeChange(sizeMultiplier, p_timeStep, p_captureTimes, p_escapeTimes);
        }while (newQD.getEnergy().compareTo(p_rangeMin) < 0 || newQD.getEnergy().compareTo(p_rangeMin.add(p_intervalSize)) > 0);
        
        return newQD;
    }
    
    private ArrayList<QuantumDot> swapQDs (ArrayList<QuantumDot> p_qdToSwap, BigDecimal p_intervalSize, BigDecimal p_pivotEnergy, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes, double p_swapProba)
    {
        ArrayList<QuantumDot> swappedList = new ArrayList<>();
        PcgRSFast RNGenerator = new PcgRSFast();
        
        for (QuantumDot qd: p_qdToSwap)
        {
            if (RNGenerator.nextDouble() < p_swapProba)
            {
                qd = getQDInEnergyRange(qd, p_pivotEnergy, p_intervalSize, p_timeStep, p_captureTimes, p_escapeTimes);
            }
            
            swappedList.add(qd);
        }
        
        return swappedList;
    }
    
    public ArrayList<QuantumDot> getFittedQDs()
    {
        return new ArrayList(m_QDList);
    }
    
    public boolean isGoodFit()
    {
        return m_goodFit;
    }
}
