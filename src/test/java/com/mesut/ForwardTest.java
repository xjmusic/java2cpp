package com.mesut;

import com.mesut.j2cpp.ast.CClass;
import com.mesut.j2cpp.ast.CHeader;
import com.mesut.j2cpp.util.BaseClassSorter;
import org.junit.Test;

import java.util.Stack;

public class ForwardTest {

    @Test
    public void name() {
        Stack<String> strings = new Stack<>();
        strings.push("a");
        strings.push("b");
        strings.push("c");

    }

    @Test
    public void test() throws Exception {
        CHeader header = new CHeader("test.h");
        //header.ns=new Namespace("");
        CClass a = makeClass("a");
        CClass b = makeClass("b");
        CClass c = makeClass("c");
        CClass d = makeClass("d");
        header.addClass(a, b, c, d);

        a.addBase(b.getType(), c.getType());
        b.addBase(d.getType());
        c.addBase(d.getType());

        header.getScope(header.classes);

        BaseClassSorter forward = new BaseClassSorter(header);
        forward.sort();
        System.out.println(header);
    }

    CClass makeClass(String name) {
        CClass cc = new CClass();
        cc.name = name;
        return cc;
    }
}