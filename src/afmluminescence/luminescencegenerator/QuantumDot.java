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
    private boolean m_recombined = false;
    
    //ΔEg(InAs/GaAs) = 1.1 eV
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        //to be calculated later
        m_radius = p_radius;
        m_energy = (new BigDecimal("0.354")).multiply(PhysicsTools.EV);
        m_captureProbability = 0.01;
    }
    
    synchronized public boolean capture(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_captureProbability;
    }
    
    //will calculate probability based on phonon density
    synchronized public boolean escape(PcgRSFast p_RNG)
    {
        return p_RNG.nextDouble() < m_captureProbability;
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
        boolean didIt = p_RNG.nextDouble() < m_captureProbability;
        
        if (!m_recombined)
        {
            m_recombined = didIt;
        }
        
        return didIt;
    }
    
    public void resetRecombine()
    {
        m_recombined = false;
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; size = " + m_radius;
    }
}
