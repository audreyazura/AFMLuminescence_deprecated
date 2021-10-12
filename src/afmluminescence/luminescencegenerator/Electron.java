/*
 * Copyright (C) 2020-2021 Alban Lafuente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package afmluminescence.luminescencegenerator;

import com.github.audreyazura.commonutils.PhysicsTools;
import com.github.kilianB.pcg.fast.PcgRSFast;
import com.sun.jdi.AbsentInformationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.nevec.rjm.BigDecimalMath;

/**
 *
 * @author Alban Lafuente
 */
public class Electron extends AbsorberObject
{
    private final int m_id;
    
    private BigDecimal m_speedX;
    private BigDecimal m_speedY;
    
    private ElectronState m_state;
    private QuantumDot m_trapingDot;
    private BigDecimal m_recombinationEnergy = null;
    
    public Electron (int p_id, BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_speedX, BigDecimal p_speedY)
    {
        m_id = p_id;
        m_positionX = new BigDecimal(p_positionX.toString());
        m_positionY = new BigDecimal(p_positionY.toString());
        m_speedX = new BigDecimal(p_speedX.toString());
        m_speedY = new BigDecimal(p_speedY.toString());
        m_state = ElectronState.FREE;
        m_trapingDot = null;
    }
    
    public Electron (int p_id, BigDecimal p_positionX, BigDecimal p_positionY, BigDecimal p_speedX, BigDecimal p_speedY, ElectronState p_state, QuantumDot p_trapingDot)
    {
        m_id = p_id;
        m_positionX = new BigDecimal(p_positionX.toString());
        m_positionY = new BigDecimal(p_positionY.toString());
        m_speedX = new BigDecimal(p_speedX.toString());
        m_speedY = new BigDecimal(p_speedY.toString());
        m_state = p_state;
        m_trapingDot = p_trapingDot;
    }
    
    public Electron copy (int p_newId)
    {
        return new Electron(p_newId, new BigDecimal(m_positionX.toString()), new BigDecimal(m_positionY.toString()), new BigDecimal(m_speedX.toString()), new BigDecimal(m_speedY.toString()), m_state, m_trapingDot.copy());
    }
    
    public BigDecimal getRecombinationEnergy() throws AbsentInformationException
    {
        if (m_recombinationEnergy == null || m_recombinationEnergy.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new AbsentInformationException();
        }
        else
        {
            return m_recombinationEnergy;
        }
    }
    
    @Override
    public boolean equals (Object obj)
    {
        return getClass().equals(obj.getClass()) && hashCode() == obj.hashCode();
    }
    
    public boolean isFree()
    {
        return m_state == ElectronState.FREE;
    }
    
    public boolean isRecombined()
    {
        return m_state == ElectronState.RECOMBINED;
//        return true;
    }
    
    @Override
    public int hashCode()
    {
        return m_id;
    }
    
    public void move(BigDecimal p_timeStep, BigDecimal p_maxX, BigDecimal p_maxY, BigDecimal p_vth, HashMap<BigInteger, Set<QuantumDot>> p_map, PcgRSFast p_RNG)
    {
        /**
         * moving the electron if it hasn't been captured or hasn't recombined
         * if it has been captured, it can either recombine or escape
        **/
        if (!(m_state == ElectronState.RECOMBINED))
        {
            //if the electron is free, we see if it is captured
            if (m_state == ElectronState.FREE)
            {
                BigDecimal deltaX = m_speedX.multiply(p_timeStep);
                BigDecimal deltaY = m_speedY.multiply(p_timeStep);
                
                BigDecimal electronVision = BigDecimalMath.sqrt(deltaX.pow(2).add(deltaY.pow(2)));
                
                //finding QD in range in x and testing if they capture the electron
                BigDecimal scanStart = (m_positionX.subtract(electronVision)).scaleByPowerOfTen(PhysicsTools.UnitsPrefix.NANO.getScale());
                BigDecimal scanEnd = (m_positionX.add(electronVision)).scaleByPowerOfTen(PhysicsTools.UnitsPrefix.NANO.getScale());
                Set<QuantumDot> testedDots = new HashSet<>();
                for (BigDecimal iter = scanStart ; iter.compareTo(scanEnd) <= 0 && m_state == ElectronState.FREE ; iter = iter.add(BigDecimal.ONE))
                {
                    Set<QuantumDot> currentQDSet = p_map.get(iter.toBigInteger());
                    if (currentQDSet != null)
                    {
                        for (QuantumDot QD: currentQDSet)
                        {
                            if (!testedDots.contains(QD))
                            {
                                BigDecimal distance = getDistance(QD.getX(), QD.getY()).subtract(QD.getRadius());
                                if (distance.compareTo(electronVision) <= 0)
                                {
                                    if (QD.capture(p_RNG, distance, electronVision))
                                    {
                                        m_state = ElectronState.CAPTURED;
                                        m_trapingDot = QD;
                                        break;
                                    }
                                }
                                testedDots.add(QD);
                            }
                        }
                    }
                }
                
//                Set<QuantumDot> dotToTest = new HashSet<>();
//                for (BigDecimal iter = scanStart ; iter.compareTo(scanEnd) <= 0 ; iter = iter.add(BigDecimal.ONE))
//                {
//                    Set<QuantumDot> currentQDSet = p_map.get(iter.toBigInteger());
//                    if (currentQDSet != null)
//                    {
//                        for (QuantumDot QD: currentQDSet)
//                        {
//                            if (QD.canCapture())
//                            {
//                                dotToTest.add(QD);
//                            }
//                        }
//                    }
//                }
////                Set<QuantumDot> dotToTest = p_manager.dostAtRange(electronVision, scanStart, scanEnd);
//                for (QuantumDot QD: dotToTest)
//                {
//                    BigDecimal distance = getDistance(QD.getX(), QD.getY()).subtract(QD.getRadius());
//                    if (distance.compareTo(electronVision) <= 0)
//                    {
//                        if (QD.capture(p_RNG, distance, electronVision))
//                        {
//                            m_state = ElectronState.CAPTURED;
//                            m_trapingDot = QD;
//                            break;
//                        }
//                    }
//                }
                
                //if the electron has not been captured (still free), we move it
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
                if ((m_recombinationEnergy = m_trapingDot.recombine(p_RNG)).compareTo(BigDecimal.ZERO) >= 0)
                {
                    m_state = ElectronState.RECOMBINED;
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
