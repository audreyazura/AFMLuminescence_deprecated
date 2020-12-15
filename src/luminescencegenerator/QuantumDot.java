/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package luminescencegenerator;

import java.math.BigDecimal;

/**
 *
 * @author audreyazura
 */
public class QuantumDot extends AbsorberObject
{
    private final BigDecimal m_energy;
    private final BigDecimal m_captureProbability;
    
    public QuantumDot (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_size)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        
        //to be calculated later
        m_energy = p_size;
        m_captureProbability = p_size;
    }
}
