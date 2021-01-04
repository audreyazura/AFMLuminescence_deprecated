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

/**
 *
 * @author Alban Lafuente
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
