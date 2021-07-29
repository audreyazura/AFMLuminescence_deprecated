/*
 * Copyright (C) 2021 audreyazura
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

import afmluminescence.executionmanager.SCSVLoader;
import com.github.audreyazura.commonutils.ContinuousFunction;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

/**
 *
 * @author Alban Lafuente
 */
public class Material
{
    private final BigDecimal m_bandgap;
    private final BigDecimal m_electronEffectiveMass;
    private final BigDecimal m_holeEffectiveMass;
    private final ContinuousFunction m_captureTimes;
    private final ContinuousFunction m_escapeTimes;
    private final ContinuousFunction m_recombinationTimes;
    private final String m_name;
    
    public Material (Properties p_materialProperty)
    {
        Set<String> definedParameters = p_materialProperty.stringPropertyNames();
        
        //getting the keys for the different times
        Pattern recombinationTimePattern = Pattern.compile("recombinationtimes_?.*");
        Pattern captureTimePattern = Pattern.compile("capturetimes_?.*");
        Pattern escapeTimePattern = Pattern.compile("escapetimes_?.*");
        String recombinationTimesKey = "";
        String captureTimesKey = "";
        String escapeTimesKey = "";
        for (String key: definedParameters)
        {
            if (recombinationTimePattern.matcher(key).matches())
            {
                recombinationTimesKey = key;
            }
            else
            {
                if (captureTimePattern.matcher(key).matches())
                {
                    captureTimesKey = key;
                }
                else if (escapeTimePattern.matcher(key).matches())
                {
                    escapeTimesKey = key;
                }
            }
        }
        
        //testing if all necessary keys are defined
        if (!definedParameters.contains("name"))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "name property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(!definedParameters.contains("bandgap"))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "bandgap property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(!definedParameters.contains("effectiveMass_electron"))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "electron effective mass property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(!definedParameters.contains("effectiveMass_hole"))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "hole effective mass property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(captureTimesKey.equals(""))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "capturetimes property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(escapeTimesKey.equals(""))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "escapetimes property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        if(recombinationTimesKey.equals(""))
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "recombinationtimes property missing in the file defining the material " + p_materialProperty.getProperty("name"), new InvalidParameterException());
        }
        
        m_name = p_materialProperty.getProperty("name");
        
        m_bandgap = (new BigDecimal(p_materialProperty.getProperty("bandgap"))).multiply(PhysicsTools.EV);
        m_electronEffectiveMass = (new BigDecimal(p_materialProperty.getProperty("effectiveMass_electron"))).multiply(PhysicsTools.ME);
        m_holeEffectiveMass = (new BigDecimal(p_materialProperty.getProperty("effectiveMass_hole"))).multiply(PhysicsTools.ME);
        
        m_captureTimes = getTimeFunction(captureTimesKey, p_materialProperty.getProperty(captureTimesKey), p_materialProperty.getProperty("name"));
        m_escapeTimes = getTimeFunction(escapeTimesKey, p_materialProperty.getProperty(escapeTimesKey), p_materialProperty.getProperty("name"));
        m_recombinationTimes = getTimeFunction(recombinationTimesKey, p_materialProperty.getProperty(recombinationTimesKey), p_materialProperty.getProperty("name"));
    }
    
    private Material (BigDecimal p_bandgap, BigDecimal p_electronEffectiveMass, BigDecimal p_holeEffectiveMass, ContinuousFunction p_captureTimes, ContinuousFunction p_escapeTimes, ContinuousFunction p_recombinationTimes, String p_name)
    {
        m_bandgap = new BigDecimal(p_bandgap.toString());
        m_electronEffectiveMass = new BigDecimal(p_electronEffectiveMass.toString());
        m_holeEffectiveMass = new BigDecimal(p_holeEffectiveMass.toString());
        m_captureTimes = new ContinuousFunction(p_captureTimes);
        m_escapeTimes = new ContinuousFunction(p_escapeTimes);
        m_recombinationTimes = new ContinuousFunction(p_recombinationTimes);
        m_name = p_name;
    }
    
    private ContinuousFunction getTimeFunction (String functionKey, String functionValue, String material)
    {
        ContinuousFunction tempFunction = new ContinuousFunction();
        
        try
        {
            String[] functionKeySplit = functionKey.split("_");
            String functionType = functionKeySplit[0];
            if (functionKeySplit.length == 4 && functionKeySplit[1].equals("file"))
            {
                PhysicsTools.UnitsPrefix abscissaUnit = PhysicsTools.UnitsPrefix.selectPrefix(functionKeySplit[2]);
                PhysicsTools.UnitsPrefix ordinateUnit = PhysicsTools.UnitsPrefix.selectPrefix(functionKeySplit[3]);
                tempFunction = (new SCSVLoader(new File("ressources/" + functionType + "/" + functionValue), abscissaUnit, ordinateUnit)).getFunction();
            }
            else
            {
                if (functionKeySplit.length == 1 || functionKeySplit.length == 2)
                {
                    BigDecimal constantRecombinationTime;
                    if (functionValue.equals("") || functionKeySplit.length == 1)
                    {
                        constantRecombinationTime = BigDecimal.ZERO;
                    }
                    else
                    {
                        PhysicsTools.UnitsPrefix recombinationTimeUnit = PhysicsTools.UnitsPrefix.selectPrefix(functionKeySplit[1]);
                        constantRecombinationTime = (new BigDecimal(functionValue)).multiply(recombinationTimeUnit.getMultiplier());
                    }
                    HashMap<BigDecimal, BigDecimal> constantFunction = new HashMap<>();
                    constantFunction.put(BigDecimal.ZERO, constantRecombinationTime);
                    constantFunction.put(BigDecimal.ONE, constantRecombinationTime);
                    tempFunction = new ContinuousFunction(constantFunction);
                }
                else
                {
                    Logger.getLogger(Material.class.getName()).log(Level.SEVERE, "Problem in the definition of recombination time in " + material, new InvalidParameterException());
                }
            }
        }
        catch (DataFormatException|IOException ex)
        {
            Logger.getLogger(Material.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return tempFunction;
    }
    
    public Material copy()
    {
        return new Material(m_bandgap, m_electronEffectiveMass, m_holeEffectiveMass, m_captureTimes, m_escapeTimes, m_recombinationTimes, m_name);
    }
    
    public BigDecimal getBandgap()
    {
        return new BigDecimal(m_bandgap.toString());
    }
    
    public BigDecimal getCaptureTime (BigDecimal p_QDSize)
    {
        return m_captureTimes.getValueAtPosition(p_QDSize);
    }
    
    public BigDecimal getElectronEffectiveMass()
    {
        return new BigDecimal(m_electronEffectiveMass.toString());
    }
    
    public BigDecimal getEscapeTime (BigDecimal p_QDSize)
    {
        return m_escapeTimes.getValueAtPosition(p_QDSize);
    }
    
    public BigDecimal getHoleEffectiveMass()
    {
        return new BigDecimal(m_holeEffectiveMass.toString());
    }
    
    public String getMaterialName()
    {
        return m_name;
    }
    
    public BigDecimal getRecombinationTime (BigDecimal p_QDSize)
    {
        return m_recombinationTimes.getValueAtPosition(p_QDSize);
    }
}
