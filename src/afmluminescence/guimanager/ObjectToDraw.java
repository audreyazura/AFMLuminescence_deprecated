/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.guimanager;

import java.math.BigDecimal;

/**
 *
 * @author audreyazura
 */
public class ObjectToDraw
{
    private final BigDecimal m_xPosition;
    private final BigDecimal m_yPosition;
    private final BigDecimal m_radius;
    private final AbsorberObjectType m_type;
    
    public ObjectToDraw (BigDecimal x, BigDecimal y, BigDecimal radius, AbsorberObjectType type)
    {
        m_xPosition = x;
        m_yPosition = y;
        m_radius = radius;
        m_type = type;
    }
    
    public BigDecimal getX()
    {
        return m_xPosition;
    }
    
    public BigDecimal getY()
    {
        return m_yPosition;
    }
    
    public BigDecimal getRadius()
    {
        return m_radius;
    }
    
    public boolean isElectron()
    {
        return m_type == AbsorberObjectType.Electron;
    }
    
    enum AbsorberObjectType
    {
        QD, Electron;
    }
}
