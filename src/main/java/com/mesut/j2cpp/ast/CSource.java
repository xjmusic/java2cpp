package com.mesut.j2cpp.ast;

import com.mesut.j2cpp.cppast.CClassImpl;
import com.mesut.j2cpp.util.Helper;

import java.util.ArrayList;
import java.util.List;

public class CSource extends Node {

    public CHeader header;
    public List<String> includes = new ArrayList<>();
    public List<Namespace> usings = new ArrayList<>();//todo header's using instead?
    public List<CField> fieldDefs = new ArrayList<>();
    public List<CMethod> methods = new ArrayList<>();
    public boolean hasRuntime = false;
    public List<CClassImpl> anony = new ArrayList<>();

    public CSource(CHeader header) {
        this.header = header;
        header.source = this;
    }

    //trim type's namespace by usings
    //java::lang::String   using java::lang -> String
    public CType normalizeType(CType type) {
        return Helper.normalizeType(type, usings);
    }

    @Override
    public void print() {
        includePath(header.getInclude());
        println();
        for (Namespace use : usings) {
            print_using(use);
        }
        println();
        printAnony();
        line("");
        printFields();
        line("");
        printMethods();

    }

    void printAnony() {
        if (!anony.isEmpty()) {
            line("//anonymous classes");
            for (CClassImpl impl : anony) {
                append(impl);
            }
        }
    }

    private void printFields() {
        //todo separate by class
        if (!fieldDefs.isEmpty()) {
            line("//static fields");
        }
        for (CField field : fieldDefs) {
            if (field.isStatic() && field.expression != null) {
                line(field.forSource());
            }
        }
    }

    private void printMethods() {
        if (!methods.isEmpty()) {
            line("//methods");
        }
        for (CMethod method : methods) {
            method.printAll(true);
            append(method);
            println();
        }
    }


}
