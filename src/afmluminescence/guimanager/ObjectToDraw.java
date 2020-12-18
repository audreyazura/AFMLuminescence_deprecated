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
    private final double m_radius;
    
    public ObjectToDraw (BigDecimal x, BigDecimal y, double radius)
    {
        m_xPosition = x;
        m_yPosition = y;
        m_radius = radius;
    }
    
    public BigDecimal getX()
    {
        return m_xPosition;
    }
    
    public BigDecimal getY()
    {
        return m_yPosition;
    }
    
    public double getRadius()
    {
        return m_radius;
    }
}
