/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package afmluminescence.luminescencegenerator;

import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import java.math.BigDecimal;
import java.util.List;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author audreyazura
 */
public class Electron extends AbsorberObject
{
    private BigDecimal m_speedX;
    private BigDecimal m_speedY;
    
    private ElectronState m_state = ElectronState.FREE;
    private QuantumDot m_trapingDot = null;
    
    public Electron (BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_speedX, BigDecimal p_speedY)
    {
        m_positionX = p_positionX;
        m_positionY = p_positionY;
        m_speedX = p_speedX;
        m_speedY = p_speedY;
    }
    
    public boolean isFree()
    {
        return m_state == ElectronState.FREE;
    }
    
    public boolean isRecombined()
    {
        return m_state == ElectronState.RECOMBINED;
    }
    
    public void stepInTime(BigDecimal p_timeStep, BigDecimal p_maxX, BigDecimal p_maxY, BigDecimal p_vth, List<QuantumDot> p_sampleQDs, PcgRSFast p_RNG)
    {
        /**
         * moving the electron if it hasn't been captured or hasn't recombined
         * if it has been captured, it can either recombine or escape
        **/
        if (!(m_state == ElectronState.RECOMBINED))
        {
            if (m_state == ElectronState.FREE)
            {
                BigDecimal deltaX = m_speedX.multiply(p_timeStep);
                BigDecimal deltaY = m_speedY.multiply(p_timeStep);
                
                BigDecimal electronVision = BigDecimalMath.sqrt(deltaX.pow(2).add(deltaY.pow(2)));
                for (QuantumDot QD: p_sampleQDs)
                {
                    if ((getDistance(QD.getX(), QD.getY()).subtract(QD.getRadius())).compareTo(electronVision) <= 0)
                    {
                        if (QD.capture(p_RNG))
                        {
                            m_state = ElectronState.CAPTURED;
                            m_trapingDot = QD;
                            break;
                        }
                    }
                }
                
                if (m_state == ElectronState.FREE)
                {
                    m_positionX = m_positionX.add(deltaX);
                    if (m_positionX.compareTo(BigDecimal.ZERO) < 0)
                    {
                        m_positionX = p_maxX.add(m_positionX);
                    }
                    else if (m_positionX.compareTo(p_maxX) > 0)
                    {
                        m_positionX = m_positionX.subtract(p_maxX);
                    }

                    m_positionY = m_positionY.add(deltaY);
                    if (m_positionY.compareTo(BigDecimal.ZERO) < 0)
                    {
                        m_positionY = p_maxX.add(m_positionY);
                    }
                    else if (m_positionY.compareTo(p_maxX) > 0)
                    {
                        m_positionY = m_positionY.subtract(p_maxX);
                    }
                }
            }
            else
            {
                if (m_trapingDot.escape(p_RNG))
                {
                    m_state = ElectronState.FREE;
                    m_positionX = m_trapingDot.getX();
                    m_positionY = m_trapingDot.getY();
                    m_speedX = GeneratorManager.formatBigDecimal((new BigDecimal(p_RNG.nextGaussian())).multiply(p_vth));
                    m_speedY = GeneratorManager.formatBigDecimal((new BigDecimal(p_RNG.nextGaussian())).multiply(p_vth));
                    m_trapingDot = null;
                }
                else
                {
                    if (m_trapingDot.recombine(p_RNG))
                    {
                        m_state = ElectronState.RECOMBINED;
                    }
                }
            }
        }
    }
    
    @Override
    public String toString()
    {
        return "(x = " + m_positionX + " ; y = " + m_positionY + " ; v_x = " + m_speedX + " ; v_y = " + m_speedY + ")";
    }
    
    enum ElectronState
    {
        CAPTURED, FREE, RECOMBINED;
    }
}
