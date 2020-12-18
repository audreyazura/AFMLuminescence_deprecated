/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import java.math.BigDecimal;

/**
 *
 * @author audreyazura
 */
public class QuantumDot extends AbsorberObject
{
    private final BigDecimal m_captureProbability;
    private final BigDecimal m_energy;
    private final BigDecimal m_radius;
    
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_radius)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        //to be calculated later
        m_radius = p_radius;
        m_energy = p_radius;
        m_captureProbability = p_radius;
    }
    
    public BigDecimal getRadius()
    {
        return m_radius;
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; size = " + m_radius;
    }
}
