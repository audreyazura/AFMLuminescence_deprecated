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
public class Electron extends AbsorberObject
{
    private final BigDecimal m_speedX;
    private final BigDecimal m_speedY;
    
    private ElectronState m_state = ElectronState.FREE;
    
    public Electron (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_speedX, BigDecimal p_speedY)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        m_speedX = p_speedX;
        m_speedY = p_speedY;
    }
    
    enum ElectronState
    {
        CAPTURED, FREE;
    }
}
