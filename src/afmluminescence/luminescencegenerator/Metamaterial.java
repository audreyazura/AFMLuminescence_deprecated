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

import afmluminescence.luminescencegenerator.Material;
import com.github.audreyazura.commonutils.PhysicsTools;
import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Alban Lafuente
 */
public class Metamaterial
{
    private final Map<String, Material> m_materials = new HashMap<>();
    private final Map<String, BigDecimal> m_CBOffsets = new HashMap<>();
    
    public Metamaterial (Properties p_metamaterial, Map<String, Material> p_materials)
    {
        Set<String> definedParameters = p_metamaterial.stringPropertyNames();
        List<String> materialParameters = new ArrayList<>();
        List<String> offsetParameters = new ArrayList<>();
        Pattern materialString = Pattern.compile(".*material.*");
        Pattern offsetString = Pattern.compile(".*offset.*");
        
        for (String parameter: definedParameters)
        {
            if (materialString.matcher(parameter).matches())
            {
                materialParameters.add(parameter);
                String materialID = parameter.split("_")[1];
                m_materials.put(materialID, p_materials.get(p_metamaterial.getProperty(parameter)));
            }
            else
            {
                if (offsetString.matcher(parameter).matches())
                {
                    offsetParameters.add(parameter);
                }
            }
        }
        
        if (materialParameters.size() < 1)
        {
            Logger.getLogger(Metamaterial.class.getName()).log(Level.SEVERE, "No material in your metamaterial", new InvalidParameterException());
        }
        if (offsetParameters.size() < materialParameters.size() - 1)
        {
            Logger.getLogger(Metamaterial.class.getName()).log(Level.SEVERE, "Not enough offsets defined", new InvalidParameterException());
        }
        
        List<String> possibleMaterialCombinations = new ArrayList<>();
        for (String materialParameter1: materialParameters)
        {
            for (String materialParameter2: materialParameters)
            {
                possibleMaterialCombinations.add(p_metamaterial.getProperty(materialParameter1) + p_metamaterial.getProperty(materialParameter2));
            }
        }
        
        for (String parameter: offsetParameters)
        {
            String materials = parameter.split("_")[1];
            
            if (possibleMaterialCombinations.contains(materials))
            {
                m_CBOffsets.put(materials, (new BigDecimal(p_metamaterial.getProperty(parameter))).multiply(PhysicsTools.EV));
            }
            else
            {
                Logger.getLogger(Metamaterial.class.getName()).log(Level.SEVERE, "The offset key should start with the two materials concerned by the offset", new InvalidParameterException());
            }
        }
    }
    
    private Metamaterial (HashMap<String, Material> p_materials, HashMap<String, BigDecimal> p_offsets)
    {
        for (String key: p_materials.keySet())
        {
            m_materials.put(key, p_materials.get(key));
        }
        
        for (String key: p_offsets.keySet())
        {
            m_CBOffsets.put(key, p_offsets.get(key));
        }
    }
    
    public Metamaterial copy()
    {
        return new Metamaterial((HashMap<String, Material>) m_materials, (HashMap<String, BigDecimal>) m_CBOffsets);
    }
    
    public Material getMaterial (String materialID)
    {
        return m_materials.get(materialID).copy();
    }
    
    public BigDecimal getOffset (String material1, String material2) throws InvalidParameterException
    {
        String compound = material1 + material2;
        if (!m_CBOffsets.containsKey(compound))
        {
            compound = material2 + material1;
            
            if (!m_CBOffsets.containsKey(compound))
            {
                throw new InvalidParameterException("These two materials offset are not defined in this metamaterial.");
            }
        }
        
        return m_CBOffsets.get(compound);
    }
}
