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
