/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.parser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates Creole markp to JSPWiki markup. Simple translator uses regular expressions.
 * See http://www.wikicreole.org for the WikiCreole spec. 
 * 
 * This translator can be configured through properties defined in
 * jspwiki.properties starting with "creole.*". See the 
 * jspwiki.properties file for an explanation of the properties
 * 
 * @author Steffen Schramm 
 * @author Hanno Eichelberger 
 * @author Christoph Sauer
 * 
 * @see <a href="http://www.wikicreole.org/">Wiki Creole Spec</a>
 */
public class CreoleToJSPWikiTranslator
{

    // These variables are expanded so that admins
    // can display information about the current installed
    // pagefilter
    //
    // The syntax is the same as a wiki var. Unlike a wiki
    // war though, the CreoleTranslator itself
    //
    // [{$creolepagefilter.version}]
    // [{$creolepagefilter.creoleversion}]
    // [{$creolepagefilter.linebreak}] -> bloglike/wikilike

    public static String VAR_VERSION = "1.0.2";

    public static String VAR_CREOLE_VERSION = "1.0";

    public static String VAR_LINEBREAK_BLOGLIKE = "bloglike";

    public static String VAR_LINEBREAK_C2LIKE = "c2like";

    private static final String CREOLE_BOLD = "\\*\\*((?s:.)*?)(\\*\\*|(\n\n|\r\r|\r\n\r\n))";

    private static final String JSPWIKI_BOLD = "__$1__$3";

    private static final String CREOLE_ITALIC = "//((?s:.)*?)(//|(\n\n|\r\r|\r\n\r\n))";

    private static final String JSPWIKI_ITALIC = "''$1''$3";

    private static final String CREOLE_SIMPLELINK = "\\[\\[([^\\]]*?)\\]\\]";

    private static final String JSPWIKI_SIMPLELINK = "[$1]";

    private static final String CREOLE_LINK = "\\[\\[([^\\]]*?)\\|([^\\[\\]]*?)\\]\\]";

    private static final String JSPWIKI_LINK = "[$2|$1]";

    private static final String CREOLE_HEADER_0 = "(\n|\r|\r\n|^)=([^=\\r\\n]*)={0,2}";

    private static final String JSPWIKI_HEADER_0 = "$1!!!$2";

    private static final String CREOLE_HEADER_1 = "(\n|\r|\r\n|^)==([^=\\r\\n]*)={0,2}";

    private static final String JSPWIKI_HEADER_1 = "$1!!!$2";

    private static final String CREOLE_HEADER_2 = "(\n|\r|\r\n|^)===([^=\\r\\n]*)={0,3}";

    private static final String JSPWIKI_HEADER_2 = "$1!!$2";

    private static final String CREOLE_HEADER_3 = "(\n|\r|\r\n|^)====([^=\\r\\n]*)={0,4}";

    private static final String JSPWIKI_HEADER_3 = "$1!$2";

    private static final String CREOLE_HEADER_4 = "(\n|\r|\r\n|^)=====([^=\\r\\n]*)={0,5}";

    private static final String JSPWIKI_HEADER_4 = "$1__$2__";

    private static final String CREOLE_SIMPLEIMAGE = "\\{\\{([^\\}]*?)\\}\\}";

    private static final String JSPWIKI_SIMPLEIMAGE = "[{Image src='$1'}]";

    private static final String CREOLE_IMAGE = "\\{\\{([^\\}]*?)\\|([^\\}]*?)\\}\\}";

    private static final String JSPWIKI_IMAGE = "[{Image src='$1' caption='$2'}]";

    private static final String CREOLE_IMAGE_LINK = "\\[\\[(.*?)\\|\\{\\{(.*?)\\}\\}\\]\\]";

    private static final String JSPWIKI_IMAGE_LINK = "[{Image src='$2' link='$1'}]";

    private static final String CREOLE_IMAGE_LINK_DESC = "\\[\\[(.*?)\\|\\{\\{(.*?)\\|(.*?)\\}\\}\\]\\]";

    private static final String JSPWIKI_IMAGE_LINK_DESC = "[{Image src='$2' link='$1' caption='$3'}]";

    private static final String PREFORMATTED_PROTECTED = "\\Q{{{\\E.*?\\Q}}}\\E";

    private static final String CREOLE_LINEBREAKS = "([^\\s\\\\])(\r\n|\r|\n)+(?=[^\\s\\*#])";

    private static final String JSPWIKI_LINEBREAKS = "$1\\\\\\\\$2";

    private static final String CREOLE_TABLE = "(\n|\r|\r\n|^)(\\|[^\n\r]*)\\|(\\t| )*(\n|\r|\r\n|$)";

    private static final String CREOLE_PLUGIN = "\\<\\<((?s:.)*?)\\>\\>";

    private static final String JSPWIKI_PLUGIN = "[{$1}]";

    private static final String WWW_URL = "(\\[\\[)\\s*(www\\..*?)(\\]\\])";

    private static final String HTTP_URL = "$1http://$2$3";

    private static final String CREOLE_IMAGE_X = "\\{\\{(.*?)((\\|)(.*?)){0,1}((\\|)(.*?)){0,1}\\}\\}";

    private static final String JSPWIKI_IMAGE_X = "[{\u2016 src='$1' caption='$4' \u2015}]";

    private static final String CREOLE_LINK_IMAG_X = "\\[\\[(.*?)\\|\\{\\{(.*?)((\\|)(.*?)){0,1}((\\|)(.*?)){0,1}\\}\\}\\]\\]";

    private static final String JSPWIKI_LINK_IMAGE_X = "[{\u2016 src='$2' link='$1' caption='$5' \u2015}]";

    private static final String JSPWIKI_TABLE = "$1$2$4";

    /* TODO Is it possible to use just protect :// ? */
    private static final String URL_PROTECTED = "http://|ftp://|https://";

    private static final String TABLE_HEADER_PROTECTED = "((\n|\r|\r\n|^)(\\|.*?)(\n|\r|\r\n|$))";

    private static final String SIGNATURE = "--~~~";

    private static final String SIGNATURE_AND_DATE = "--~~~~";

    private static final String DEFAULT_DATEFORMAT = "yyyy-MM-dd";

    private static final String ESCAPE_PROTECTED = "~(\\*\\*|~|//|-|#|\\{\\{|}}|\\\\|~\\[~~[|]]|----|=|\\|)";

    private static final Map protectionMap = new HashMap();

    public ArrayList hashList = new ArrayList();

    public String translateSignature(Properties wikiProps, final String content, String username)
    {

        String dateFormat = wikiProps.getProperty("creole.dateFormat");

        if (dateFormat == null)
        {
            dateFormat = DEFAULT_DATEFORMAT;
        }

        SimpleDateFormat df = null;
        try
        {
            df = new SimpleDateFormat(dateFormat);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            df = new SimpleDateFormat(DEFAULT_DATEFORMAT);
        }

        String result = content;
        result = protectMarkup(result, PREFORMATTED_PROTECTED, "", "");
        result = protectMarkup(result, URL_PROTECTED, "", "");

        Calendar cal = Calendar.getInstance();
        result = translateElement(result, SIGNATURE_AND_DATE, "-- [[" + username + "]], " + df.format(cal.getTime()));
        result = translateElement(result, SIGNATURE, "-- [[" + username + "]]");
        result = unprotectMarkup(result);
        return result;
    }

    /** Translates Creole markup to JSPWiki markup */
    public String translate(Properties wikiProps, final String content)
    {
        String tmp = wikiProps.getProperty("creole.blogLineBreaks");
        boolean blogLineBreaks = false;
        if (tmp != null)
        {
            if (tmp.trim().equals("true"))
                blogLineBreaks = true;
        }

        String imagePlugin = wikiProps.getProperty("creole.imagePlugin.name");

        String result = content;
        result = result.replace("\r\n", "\n");
        result = result.replace("\r", "\n");

        /* Now protect the rest */
        result = protectMarkup(result);
        result = translateLists(result, "*", "-", "Nothing");
        result = translateElement(result, CREOLE_BOLD, JSPWIKI_BOLD);
        result = translateElement(result, CREOLE_ITALIC, JSPWIKI_ITALIC);
        result = translateElement(result, WWW_URL, HTTP_URL);

        if (imagePlugin != null && !imagePlugin.equals(""))
        {
            result = this.replaceImageArea(wikiProps, result, CREOLE_LINK_IMAG_X, JSPWIKI_LINK_IMAGE_X, 6, imagePlugin);
            result = this.replaceImageArea(wikiProps, result, CREOLE_IMAGE_X, JSPWIKI_IMAGE_X, 5, imagePlugin);
        }
        result = translateElement(result, CREOLE_IMAGE_LINK_DESC, JSPWIKI_IMAGE_LINK_DESC);
        result = translateElement(result, CREOLE_IMAGE_LINK, JSPWIKI_IMAGE_LINK);
        result = translateElement(result, CREOLE_LINK, JSPWIKI_LINK);
        result = translateElement(result, CREOLE_SIMPLELINK, JSPWIKI_SIMPLELINK);
        result = translateElement(result, CREOLE_HEADER_4, JSPWIKI_HEADER_4);
        result = translateElement(result, CREOLE_HEADER_3, JSPWIKI_HEADER_3);
        result = translateElement(result, CREOLE_HEADER_2, JSPWIKI_HEADER_2);
        result = translateElement(result, CREOLE_HEADER_1, JSPWIKI_HEADER_1);
        result = translateElement(result, CREOLE_HEADER_0, JSPWIKI_HEADER_0);
        result = translateElement(result, CREOLE_IMAGE, JSPWIKI_IMAGE);
        result = translateLists(result, "-", "*", "#");
        result = translateElement(result, CREOLE_SIMPLEIMAGE, JSPWIKI_SIMPLEIMAGE);
        result = translateElement(result, CREOLE_TABLE, JSPWIKI_TABLE);
        result = replaceArea(result, TABLE_HEADER_PROTECTED, "\\|=([^\\|]*)=|\\|=([^\\|]*)", "||$1$2");

        if (blogLineBreaks)
        {
            result = translateElement(result, CREOLE_LINEBREAKS, JSPWIKI_LINEBREAKS);
        }

        result = unprotectMarkup(result);

        result = translateVariables(result, blogLineBreaks);
        result = result.replace("\n", System.getProperty("line.separator"));
        return result;
    }

    /** Translates lists. */
    private static String translateLists(String content, String sourceSymbol, String targetSymbol, String sourceSymbol2)
    {
        String[] lines = content.split("\n");
        StringBuffer result = new StringBuffer();
        int counter = 0;
        int inList = -1;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            String actSourceSymbol = "";
            while ((line.startsWith(sourceSymbol) || line.startsWith(sourceSymbol2))
                   && (actSourceSymbol.equals("") || line.substring(0, 1).equals(actSourceSymbol)))
            {
                actSourceSymbol = line.substring(0, 1);
                line = line.substring(1, line.length());
                counter++;
            }
            if ((inList == -1 && counter != 1) || (inList != -1 && inList + 1 < counter))
            {
                for (int c = 0; c < counter; c++)
                {
                    result.append(actSourceSymbol);
                }
                inList = -1;
            }
            else
            {
                for (int c = 0; c < counter; c++)
                {
                    if (actSourceSymbol.equals(sourceSymbol2))
                    {
                        result.append(sourceSymbol2);
                    }
                    else
                    {
                        result.append(targetSymbol);
                    }
                }
                inList = counter;
            }
            result.append(line);
            if (i < lines.length - 1)
            {
                result.append("\n");
            }
            counter = 0;
        }
        return result.toString();
    }

    private String translateVariables(String result, boolean blogLineBreaks)
    {
        result = result.replace("[{$creolepagefilter.version}]", VAR_VERSION);
        result = result.replace("[{$creolepagefilter.creoleversion}]", VAR_CREOLE_VERSION);
        String linebreaks = blogLineBreaks ? VAR_LINEBREAK_BLOGLIKE : VAR_LINEBREAK_C2LIKE;
        result = result.replace("[{$creolepagefilter.linebreak}]", linebreaks);
        return result;
    }

    /**
     * Undoes the protection. This is done by replacing the md5 hashes by the
     * original markup.
     * 
     * @see #protectMarkup(String)
     */
    private String unprotectMarkup(String content)
    {
        Object[] it = this.hashList.toArray();

        for (int i = it.length - 1; i >= 0; i--)
        {
            String hash = (String) it[i];
            String protectedMarkup = (String) protectionMap.get(hash);
            content = content.replace(hash, protectedMarkup);
            if (protectedMarkup.length() < 3 || (protectedMarkup.length() > 2 && !protectedMarkup.substring(0, 3).equals("{{{")))
                content = translateElement(content, CREOLE_PLUGIN, JSPWIKI_PLUGIN);

        }
        return content;
    }

    /**
     * Protects markup that should not be processed. For now this includes:
     * <ul>
     * <li>Preformatted sections, they should be ignored</li>
     * </li>
     * <li>Protocol strings like <code>http://</code>, they cause problems
     * because of the <code>//</code> which is interpreted as italic</li>
     * </ul>
     * This protection is a simple method to keep the regular expressions for
     * the other markup simple. Internally the protection is done by replacing
     * the protected markup with the the md5 hash of the markup.
     * 
     * @param content
     * @return
     */
    private String protectMarkup(String content)
    {
        protectionMap.clear();
        this.hashList = new ArrayList();
        content = protectMarkup(content, PREFORMATTED_PROTECTED, "", "");
        content = protectMarkup(content, URL_PROTECTED, "", "");
        content = protectMarkup(content, ESCAPE_PROTECTED, "", "");
        content = protectMarkup(content, CREOLE_PLUGIN, "", "");

        // content = protectMarkup(content, LINE_PROTECTED);
        // content = protectMarkup(content, SIGNATURE_PROTECTED);
        return content;
    }

    public ArrayList readPlaceholderProperties(Properties wikiProps)
    {
        Set keySet = wikiProps.keySet();
        Object[] keys = keySet.toArray();
        ArrayList result = new ArrayList();

        for (int i = 0; i < keys.length; i++)
        {
            String key = keys[i] + "";
            String value = wikiProps.getProperty(keys[i] + "");
            if ((key).indexOf("creole.imagePlugin.para.%") > -1)
            {
                String[] pair = new String[2];
                pair[0] = key.replaceAll("creole.imagePlugin.para.%", "");
                pair[1] = value;
                result.add(pair);
            }
        }
        return result;
    }

    public String replaceImageArea(Properties wikiProps, String content, String markupRegex, String replaceContent, int groupPos,
                                   String imagePlugin)
    {
        Matcher matcher = Pattern.compile(markupRegex, Pattern.MULTILINE | Pattern.DOTALL).matcher(content);
        String contentCopy = content;

        ArrayList plProperties = readPlaceholderProperties(wikiProps);

        while (matcher.find())
        {
            String protectedMarkup = matcher.group(0);
            String paramsField = matcher.group(groupPos);
            String paramsString = "";

            if (paramsField != null)
            {
                String[] params = paramsField.split(",");

                for (int i = 0; i < params.length; i++)
                {
                    String param = params[i].replaceAll("\\||\\s", "").toUpperCase();

                    // Replace placeholder params
                    for (int j = 0; j < plProperties.size(); j++)
                    {
                        String[] pair = (String[]) plProperties.get(j);
                        String key = pair[0];
                        String value = pair[1];
                        String code = param.replaceAll("(?i)([0-9]+)" + key, value + "<check>" + "$1" + "</check>");
                        code = code.replaceAll("(.*?)%(.*?)<check>(.*?)</check>", "$1$3$2");
                        if (!code.equals(param))
                            paramsString += code;
                    }

                    // Check if it is a number
                    try
                    {
                        Integer.parseInt(param);
                        paramsString += " width='" + param + "px'";
                    }
                    catch (Exception e)
                    {

                        if (wikiProps.getProperty("creole.imagePlugin.para." + param) != null)
                            paramsString += " "
                                            + wikiProps.getProperty("creole.imagePlugin.para." + param)
                                                .replaceAll("^(\"|')(.*)(\"|')$", "$2");
                    }
                }
            }
            String temp = protectedMarkup;

            protectedMarkup = translateElement(protectedMarkup, markupRegex, replaceContent);
            protectedMarkup = protectedMarkup.replaceAll("\u2015", paramsString);
            protectedMarkup = protectedMarkup.replaceAll("\u2016", imagePlugin);
            protectedMarkup = protectedMarkup.replaceAll("caption=''", "");
            protectedMarkup = protectedMarkup.replaceAll("\\s+", " ");

            int pos = contentCopy.indexOf(temp);
            contentCopy = contentCopy.substring(0, pos) + protectedMarkup
                          + contentCopy.substring(pos + temp.length(), contentCopy.length());
        }
        return contentCopy;
    }

    public String replaceArea(String content, String markupRegex, String replaceSource, String replaceTarget)
    {
        Matcher matcher = Pattern.compile(markupRegex, Pattern.MULTILINE | Pattern.DOTALL).matcher(content);
        String contentCopy = content;

        while (matcher.find())
        {
            String protectedMarkup = matcher.group(0);
            String temp = protectedMarkup;
            protectedMarkup = protectedMarkup.replaceAll(replaceSource, replaceTarget);
            int pos = contentCopy.indexOf(temp);
            contentCopy = contentCopy.substring(0, pos) + protectedMarkup
                          + contentCopy.substring(pos + temp.length(), contentCopy.length());
        }
        return contentCopy;
    }

    /**
     * Protects a specific markup
     * 
     * @see #protectMarkup(String)
     */
    private String protectMarkup(String content, String markupRegex, String replaceSource, String replaceTarget)
    {
        Matcher matcher = Pattern.compile(markupRegex, Pattern.MULTILINE | Pattern.DOTALL).matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find())
        {
            String protectedMarkup = matcher.group();
            protectedMarkup = protectedMarkup.replaceAll(replaceSource, replaceTarget);
            try
            {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.reset();
                digest.update(protectedMarkup.getBytes());
                String hash = bytesToHash(digest.digest());
                matcher.appendReplacement(result, hash);
                protectionMap.put(hash, protectedMarkup);
                this.hashList.add(hash);
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String bytesToHash(byte[] b)
    {
        String hash = "";
        for (int i = 0; i < b.length; i++)
        {
            hash += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return hash;
    }

    private String translateElement(String content, String fromMarkup, String toMarkup)
    {
        Matcher matcher = Pattern.compile(fromMarkup, Pattern.MULTILINE).matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find())
        {
            matcher.appendReplacement(result, toMarkup);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}