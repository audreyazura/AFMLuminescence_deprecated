/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;

/**
 *
 * @author audreyazura
 */
public class QuantumDot extends AbsorberObject
{
    private final double m_captureProbability;
    private final BigDecimal m_energy;
    private final BigDecimal m_radius;
    
    //ΔEg(InAs/GaAs) = 1.1 eV
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        //to be calculated later
        m_radius = p_radius;
        m_energy = (new BigDecimal("0.354")).multiply(PhysicsTools.EV);
        m_captureProbability = 0.001;
    }
    
    public boolean capture(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_captureProbability;
    }
    
    public boolean escape(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_captureProbability;
    }
    
    public BigDecimal getRadius()
    {
        return m_radius;
    }
    
    //will calculate the probablity based on the electron and hole wave function
    public boolean recombine(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_captureProbability;
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; size = " + m_radius;
    }
}
