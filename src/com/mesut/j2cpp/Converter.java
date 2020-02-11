package com.mesut.j2cpp;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.mesut.j2cpp.ast.CHeader;
import com.mesut.j2cpp.ast.CSource;
import com.mesut.j2cpp.visitor.MainVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Converter {

    SymbolTable table;
    Resolver resolver;
    JavaSymbolSolver symbolSolver;
    JavaParser javaParser;
    String srcDir;//source folder
    String destDir;//destinaytion folder for c++ files
    String sysPath;//openjdk sources
    List<UnitMap> units;//parsed sources
    boolean includeAll = false;
    List<PackageName> includeDirs = new ArrayList<>();
    List<String> excludeDirs = new ArrayList<>();
    List<String> includeClasses = new ArrayList<>();
    List<String> excludeClasses = new ArrayList<>();
    //look fist this while resolving
    List<PackageNode> packageHierarchy = new ArrayList<>();

    public Converter(String srcDir, String destDir) {
        this.srcDir = srcDir;
        this.destDir = destDir;
        TypeSolver typeSolver = new JavaParserTypeSolver(srcDir);
        symbolSolver = new JavaSymbolSolver(typeSolver);
        javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(symbolSolver));
        //StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    }

    public void addIncludeDir(String prefix) {
        includeDirs.add(new PackageName(prefix));
    }

    public void addExcludeDir(String prefix) {
        excludeDirs.add(prefix);
    }

    public void addIncludeClass(String name) {
        includeClasses.add(name);
    }

    public void addExcludeClass(String name) {
        excludeClasses.add(name);
    }

    public void setIncludeAll(boolean flag) {
        this.includeAll = flag;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public void fixImports() {

    }

    public void convert() {
        //convertDir(new File(src), "");

        for (UnitMap h : units) {
            String pkg = "";
            if (h.cu.getPackageDeclaration().isPresent()) {
                pkg = h.cu.getPackageDeclaration().get().getNameAsString();
            }
            convertSingle(pkg.replaceAll("\\.", "/") + "/" + h.name, h.cu);
        }
    }

    //
    public void makeTable() {
        table = new SymbolTable();
        resolver = new Resolver(table);
        File dir = new File(srcDir);
        units = new ArrayList<>();

        tableDir(dir, null);
        //System.out.println("total=" + table.list.size());
        /*for (PackageNode node : packageHierarchy) {
            System.out.println(node);
        }*/
        /*for (Symbol s:table.list) {
            System.out.println(s.name+" , "+s.pkg);
        }*/
    }

    //walk in source directory,parse all files and add classes to symbol table
    //useful for converting directory
    void tableDir(File dir, PackageNode node) {
        System.out.println("tabling dir=" + dir);

        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java")) {
                    try {
                        CompilationUnit cu = javaParser.parse(file).getResult().get();
                        units.add(new UnitMap(cu, file.getName()));
                        // cu,pkg,name
                        for (TypeDeclaration<?> type : cu.getTypes()) {
                            if (type.isClassOrInterfaceDeclaration()) {
                                tableClass(type, cu);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                PackageNode sub;
                if (node == null) {
                    sub = new PackageNode(file.getName());
                    packageHierarchy.add(sub);
                } else {
                    sub = node.addSub(file.getName());
                }

                for (PackageName packageName : includeDirs) {
                    if (packageName.isSub(file.getAbsolutePath().substring(srcDir.length() + 1))) {
                        tableDir(file, sub);
                    }
                }

            }
        }
    }

    //add class unit as symbol to table
    void tableClass(TypeDeclaration<?> type, CompilationUnit cu) {
        if (cu.getPackageDeclaration().isPresent()) {
            table.addSymbol(cu.getPackageDeclaration().get().getNameAsString(), type.getNameAsString());
        } else {
            table.addSymbol("", type.getNameAsString());//no package
        }
        type.getMembers().forEach(m -> {
            if (m.isClassOrInterfaceDeclaration()) {
                tableClass(m.asClassOrInterfaceDeclaration(), cu);
            }
        });
    }

    /*public void convertDir(File dir, String pkg) throws FileNotFoundException {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                if (file.getName().endsWith(".java")) {
                    convertSingle(file, pkg,StaticJavaParser.parse(file));
                }
            } else {
                convertDir(file, pkg + "/" + file.getName());
            }
        }
    }*/

    String getPath(CompilationUnit cu) {
        if (cu.getPackageDeclaration().isPresent()) {
            String pkg = cu.getPackageDeclaration().get().getNameAsString();
            return pkg.replace(".", "/");
        }
        return "";
    }

    public void convertSingle(String path, CompilationUnit cu) {
        try {
            System.out.println("converting " + path);

            CHeader header = new CHeader(path.substring(0, path.length() - 4) + "h");
            CSource cpp = new CSource(header);

            header.addIncludeStar("java/lang");//by default as in java compilers
            header.addIncludeStar(getPath(cu));//visible to current package
            MainVisitor visitor = new MainVisitor(this, header);

            cu.accept(visitor, null);

            String hs = header.toString();
            String ss = cpp.toString();
            System.out.println(hs);
            System.out.println("---------------");
            System.out.println(ss);
            File fcpp = new File(destDir, path.replace(".java", ".cpp"));
            fcpp.getParentFile().mkdirs();
            Files.write(Paths.get(fcpp.getAbsolutePath()), ss.getBytes());

            File fh = new File(destDir, path.replace(".java", ".h"));
            Files.write(Paths.get(fh.getAbsolutePath()), hs.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void convertSingle(String cls) throws FileNotFoundException {
        File file = new File(srcDir, cls);
        CompilationUnit unit = javaParser.parse(file).getResult().get();
        for (TypeDeclaration<?> typeDeclaration : unit.getTypes()) {
            tableClass(typeDeclaration, unit);
        }
        convertSingle(cls, unit);
    }

}

