package com.mesut.j2cpp.ast;
import java.util.*;

public class Namespace extends Node
{
    public String all;
    public List<String> split=new ArrayList<>();
    
    public Namespace(String ns){
        all=ns.replace(".","::");
    }

    public void pkg(String str){
        int i=0;
        all=str.replace(".","::");
        for(String ns:str.split("::")){
            split.add(ns);
        }
        
    }
    
    public Namespace append(String str){
        
    }
    @Override
    public void print()
    {
        
    }
    
    
    
}
