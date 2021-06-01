package com.mesut.j2cpp;


import com.mesut.j2cpp.ast.*;
import com.mesut.j2cpp.cppast.expr.CAssignment;
import com.mesut.j2cpp.cppast.expr.CFieldAccess;
import com.mesut.j2cpp.cppast.expr.CThisExpression;
import com.mesut.j2cpp.cppast.stmt.CBlockStatement;
import com.mesut.j2cpp.cppast.stmt.CExpressionStatement;
import com.mesut.j2cpp.map.ClassMap;
import com.mesut.j2cpp.util.*;
import com.mesut.j2cpp.visitor.DeclarationVisitor;
import com.mesut.j2cpp.visitor.PreVisitor;
import com.mesut.j2cpp.visitor.SourceVisitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Converter {

    public List<String> classpath = new ArrayList<>();
    public CMakeWriter cMakeWriter;
    public CMakeWriter.Target target;
    Path srcDir;//source folder
    Path destDir;//destination folder for c++ files
    Path headerDir;
    Filter filter;
    ASTParser parser;
    List<String> sourceList;
    int count = 0;
    CHeader forwardHeader;

    public Converter(String srcDir, String destDir) {
        this.srcDir = Paths.get(srcDir);
        this.destDir = Paths.get(destDir);
        filter = new Filter(this.srcDir);
        forwardHeader = new CHeader("common.h");
        forwardHeader.forwardDeclarator = new ForwardDeclarator(ClassMap.sourceMap);
        try {
            Logger.init(this.destDir.resolve( "log.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Filter getFilter() {
        return filter;
    }

    //jar or dir
    public void addClasspath(String path) {
        classpath.add(path);
    }

    @SuppressWarnings("rawtypes,unchecked")
    public void initParser() {
        parser = ASTParser.newParser(AST.JLS13);
        List<String> cpDirs = new ArrayList<>();
        List<String> cpJars = new ArrayList<>();

        for (String path : classpath) {
            if (path.endsWith(".jar")) {
                cpJars.add(path);
            }
            else {
                cpDirs.add(path);
            }
        }
        cpDirs.add(srcDir.toString());
        parser.setEnvironment(cpJars.toArray(new String[0]), cpDirs.toArray(new String[0]), null, true);

        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);

        Map options = JavaCore.getOptions();
        String ver = JavaCore.VERSION_13;
        options.put(JavaCore.COMPILER_COMPLIANCE, ver);
        options.put(JavaCore.COMPILER_SOURCE, ver);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, ver);
        //options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES,"true");
        parser.setCompilerOptions(options);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
    }

    void initCmake() {
        cMakeWriter = new CMakeWriter("myproject");
        cMakeWriter.sourceDir = destDir.toString();
        target = cMakeWriter.addTarget("mylib", false);
        target.addInclude(destDir.toString());
        target.addInclude(headerDir.toString());
        target.addInclude("lib");
    }

    public void convert() {
        try {
            headerDir = Config.separateInclude ? destDir.resolve("include") : destDir;
            Files.createDirectories(headerDir);
            initCmake();
            initMain();
            preVisitDir();
            convertDir();
            writeCmake();
            writeForwards();
            if (Logger.hasErrors) {
                System.err.println("conversion has errors check logs");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("conversion done for " + count + " files");
    }

    void initMain() {
        CClass cc;
        if (Config.mainClass != null) {
            CType type = new CType(Config.mainClass);
            type.fromSource = true;
            cc = new CClass(type);
        }
        else {
            cc = new CClass();
            cc.name = "main";
        }
        //si_init
        CMethod method = new CMethod();
        cc.addMethod(method);
        method.body = new CBlockStatement();
        method.name = new CName("si_init");
        method.type = TypeHelper.getVoidType();
        method.setStatic(true);
        method.setPublic(true);
        ClassMap.sourceMap.mainClass = cc;
    }

    void writeForwards() throws IOException {
        if (Config.common_forwards) {
            if (Config.include_common_forwards) {
                forwardHeader.includes.add("lib/lib_common.h");
            }
            System.out.println("wrote " + forwardHeader.getInclude());
            Util.writeHeader(forwardHeader, headerDir);
        }
        if (Config.writeLibHeader) {
            LibHandler.instance.writeAll(Paths.get(headerDir.toString(), "lib"));
        }
    }

    private void convertDir() throws IOException {
        initParser();
        String[] b = new String[sourceList.size()];
        Arrays.fill(b, "");
        parser.createASTs(sourceList.toArray(new String[0]), null, b, new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                convertSingle(Paths.get(sourceFilePath), ast);
            }
        }, null);
    }

    private void preVisitDir() throws IOException {
        sourceList = new ArrayList<>();
        collect();
        System.out.println("total of " + sourceList.size() + " files");
        initParser();
        String[] b = new String[sourceList.size()];
        Arrays.fill(b, "");
        parser.createASTs(sourceList.toArray(new String[0]), null, b, new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                preVisit(ast);
            }
        }, null);
        System.out.println("pre visit done");
    }

    void collect() throws IOException {
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java") && filter.checkPath(file)) {
                    sourceList.add(file.toString());
                }
                return super.visitFile(file, attrs);
            }
        });
    }

    //create class-member model
    void preVisit(CompilationUnit cu) {
        PreVisitor visitor = new PreVisitor();
        visitor.handle(cu);
    }

    private void processFields(CClass clazz, CSource source) {
        for (CField field : clazz.fields) {
            if (field.expression == null || field.is(ModifierNode.CONSTEXPR_NAME)) continue;
            if (field.isStatic()) {
                //normal static field or enum constant
                source.fieldDefs.add(field);
            }
            else if (Config.fields_in_constructors) {
                //add to all cons
                //make statement
                CAssignment assignment = new CAssignment();
                assignment.left = new CFieldAccess(new CThisExpression(), field.name, true);
                assignment.right = field.expression;
                assignment.operator = "=";
                clazz.consStatements.add(new CExpressionStatement(assignment));
            }
            else {
                //header has it
            }
        }
    }

    //convert bodies
    public void convertSingle(Path path, CompilationUnit cu) {
        try {
            Path relativePath = srcDir.relativize(path);
            //relativePath = Util.trimPrefix(relativePath, "/");
            System.out.println("converting " + relativePath);
            CSource source = new CSource();
            source.name = Util.trimSuffix(relativePath.toString(), ".java") + ".cpp";

            SourceVisitor sourceVisitor = new SourceVisitor(source);
            DeclarationVisitor headerVisitor = new DeclarationVisitor(sourceVisitor);
            headerVisitor.convert(cu);
            //handle fields
            List<CClass> classes = headerVisitor.classes;
            for (CClass cc : classes) {
                processFields(cc, source);
            }
            source.classes.addAll(classes);
            Namespace ns = headerVisitor.ns;

            source.usings.add(ns);

            //create headers
            for (CClass cc : classes) {
                cc.ns = ns;
                CHeader header = new CHeader(cc.getHeaderPath());
                header.setNs(ns);
                header.setClass(cc);
                source.includes.add(IncludeStmt.src(header.getInclude()));
                new HeaderDeps(header).handle();
                writeHeader(header);
            }

            if (Config.common_forwards) {
                forwardHeader.forwardDeclarator.addAll(classes);
                if (Config.include_common_forwards) {
                    source.includes.add(0, IncludeStmt.src(forwardHeader.getInclude()));
                }
            }
            handleDeps(source);
            Util.writeSource(source, destDir);

            target.sourceFiles.add(source.name);
            count++;
        } catch (Exception e) {
            System.err.println("cant convert " + path);
            Logger.log(path + ":" + e.getMessage());
            e.printStackTrace();
        }
    }

    void handleDeps(CSource source) {
        Set<CClass> all = new HashSet<>();
        List<CHeader> headers = new ArrayList<>();
        for (CClass cc : source.classes) {
            headers.add(cc.header);
        }
        for (CClass cc : source.classes) {
            for (CType type : cc.types) {
                CClass t = ClassMap.sourceMap.get(type);
                if (t == null) continue;
                //prevent header types being included again
                if (source.includes.has(t) || cc.header.includes.has(t)) continue;
                boolean flag = true;
                for (CHeader header : headers) {
                    if (header.includes.has(t)) {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                    all.add(t);
            }
        }
        try {
            List<CClass> list = new ArrayList<>(all);
            BaseClassSorter.sort(list);
            for (CClass cc : list) {
                source.includes.add(cc.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeHeader(CHeader header) throws IOException {
        Util.writeHeader(header, headerDir);
    }

    public void writeCmake() throws IOException {
        target.addInclude("lib");
        String src = cMakeWriter.generate();
        Path file = destDir.resolve("CMakeLists.txt");
        FileWriter writer = new FileWriter(file.toString());
        writer.write(src);
        writer.close();
        System.out.println("cmake generation done");
    }

}


