package com.mesut.j2cpp.ast;
import java.io.*;

public class CParameter extends Node
{
    public TypeName type;
    public String name;
    //public boolean isPointer=true;

    public void print()
    {
        list.clear();
        append(type.toString().replace(".","::"));//normalize the type(base::type)
        if(type.isPointer()&&!type.isArray()){
            append("*");
        }
        append(" ");
        append(name);
    }
    
    
}
