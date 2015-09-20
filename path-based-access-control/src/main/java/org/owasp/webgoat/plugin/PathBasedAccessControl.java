
package org.owasp.webgoat.plugin;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.ecs.Element;
import org.apache.ecs.ElementContainer;
import org.apache.ecs.StringElement;
import org.apache.ecs.html.BR;
import org.apache.ecs.html.HR;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TR;
import org.apache.ecs.html.Table;
import org.owasp.webgoat.lessons.Category;
import org.owasp.webgoat.lessons.LessonAdapter;
import org.owasp.webgoat.session.ECSFactory;
import org.owasp.webgoat.session.WebSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


/**
 * ************************************************************************************************
 * <p/>
 * <p/>
 * This file is part of WebGoat, an Open Web Application Security Project utility. For details,
 * please see http://www.owasp.org/
 * <p/>
 * Copyright (c) 2002 - 20014 Bruce Mayhew
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p/>
 * Getting Source ==============
 * <p/>
 * Source for this application is maintained at https://github.com/WebGoat/WebGoat, a repository for free software
 * projects.
 * <p/>
 * For details, please see http://webgoat.github.io
 *
 * @author Bruce Mayhew <a href="http://code.google.com/p/webgoat">WebGoat</a>
 * @created October 28, 2003
 */
public class PathBasedAccessControl extends LessonAdapter {

    private final static String FILE = "File";

    /**
     * Description of the Method
     *
     * @param s Description of the Parameter
     * @return Description of the Return Value
     */
    protected Element createContent(WebSession s) {
        ElementContainer ec = new ElementContainer();

        try {
            Table t = new Table().setCellSpacing(0).setCellPadding(2).setWidth("90%").setAlign("center");

            if (s.isColor()) {
                t.setBorder(1);
            }
            List<File> htmlFiles = findHtmlFiles(getLessonDirectory(s).getParentFile());
            List<String> htmlFilenames = Lists.newArrayList(
                    Iterables.transform(htmlFiles, new Function<File, String>() {
                        @Override
                        public String apply(File input) {
                            return input.getName();
                        }
                    }));
            String[] list = htmlFilenames.toArray(new String[htmlFilenames.size()]);
            String listing = " <p><B>" + getLabelManager().get("CurrentDirectory") + "</B> " + Encoding
                    .urlDecode(htmlFiles.get(0).getParent())
                    + "<br><br>" + getLabelManager().get("ChooseFileToView") + "</p>";

            TR tr = new TR();
            tr.addElement(new TD().setColSpan(2).addElement(new StringElement(listing)));
            t.addElement(tr);

            tr = new TR();
            tr.addElement(new TD().setWidth("35%").addElement(ECSFactory.makePulldown(FILE, list, "", 15)));
            tr.addElement(new TD().addElement(ECSFactory.makeButton(getLabelManager().get("ViewFile"))));
            t.addElement(tr);

            ec.addElement(t);

            // FIXME: would be cool to allow encodings here -- hex, percent,
            // url, etc...
            final String file = s.getParser().getRawParameter(FILE, "");

            // defuse file searching
            boolean illegalCommand = getWebgoatContext().isDefuseOSCommands();
            if (getWebgoatContext().isDefuseOSCommands()) {
                // allow them to look at any file in the webgoat hierachy. Don't
                // allow them
                // to look about the webgoat root, except to see the LICENSE
                // file
                if (upDirCount(file) == 3 && !file.endsWith("LICENSE")) {
                    s.setMessage(getLabelManager().get("AccessDenied"));
                    s.setMessage(getLabelManager().get("ItAppears1"));
                } else
                    if (upDirCount(file) > 3) {
                        s.setMessage(getLabelManager().get("AccessDenied"));
                        s.setMessage(getLabelManager().get("ItAppears2"));
                    } else {
                        illegalCommand = false;
                    }
            }

            // Using the URI supports encoding of the data.
            // We could force the user to use encoded '/'s == %2f to make the lesson more difficult.
            // We url Encode our dir name to avoid problems with special characters in our own path.
            // File f = new File( new URI("file:///" +
            // Encoding.urlEncode(dir).replaceAll("\\\\","/") + "/" +
            // file.replaceAll("\\\\","/")) );
            System.out.println(htmlFilenames.size());
            File f = null;
            for ( File htmlFile : htmlFiles) {
                if (htmlFile.getName().equals(file)) {
                    f = htmlFile;
                }
            }
            if (s.isDebug()) {

                s.setMessage(getLabelManager().get("File") + file);
                s.setMessage(getLabelManager().get("Dir") + f.getParentFile());
                // s.setMessage("File URI: " + "file:///" +
                // (Encoding.urlEncode(dir) + "\\" +
                // Encoding.urlEncode(file)).replaceAll("\\\\","/"));
                s.setMessage(getLabelManager().get("IsFile") + f.isFile());
                s.setMessage(getLabelManager().get("Exists") + f.exists());
            }
            if (!illegalCommand) {
                if (f != null && f.isFile() && f.exists()) {
                    // Don't set completion if they are listing files in the
                    // directory listing we gave them.
                    if (upDirCount(file) >= 1) {
                        s.setMessage(getLabelManager().get("CongratsAccessToFileAllowed"));
                        s.setMessage(" ==> " + Encoding.urlDecode(f.getCanonicalPath()));
                        makeSuccess(s);
                    } else {
                        s.setMessage(getLabelManager().get("FileInAllowedDirectory"));
                        s.setMessage(" ==> " + Encoding.urlDecode(f.getCanonicalPath()));
                    }
                } else
                    if (file != null && file.length() != 0) {
                        s
                                .setMessage(getLabelManager().get("AccessToFileDenied1") + Encoding
                                        .urlDecode(f.getCanonicalPath())
                                        + getLabelManager().get("AccessToFileDenied2"));
                    } else {
                        // do nothing, probably entry screen
                    }

                try {
                    // Show them the file
                    // Strip out some of the extra html from the "help" file
                    ec.addElement(new BR());
                    ec.addElement(new BR());
                    ec.addElement(new HR().setWidth("100%"));
                    ec.addElement(getLabelManager().get("ViewingFile") + f.getCanonicalPath());
                    ec.addElement(new HR().setWidth("100%"));
                    if (f.length() > 80000) {
                        throw new Exception(getLabelManager().get("FileTooLarge"));
                    }
                    String fileData = getFileText(new BufferedReader(new FileReader(f)), false);
                    if (fileData.indexOf(0x00) != -1) {
                        throw new Exception(getLabelManager().get("FileBinary"));
                    }
                    ec.addElement(new StringElement(fileData.replaceAll(System.getProperty("line.separator"), "<br>")
                            .replaceAll("(?s)<!DOCTYPE.*/head>", "").replaceAll("<br><br>", "<br>")
                            .replaceAll("<br>\\s<br>", "<br>").replaceAll("<\\?", "&lt;").replaceAll("<(r|u|t)",
                                    "&lt;$1")));
                } catch (Exception e) {
                    ec.addElement(new BR());
                    ec.addElement(getLabelManager().get("TheFollowingError"));
                    ec.addElement(e.getMessage());
                }
            }
        } catch (Exception e) {
            s.setMessage(getLabelManager().get("ErrorGenerating") + this.getClass().getName());
            e.printStackTrace();
        }

        return (ec);
    }

    private List<File> findHtmlFiles(File start) {
        final List<File> files = Lists.newArrayList();
        start.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {

                if (pathname.isDirectory()) {
                    //stop after 20 files
                    if (files.size() <= 20) {
                        files.addAll(findHtmlFiles(pathname));
                    }
                } else
                    if (pathname.isFile() && pathname.getName().endsWith("html") && pathname.getParentFile().getName()
                            .equals("en") && pathname.getParentFile().getParentFile().getName()
                            .equals("lessonPlans")) {
                        files.add(pathname);
                    }
                return false;
            }
        });
        return files;
    }

    private int upDirCount(String fileName) {
        int count = 0;
        int startIndex = fileName.indexOf("..");
        while (startIndex != -1) {
            count++;
            startIndex = fileName.indexOf("..", startIndex + 1);
        }
        return count;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    protected Category getDefaultCategory() {
        return Category.ACCESS_CONTROL;
    }

    /**
     * Gets the hints attribute of the AccessControlScreen object
     *
     * @return The hints value
     */
    protected List<String> getHints(WebSession s) {
        List<String> hints = new ArrayList<String>();
        hints.add(getLabelManager().get("PathBasedAccessControlHint1"));
        hints.add(getLabelManager().get("PathBasedAccessControlHint2"));
        hints.add(getLabelManager().get("PathBasedAccessControlHint3"));
        hints.add(getLabelManager().get("PathBasedAccessControlHint4"));

        return hints;
    }

    /**
     * Gets the instructions attribute of the WeakAccessControl object
     *
     * @return The instructions value
     */
    public String getInstructions(WebSession s) {
        String instructions = getLabelManager().get("PathBasedAccessControlInstr1") + s
                .getUserName() + getLabelManager().get("PathBasedAccessControlInstr2");

        return (instructions);
    }

    private final static Integer DEFAULT_RANKING = new Integer(115);

    protected Integer getDefaultRanking() {
        return DEFAULT_RANKING;
    }

    /**
     * Gets the title attribute of the AccessControlScreen object
     *
     * @return The title value
     */
    public String getTitle() {
        return ("Bypass a Path Based Access Control Scheme");
    }
}