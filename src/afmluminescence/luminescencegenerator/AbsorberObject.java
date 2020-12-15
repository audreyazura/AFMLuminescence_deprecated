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
        BigDecimal two = new BigDecimal("2");
        
        return BigDecimalMath.sqrt(BigDecimalMath.pow(m_positionX.subtract(p_positionX), two).add(BigDecimalMath.pow(m_positionY.subtract(p_positionY), two)));
    }
}
