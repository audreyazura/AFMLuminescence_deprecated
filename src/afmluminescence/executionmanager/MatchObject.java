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
package afmluminescence.executionmanager;

/**
 *
 * @author audreyazura
 */
public class MatchObject
{
    private final boolean m_matching;
    private final String m_comment;

    public MatchObject(boolean p_match, String p_comment)
    {
        m_matching = p_match;
        m_comment = p_comment;
    }

    @Override
    public String toString()
    {
        String returnString;

        if (m_matching)
        {
            returnString = "Matching";
        }
        else
        {
            returnString = "Not matching, " + m_comment;
        }

        return returnString;
    }

    public boolean isMatching()
    {
        return m_matching;
    }

    public String comment()
    {
        return m_comment;
    }
}
