package com.mesut.j2cpp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {


    public static void main(String[] args) {

        try {
            /*if (true) {
                resolve();
                return;
            }*/
            Converter converter;
            String srcPath;
            String destPath;
            String cls = "";
            boolean android = false;
            if (android) {
                srcPath = "/storage/extSdCard/asd/dx/dex/src";
                destPath = "/storage/emulated/0/AppProjects/java2cpp/asd/test/cpp";
                //srcPath = "/storage/emulated/0";
                //destPath = srcPath;
                cls = "test.java";
            } else {
                //srcPath="/home/mesut/Desktop/dx-org/src";
                srcPath = "/home/mesut/Desktop/src7";
                //srcPath="/home/mesut/IdeaProjects/java2cpp/asd/test/java";
                //destPath="/home/mesut/Desktop/dx-cpp";
                destPath = "/home/mesut/Desktop/src7-cpp";
                //destPath="/home/mesut/IdeaProjects/java2cpp/asd/test/cpp";

            }
            converter = new Converter(srcPath, destPath);
            //converter.addJar("/home/mesut/Desktop/rt7.jar");
            //converter.addIncludeDir("java/lang");
            //converter.addInclude("java/util");
            //converter.addInclude("java/io");
            //converter.addInclude("java/nio");

            cls = "java/lang/Object.java";

            if (args.length > 0) {
                if (args[0].equals("tree")) {
                    yaml(srcPath, destPath, cls);
                    return;
                }
            }
            converter.makeTable();
            converter.convertSingle(cls);
            //converter.convert();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void yaml(String srcPath, String destPath, String cls) throws IOException {
        String path = srcPath + "/" + cls;
        CompilationUnit cu = StaticJavaParser.parse(new File(path));
        String yaml = new YamlPrinter(true).output(cu);
        File fyml = new File(destPath + "/" + cls.replace(".java", ".yaml"));
        fyml.getParentFile().mkdirs();
        Files.write(Paths.get(fyml.getAbsolutePath()), yaml.getBytes());
        System.out.println(yaml);
    }

    static void resolve() throws FileNotFoundException {
        String dir = "/home/mesut/Desktop/src7/";
        String file = dir + "java/lang/Object.java";

        //TypeSolver typeSolver = new CombinedTypeSolver();
        TypeSolver typeSolver = new JavaParserTypeSolver(dir);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser
                .getConfiguration()
                .setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(new File(file));

        ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getType(0);

        System.out.println(type.resolve().getQualifiedName());

        MethodDeclaration method = type.getMethodsByName("getClass").get(0);
        System.out.println(method.getType().resolve());

        List<ClassOrInterfaceDeclaration> inners = type.getMembers().stream().filter(BodyDeclaration::isClassOrInterfaceDeclaration).map(BodyDeclaration::asClassOrInterfaceDeclaration).collect(Collectors.toList());
        ClassOrInterfaceDeclaration inner = inners.get(0);

        MethodDeclaration m2 = type.getMethodsByName("mymethod").get(0);
        System.out.println(m2.getType().resolve().asReferenceType());
    }
}
