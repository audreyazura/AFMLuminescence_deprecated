/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import java.math.BigDecimal;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author audreyazura
 */
public class AbsorberObject
{
    BigDecimal m_positionX;
    BigDecimal m_positionY;
    
    public BigDecimal getDistance (BigDecimal p_positionX, BigDecimal p_positionY)
    {
        return BigDecimalMath.sqrt(((m_positionX.subtract(p_positionX)).pow(2)).add(((m_positionY.subtract(p_positionY)).pow(2))));
    }
    
    public BigDecimal getX()
    {
        return m_positionX;
    }
    
    public BigDecimal getY()
    {
        return m_positionY;
    }
}
