package com.mesut.j2cpp.ast;

import java.util.ArrayList;
import java.util.List;

public class CHeader extends Node {
    public String name;
    public List<String> includes = new ArrayList<>();
    public List<String> importStar = new ArrayList<>();//import namespace for symbols on the fly(java.util.*)
    public List<CClass> classes = new ArrayList<>();
    public Namespace ns;
    public String rpath;//header path:java/lang/String.h
    boolean hasRuntime = false;

    public CHeader(String path) {
        rpath = path;
    }

    String getPathNoExt() {
        return rpath.substring(0, rpath.length() - 2);
    }

    public void addClass(CClass cc) {
        cc.ns = ns;
        classes.add(cc);
    }

    public void addRuntime() {
        hasRuntime = true;
    }

    public void addInclude(String include) {
        //prevent same header includes itself and other duplications
        if (!include.equals(getPathNoExt()) && !includes.contains(include)) {
            includes.add(include);
        }
    }

    public void addIncludeStar(String include) {
        if (!importStar.contains(include)) {
            importStar.add(include);
        }
    }

    /**
     * make sure type is included
     **/
    public void validate(CType type) {
        for (String inc : includes) {
            int idx = inc.lastIndexOf("/");
            String name = inc.substring(idx + 1, inc.length() - 2);
            if (type.type.equals(name)) {
                return;
            }
        }
        //check asterisk imps
    }

    public void print() {
        append("#pragma once");
        println();
        println();
        for (String imp : includes) {
            include(imp);
        }
        if (hasRuntime) {
            include("JavaRuntime");
        }
        //println();
        //appendln("using namespace java::lang;");
        /*if(ns!=null){
            append("using namespace ").append(ns.all).appendln(";");
        }*/

        for (CClass cc : classes) {
            cc.forHeader = true;
            append(cc);
        }
    }
    
    /*public void printSource(CSource cs){
        cs.header=this;
        cs.print();
    }*/

    public String getInclude() {
        return rpath;
    }


    public boolean isIncluded(String path) {
        for (String str : includes) {
            if (str.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
