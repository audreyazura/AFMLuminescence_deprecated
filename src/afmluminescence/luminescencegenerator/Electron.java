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
public class Electron extends AbsorberObject
{
    private final BigDecimal m_speedX;
    private final BigDecimal m_speedY;
    
    private ElectronState m_state = ElectronState.FREE;
    private QuantumDot m_trapingDot = null;
    
    public Electron (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_speedX, BigDecimal p_speedY)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        m_speedX = p_speedX;
        m_speedY = p_speedY;
    }
    
    public void stepInTime(BigDecimal p_timeStep, BigDecimal p_maxX, BigDecimal p_maxY)
    {
        m_positionX = m_positionX.add(m_speedX.multiply(p_timeStep));
        if (m_positionX.compareTo(BigDecimal.ZERO) < 0)
        {
            m_positionX = p_maxX.add(m_positionX);
        }
        else if (m_positionX.compareTo(p_maxX) > 0)
        {
            m_positionX = m_positionX.subtract(p_maxX);
        }
        
        m_positionY = m_positionY.add(m_speedY.multiply(p_timeStep));
        if (m_positionY.compareTo(BigDecimal.ZERO) < 0)
        {
            m_positionY = p_maxX.add(m_positionY);
        }
        else if (m_positionY.compareTo(p_maxX) > 0)
        {
            m_positionY = m_positionY.subtract(p_maxX);
        }
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; v_x = " + m_speedX + " ; v_y = " + m_speedY + ")";
    }
    
    enum ElectronState
    {
        CAPTURED, FREE;
    }
}
