package com.jonathanblood.restaurant.utils;

/**
 * Created by Jonat on 04/06/2016.
 */
public class StringUtils
{

    /**
     * Null safe is empty check
     * @param value
     * @return true if the value is empty or null, else true
     */
    public static boolean isEmpty(String value)
    {
        if (value == null || value.isEmpty())
            return true;

        return false;
    }
}
