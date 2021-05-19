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

import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author Alban Lafuente
 */
public class QuantumDot extends AbsorberObject
{
    private final BigDecimal m_energy;
    private final BigDecimal m_radius;
    private final BigDecimal m_height;
    private final double m_captureProba;
    private final double m_escapeProbability;
    private final double m_recombinationProbability;
    private boolean m_recombined = false;
    
    //ΔEg(InAs/GaAs) ~ 1.1 eV
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius, BigDecimal p_height, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes)
    {
        BigDecimal two = new BigDecimal("2");
        BigDecimal eight = new BigDecimal("8");
        PhysicsTools.Materials QDMaterial = PhysicsTools.Materials.INAS;
        PhysicsTools.Materials hostMaterial = PhysicsTools.Materials.GAAS;
        
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        m_radius = p_radius.multiply(new BigDecimal("1"));
        m_height = p_height;
//        BigDecimal height = p_height.multiply(new BigDecimal("2")); //multiplied to have enough QDs that can capture (some problem with file?)
        BigDecimal equivalentSquareSide = m_radius.multiply(BigDecimalMath.sqrt(BigDecimalMath.pi(MathContext.DECIMAL128), MathContext.DECIMAL128));
        
        BigDecimal CBOffset = (new BigDecimal("0.7")).multiply(PhysicsTools.EV); //from https://aip.scitation.org/doi/abs/10.1063/1.125965, make it into PhysicalTools as a new enum, Metamaterials
        BigDecimal energyPlaneElectron = (energyParameter(equivalentSquareSide, CBOffset, QDMaterial.getElectronEffectiveMassSI()).divide(equivalentSquareSide, MathContext.DECIMAL128)).pow(2);
        BigDecimal energyHeightElectron = (energyParameter(p_height, CBOffset, QDMaterial.getElectronEffectiveMassSI()).divide(p_height, MathContext.DECIMAL128)).pow(2);
        BigDecimal electronConfinementEnergy = (two.multiply(PhysicsTools.hbar.pow(2)).divide(QDMaterial.getElectronEffectiveMassSI(), MathContext.DECIMAL128)).multiply((energyPlaneElectron.multiply(two)).add(energyHeightElectron));
//        BigDecimal electronOscillatorPlane = BigDecimalMath.sqrt(eight.multiply(CBOffset).divide(QDMaterial.getElectronEffectiveMassSI().multiply(m_radius.pow(2)), MathContext.DECIMAL128), MathContext.DECIMAL128);
//        BigDecimal electronOscillatorHeight = BigDecimalMath.sqrt(eight.multiply(CBOffset).divide(QDMaterial.getElectronEffectiveMassSI().multiply(height.pow(2)), MathContext.DECIMAL128), MathContext.DECIMAL128);
//        BigDecimal electronConfinementEnergy = PhysicsTools.hbar.multiply(electronOscillatorPlane.add(electronOscillatorHeight)).divide(two);

        BigDecimal VBOffset = hostMaterial.getBaseBandgapSI().subtract(QDMaterial.getBaseBandgapSI()).subtract(CBOffset);
        BigDecimal energyPlaneHole = (energyParameter(equivalentSquareSide, VBOffset, QDMaterial.getHoleEffectiveMassSI()).divide(equivalentSquareSide, MathContext.DECIMAL128)).pow(2);
        BigDecimal energyHeightHole = (energyParameter(p_height, VBOffset, QDMaterial.getHoleEffectiveMassSI()).divide(p_height, MathContext.DECIMAL128)).pow(2);
        BigDecimal holeConfinementEnergy = (two.multiply(PhysicsTools.hbar.pow(2)).divide(QDMaterial.getHoleEffectiveMassSI(), MathContext.DECIMAL128)).multiply((energyPlaneHole.multiply(two)).add(energyHeightHole));
//        BigDecimal holeOscillatorPlane = BigDecimalMath.sqrt(eight.multiply(VBOffset).divide(QDMaterial.getHoleEffectiveMassSI().multiply(m_radius.pow(2)), MathContext.DECIMAL128), MathContext.DECIMAL128);
//        BigDecimal holeOscillatorHeight = BigDecimalMath.sqrt(eight.multiply(VBOffset).divide(QDMaterial.getHoleEffectiveMassSI().multiply(p_height.pow(2)), MathContext.DECIMAL128), MathContext.DECIMAL128);
//        BigDecimal holeConfinementEnergy = PhysicsTools.hbar.multiply(holeOscillatorPlane.add(holeOscillatorHeight)).divide(two);

        m_energy = QDMaterial.getBaseBandgapSI().add(electronConfinementEnergy).add(holeConfinementEnergy);
        
        BigDecimal minPhononEnergy = CBOffset.subtract(electronConfinementEnergy);
        if (minPhononEnergy.compareTo(BigDecimal.ZERO) <= 0)
        {
            //if the first QD energy level is higher than barrier conduction band, the QD cannot confine the carrier, and thus the capture probability is null
            minPhononEnergy = BigDecimal.ZERO;
            m_captureProba = 0;
        }
        else
        {
            //else, the capture probability is calculated using P_capture = 1 - exp(-Δt/tau_capture), with tau_capture given in https://aip.scitation.org/doi/10.1063/1.1512694
            m_captureProba = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide(p_captureTimes.getValueAtPosition(m_radius), MathContext.DECIMAL128)))).doubleValue();
        }
        
        //the escape probability is calculated using P_capture = 1 - exp(-Δt/tau_escape) with tau_escape from https://aip.scitation.org/doi/10.1063/1.4824469
        m_escapeProbability = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide(p_escapeTimes.getValueAtPosition(m_radius), MathContext.DECIMAL128)))).doubleValue();
        
        BigDecimal minPhotonEnergy = hostMaterial.getBaseBandgapSI().add(minPhononEnergy);
        
        m_recombinationProbability = (BigDecimal.ONE.subtract(BigDecimalMath.exp(p_timeStep.negate().divide((new BigDecimal("3.6")).multiply(PhysicsTools.UnitsPrefix.NANO.getMultiplier()), MathContext.DECIMAL128)))).doubleValue();
    }
    
    synchronized public boolean canCapture()
    {
        return m_captureProba != 0;
    }
    
    /**
     * The capture probability depends on many parameters and demand to be further investigate.
     * At the moment, the probability is calculated by the probability for the electron to reach the QDs multiplied by a capture probability based theoretical calculation (to be implemented)
     * At the moment, it is approximated as the overlapping between the QD and the circle containing the positions the electron can reach
     * See here for the calculation of the overlap: https://www.xarg.org/2016/07/calculate-the-intersection-area-of-two-circles/
     * It had to be slightly adapted with four different cases:
     *  - when the electron is entirely inside the QD (electronDistance + electronSpan < radius)
     *  - when the QD is entirely "inside" the electron (electronDistance + radius < electronSpan)
     *  - when the center of the QD is farther away from the electron position than the intersection between the QD limit and the electron span limit (electronDistance > sqrt(abs(radius^2 - electronSpan^2)))
     *  - when the center of the QD is closer from the electron position than the intersection between the QD limit and the electron span limit (electronDistance < sqrt(abs(radius^2 - electronSpan^2)))
     * @param p_RNG the random number generator
     * @param electronDistance the distance between the center of the QD and electron position
     * @param electronSpan the circle containing the position the electron can reach
     * @return whether the electron has been captured or not
     */
    synchronized public boolean capture(PcgRSFast p_RNG, BigDecimal electronDistance, BigDecimal electronSpan)
    {
        double reachingProbability = 0;
        
        /** calculating the probability of the electron to reach the QD
        *   This is done by calculating how many of the position the electron can reach are occupied by the QD
        *   case if the electron is entirely inside the QD
        */
        if (electronDistance.add(electronSpan).compareTo(m_radius) <= 0)
        {
            reachingProbability = 1;
        }
        else
        {
            //if the QD is entirely in the electron span
            if (electronDistance.add(m_radius).compareTo(electronSpan) <= 0)
            {
                reachingProbability = (m_radius.pow(2).divide(electronSpan.pow(2), MathContext.DECIMAL128)).doubleValue();
            }
            else
            {
                BigDecimal overlapArea = BigDecimal.ZERO;

                //we compare the distance to sqrt(abs(radius^2 - electronSpan^2))
                BigDecimal radiusDiff = BigDecimalMath.sqrt(((m_radius.pow(2)).subtract(electronSpan.pow(2))).abs(), MathContext.DECIMAL128);
                
                //if the QD center is farther away than the intersection points
                if (electronDistance.compareTo(radiusDiff) >= 0)
                {
                    //the base of the triangle for the calculation here is (electronSpan^2 + electronDistance^2 - QDradius^2) / (2 * electronDistance)
                    BigDecimal triangleBase = (electronSpan.pow(2).add(electronDistance.pow(2)).subtract(m_radius.pow(2))).divide(electronDistance.multiply(new BigDecimal("2")), MathContext.DECIMAL128);
                    
                    BigDecimal electronSlice = electronSpan.pow(2).multiply(BigDecimalMath.acos(triangleBase.divide(electronSpan, MathContext.DECIMAL128)));
                    BigDecimal QDSlice = m_radius.pow(2).multiply(BigDecimalMath.acos((electronDistance.subtract(triangleBase)).divide(m_radius, MathContext.DECIMAL128)));
                    BigDecimal triangleCorrection = electronDistance.multiply(BigDecimalMath.sqrt(electronSpan.pow(2).subtract(triangleBase.pow(2)), MathContext.DECIMAL128));
                    
                    overlapArea = electronSlice.add(QDSlice).subtract(triangleCorrection);
                }
                else
                {
                    //the base of the triangle for the calculation here is (electronSpan^2 - electronDistance^2 - QDradius^2) / (2 * electronDistance)
                    BigDecimal triangleBase = (electronSpan.pow(2).subtract(electronDistance.pow(2)).subtract(m_radius.pow(2))).divide(electronDistance.multiply(new BigDecimal("2")), MathContext.DECIMAL128);
                    
                    BigDecimal electronSlice = electronSpan.pow(2).multiply(BigDecimalMath.acos((triangleBase.add(electronDistance)).divide(electronSpan, MathContext.DECIMAL128)));
                    BigDecimal QDSlice = m_radius.pow(2).multiply(BigDecimalMath.pi(MathContext.DECIMAL128).subtract(BigDecimalMath.acos(triangleBase.divide(m_radius, MathContext.DECIMAL128))));
                    BigDecimal triangleCorrection = electronDistance.multiply(BigDecimalMath.sqrt(m_radius.pow(2).subtract(triangleBase.pow(2)), MathContext.DECIMAL128));
                    
                    overlapArea = electronSlice.add(QDSlice).subtract(triangleCorrection);
                }
                
                reachingProbability = overlapArea.divide(BigDecimalMath.pi(MathContext.DECIMAL128).multiply(electronSpan.pow(2)), MathContext.DECIMAL128).doubleValue();
            }
        }
        
        if (reachingProbability < 0 || reachingProbability > 1)
        {
            System.out.println("Probability has to be bound between 0 and 1");
            Logger.getLogger(QuantumDot.class.getName()).log(Level.SEVERE, null, new ArithmeticException("Probability has to be bound between 0 and 1"));
        }
        
        //the complete capture probability is the probability to reach the QD multiplied by the probability to be captured
        return p_RNG.nextDouble() < reachingProbability * m_captureProba;
    }
    
    public QuantumDot copyWithSizeChange(BigDecimal p_sizeMultiplier, BigDecimal p_timeStep, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes)
    {
        BigDecimal newRadius = m_radius;
        BigDecimal newHeight = m_height;
        
        if (m_radius.compareTo(m_height) > 0)
        {
            newHeight = newHeight.multiply(p_sizeMultiplier);
        }
        else
        {
            newRadius = newRadius.multiply(p_sizeMultiplier);
        }
        
        return new QuantumDot(m_positionX, m_positionY, newRadius, newHeight, p_timeStep, p_captureTimes, p_escapeTimes);
    }
    
    /**
     * See https://en.wikipedia.org/wiki/Finite_potential_well
     * Find v_0 using Newton's Method with the equation sqrt(u0^2 - v0^2) = v0*tan(v0)
     * Solved the equation squared in order to avoid the sqrt, since we want 0 < v0 < pi/2 anyway
     * @param size
     * @return 
     */
    private BigDecimal energyParameter (BigDecimal size, BigDecimal bandOffset, BigDecimal effectiveMass)
    {
        double u02 = (effectiveMass.multiply(size.pow(2)).multiply(bandOffset).divide((new BigDecimal(2)).multiply(PhysicsTools.hbar.pow(2)), MathContext.DECIMAL128)).doubleValue();
        
        //vtan have two constraints: it has to be between 0 and PI/2, and u02 - vtan^2 have to be positive
        double vtan = Math.random()*Double.min(Math.PI/2, Math.sqrt(u02));
        double error = 1E-50;

        double vprevtan = 0;
        int counter = 0;
        do
        {
            vprevtan = vtan;
            double tangent = Math.tan(vtan);
            
            double fvi = functionToOptimize(vtan) - u02;
            double fderivvi = 2 * vtan * (1 + (vtan * tangent / (Math.pow(Math.cos(vtan), 2))) + Math.pow(tangent, 2));
            
            vtan = Math.abs(vprevtan - (fvi / fderivvi));
            while (vtan >= Math.PI/2)
            {
                //vi has to be between 0 and pi/2
                vtan = vtan - Math.PI/2;
            }
            
            counter += 1;
        }while(counter <= 100 && Math.abs(functionToOptimize(vtan) - u02) >= error);
        
        return new BigDecimal(vtan);
    }
    
    private double functionToOptimize(double v)
    {
        return Math.pow(v, 2) * (1 + Math.pow(Math.tan(v), 2));
    }

    //will calculate probability based on phonon density
    synchronized public boolean escape(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_escapeProbability;
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
            m_recombined = p_RNG.nextDouble() < m_recombinationProbability;
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
        return m_positionX + "\t" + m_positionY + "\t" + m_radius + "\t" + m_height + "\t" + m_energy;
    }
}
