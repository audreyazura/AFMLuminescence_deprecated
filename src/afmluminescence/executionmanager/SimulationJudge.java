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

import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 *
 * @author audreyazura
 */
public class SimulationJudge
{
    private final MatchObject m_maxMatching;
    private final MatchObject m_shapeMatchingHighEnergy;
    
    public SimulationJudge (ContinuousFunction p_experimentalLuminescence, ContinuousFunction p_simulatedLuminescence)
    {
        BigDecimal experimentalMaxPosition = p_experimentalLuminescence.maximum().get("abscissa");
        BigDecimal simulatedMaxPosition = p_simulatedLuminescence.maximum().get("abscissa");
        
        //comparing the position of maximum
        //supposing an acceptable error on the abscissa of +/-1 meV
        BigDecimal maxError = (new BigDecimal("0.001")).multiply(PhysicsTools.EV);
        BigDecimal differenceMaxEnergy = experimentalMaxPosition.subtract(simulatedMaxPosition);
        if(differenceMaxEnergy.abs().compareTo(maxError) <= 0)
        {
            m_maxMatching = new MatchObject(true, "");
        }
        else
        {
            m_maxMatching = new MatchObject(false, (experimentalMaxPosition.divide(simulatedMaxPosition, MathContext.DECIMAL128)).toString());
        }
        
        //comparing the overall shape
        BigDecimal simuLowEnergyRatio = BigDecimal.ZERO;
        if(p_simulatedLuminescence.start().compareTo(simulatedMaxPosition) != 0)
        {
            simuLowEnergyRatio = p_simulatedLuminescence.integrate(p_simulatedLuminescence.start(), simulatedMaxPosition).divide(p_simulatedLuminescence.integrate(), MathContext.DECIMAL128);
        }
        
        BigDecimal experimentLowEnergyRatio = BigDecimal.ZERO;
        if(p_experimentalLuminescence.start().compareTo(experimentalMaxPosition) != 0)
        {
            experimentLowEnergyRatio = p_experimentalLuminescence.integrate(p_experimentalLuminescence.start(), experimentalMaxPosition).divide(p_experimentalLuminescence.integrate(), MathContext.DECIMAL128);
        }
        
        BigDecimal simuHighEnergyRatio = BigDecimal.ZERO;
        if(p_simulatedLuminescence.end().compareTo(simulatedMaxPosition) != 0)
        {
            simuHighEnergyRatio = p_simulatedLuminescence.integrate(simulatedMaxPosition, p_simulatedLuminescence.end()).divide(p_simulatedLuminescence.integrate(), MathContext.DECIMAL128);
        }
        
        BigDecimal experimentHighEnergyRatio = BigDecimal.ZERO;
        if(p_experimentalLuminescence.end().compareTo(experimentalMaxPosition) != 0)
        {
            experimentHighEnergyRatio = p_experimentalLuminescence.integrate(experimentalMaxPosition, p_experimentalLuminescence.end()).divide(p_experimentalLuminescence.integrate(), MathContext.DECIMAL128);
        }
        
//        System.out.println("Low -> simu: " + simuLowEnergyRatio + "\t experiment: " + experimentLowEnergyRatio);
//        System.out.println("High -> simu: " + simuHighEnergyRatio + "\t experiment: " + experimentHighEnergyRatio);
        
        //putting an acceptable error on the shape of 5%
        BigDecimal maxErrorShape = new BigDecimal("0.05");
        BigDecimal differenceShapeHighEnergy = simuHighEnergyRatio.subtract(experimentHighEnergyRatio);
        BigDecimal differenceShapeLowEnergy = simuLowEnergyRatio.subtract(experimentLowEnergyRatio);
        
        //the error should compensate each other. If they don't, an error occured. We give ourselves a leeway of 1e-20.
        if((differenceShapeHighEnergy.add(differenceShapeLowEnergy)).compareTo(new BigDecimal("1e-20")) <= 0)
        {
            //the absolute error is basically the same on each side, therefore we can only care on one side
            m_shapeMatchingHighEnergy = new MatchObject(((differenceShapeHighEnergy.abs()).compareTo(maxErrorShape) <= 0), differenceShapeHighEnergy.toString());
        }
        else
        {
            System.out.println(simuHighEnergyRatio.add(simuLowEnergyRatio));
            throw new ArithmeticException("Sum of ratioed integral different than 1.");
        }
    }
    
    public boolean maximumMatch()
    {
        return m_maxMatching.isMatching();
    }
    
    public boolean shapeMatch()
    {
        return m_shapeMatchingHighEnergy.isMatching();
    }
    
    public BigDecimal maximumRatio() throws NumberFormatException
    {
        BigDecimal difference = BigDecimal.ZERO;
        
        if (!m_maxMatching.isMatching())
        {
            try
            {
                difference = new BigDecimal(m_maxMatching.comment());
            }
            catch (NumberFormatException exception)
            {
                throw exception;
            }
        }
        
        return difference;
    }
    
    public BigDecimal shapeDifferenceRatio() throws NumberFormatException
    {
        BigDecimal difference = BigDecimal.ZERO;
        
        if (!m_shapeMatchingHighEnergy.isMatching())
        {
            try
            {
                difference = new BigDecimal(m_shapeMatchingHighEnergy.comment());
            }
            catch (NumberFormatException exception)
            {
                throw exception;
            }
        }
        
        return difference;
    }
}
