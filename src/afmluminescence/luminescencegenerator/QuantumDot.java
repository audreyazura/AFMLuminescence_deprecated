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
import java.math.BigDecimal;
import java.math.MathContext;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author Alban Lafuente
 */
public class QuantumDot extends AbsorberObject
{
    private final BigDecimal m_energy;
    private final BigDecimal m_radius;
    private boolean m_recombined = false;
    
    //ΔEg(InAs/GaAs) = 1.1 eV
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius, BigDecimal p_height)
    {
        BigDecimal two = new BigDecimal("2");
        BigDecimal three = new BigDecimal("3");
        BigDecimal pi = BigDecimalMath.pi(MathContext.DECIMAL128);
        PhysicsTools.Materials material = PhysicsTools.Materials.INAS;
        
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        m_radius = p_radius;
        
        //at the moment, approximation of energy modeling the QD as a cube with side length L = (2*radius + height)/3
        BigDecimal characteristicLength = ((p_radius.multiply(two)).add(p_height)).divide(three, MathContext.DECIMAL128);
        m_energy = material.getBaseBandgapSI().add((three.multiply(PhysicsTools.hbar.pow(2)).multiply(pi.pow(2))).divide(two.multiply(material.getElectronEffectiveMassSI()).multiply(characteristicLength.pow(2)), MathContext.DECIMAL128));
    }
    
    /**
     * The capture probability depends on many parameters and demand to be further investigate.
     * At the moment, it is approximated as the overlapping between the QD and the circle containing the positions the electron can reach
     * See here for the calculation of the overlap: https://www.xarg.org/2016/07/calculate-the-intersection-area-of-two-circles/
     * @param p_RNG the random number generator
     * @param electronDistance the distance between the center of the QD and electron position
     * @param electronSpan the circle containing the position the electron can reach
     * @return whether the electron has been captured or not
     */
    synchronized public boolean capture(PcgRSFast p_RNG, BigDecimal electronDistance, BigDecimal electronSpan)
    {
//        BigDecimal triangleBase = (m_radius.pow(2).add(electronDistance.pow(2)).subtract(electronSpan.pow(2))).divide(electronDistance.multiply(new BigDecimal("2")), MathContext.DECIMAL128);
//        if (triangleBase.compareTo(BigDecimal.ZERO) < 0)
//        {
//            System.err.println("PROBLEM!!!!!!!!");
//        }
        
        double captureProba = 0.01;
        
        return p_RNG.nextDouble() < captureProba;
    }
    
    //will calculate probability based on phonon density
    synchronized public boolean escape(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < 0.01;
    }
    
    public BigDecimal getEnergy()
    {
        return m_energy;
    }
    
    public BigDecimal getRadius()
    {
        return m_radius;
    }
    
    public boolean hasRecombined()
    {
        return m_recombined;
    }
    
    //will calculate the probablity based on the electron and hole wave function
    synchronized public boolean recombine(PcgRSFast p_RNG)
    {
        if (!m_recombined)
        {
            m_recombined = p_RNG.nextDouble() < 0.01;
        }
        
        return m_recombined;
    }
    
    public void resetRecombine()
    {
        m_recombined = false;
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; radius = " + m_radius;
    }
}
