/**
 * Created by snigdhc on 16/2/17.
 **/
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Map;

/**.
 * Calculating the usages of the imported classes under the assumption that no method is called within another method
 **/

@SuppressWarnings("Since15")
class Main {

    private static List<String> parameterList;

    /**
     * Reading the Input File to be parsed
     * @return the file content in a character array
     * @throws IOException
     */

    public static char[] readFile() throws IOException{
        Scanner sc = new Scanner(System.in);
        File file = new File("/home/snigdhc/Projects/JavaParsing/src/main/java/Main.java");
        FileReader fileReader;
        fileReader = new FileReader(file);
        int size = (int) file.length();
        char[] filecontent = new char[size];
        if (fileReader.read(filecontent) == -1) {
            System.out.println("Empty File"); System.exit(0);
        }
        return (filecontent);
    }

    public void settingParsers(ASTParser parser){
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
    }

    public List<String> getImportList(CompilationUnit cu){
        final List<String> imports = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                imports.add(node.getName().toString());
                return super.visit(node);
            }
        });
        //System.out.println(imports);
        return imports;
    }


    public HashMap<String,Integer> termFrequency(MethodDeclaration cu, final HashMap<String, Integer> importedClassCount,
                                                 HashMap<String, String> importedClass, HashMap<String, HashMap<String, Integer>> fullyQualifiedClassNames){
        Block block = cu.getBody();

        block.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleType node) {

                if(importedClassCount.containsKey(node.getName().toString())){
                    int currentCount = (importedClassCount.get(node.getName().toString()));
                    importedClassCount.replace(node.getName().toString(),currentCount,currentCount+1);
                }

                return super.visit(node);
            }
        });
        return(importedClassCount);
    }

    public void traverseBody(final CompilationUnit cu, final Main main, final List<String> imports,
                             final HashMap<String,Integer> classCount){

        cu.accept(new ASTVisitor() {

            Set names = new HashSet();
            public boolean visit(MethodDeclaration node) {


                Block block = node.getBody();
                SimpleName name = node.getName();
                this.names.add(name.getIdentifier());
                //System.out.println(name+"...."+cu.getLineNumber(node.getStartPosition()));

                List b = (node.thrownExceptionTypes());
                for(int i = 0 ; i < b.size() ; i++){
                    String temp = b.get(i).toString();
                    if(classCount.containsKey(temp)) {
                        int value = classCount.get(temp);
                        classCount.replace(temp,value+1);
                    }
                }

                block.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(SimpleType node2) {
//                        System.out.println(node2.getName()+"---"+cu.getLineNumber(node2.getStartPosition()));
                        String la = node2.getName().toString();
                        for (String key: imports) {
                            if(la.equals(key) && classCount.containsKey(la)){
                                int value = classCount.get(key);
                                classCount.replace(key,value+1);
                                //System.out.println(key);//+"  "+classCount.get(key));
                            }
                        }
                        return super.visit(node2);
                    }
                });
//(MethodDeclaration cu, final HashMap<String, Integer> importedClassCount,
//HashMap<String, String> importedClass, HashMap<String, HashMap<String, Integer>> fullyQualifiedClassNames)
//                main.termFrequency(cu,)
                return false;
            }// End of MethodDeclaration Visit

        });

//        System.out.println(classCount);
    }

    public static void main(String[] args) throws IOException {

        final Main main = new Main();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(readFile());
        main.settingParsers(parser);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        final HashMap<String,Integer> importedClasses = new HashMap<>();
        final HashMap<String,HashMap<String,Integer>> fullyQualifiedClassNames = new HashMap<>();
        final List<String> importList = main.getImportList(cu);

        for(String cl : importList){
            HashMap<String,Integer> classNames = new HashMap<>();
            classNames.put(cl.substring(cl.lastIndexOf(".")+1),0);
            importedClasses.put(cl.substring(cl.lastIndexOf(".")+1),0);
            fullyQualifiedClassNames.put(cl,classNames);
        }

//        importList.clear();

        System.out.println("1 "+importedClasses);
        System.out.println("2 "+fullyQualifiedClassNames);
        final List<String> names = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleType node) {
//              System.out.println(node.getName()+"---"+cu.getLineNumber(node.getStartPosition()));
                String nodeName = node.getName().toString();
//                if(fullyQualifiedClassNames)
                for(String keys : importList) {
                    String k = keys.substring(keys.lastIndexOf(".")+1);
                    if(k.equals(nodeName)){
                        names.add(k);
                    }
                }

                if(importedClasses.containsKey(nodeName)){
                    int key1 = importedClasses.get(nodeName);
                    key1 = key1 + 1;
                    importedClasses.replace(nodeName,key1);
                    HashMap<String,Integer> temp = new HashMap<>();
                    temp.put(nodeName,key1);
                    for(HashMap.Entry<String,HashMap<String,Integer>> en : fullyQualifiedClassNames.entrySet()){
                        String key2 = en.getKey().toString();
                        String className = key2.substring(key2.lastIndexOf(".")+1);
                        if(nodeName.equals(className)){
                            fullyQualifiedClassNames.replace(key2,temp);
                        }
                    }
                }
                return super.visit(node);
            }
        });

        for(String a: importList){
            System.out.println(a);
        }
        System.out.println("3 "+importList);
        System.out.println("4 "+importedClasses);
        System.out.println("5 "+fullyQualifiedClassNames);

//        for(HashMap.Entry<String,Integer> innerHm: importedClasses.entrySet()){
//            String className = innerHm.getKey();
//            int value = innerHm.getValue();
//            System.out.println(className+" "+value);
//        }

//        for(HashMap.Entry<String,HashMap<String,Integer>> outerHm: fullyQualifiedClassNames.entrySet()){
//            String fullyQualifiedName = outerHm.getKey();
//            for(HashMap.Entry<String,Integer> innerHm: importedClasses.entrySet()){
//                String className = innerHm.getKey();
//                int value = innerHm.getValue();
//                if(fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".")+1).equals(className))
//                    System.out.println(className+" "+value);
//                outerHm
//            }
//        }

//        System.out.println(importedClasses);

//        for(Map.Entry<String,Integer> entry : importedClasses.entrySet()){
//            String key = entry.getKey();
//            importList.add(key);
//        }

//        cu.accept(new ASTVisitor() {
//            @Override
//            public boolean visit(MethodDeclaration node) {
//                parameterList = node.parameters();
//                System.out.println(node.parameters().contains("HashMap"));
//                System.out.println(parameterList);
//                return super.visit(node);
//            }
//        });

//        cu.accept(new ASTVisitor() {
//            @Override
//            public boolean visit(ParameterizedType node) {
////                System.out.println(node.getType()+" ---> "+cu.getLineNumber(node.getStartPosition()));
////                System.out.println(node.typeArguments());
//                System.out.println(node.toString());
//                return super.visit(node);
//            }
//        });

        // USE SimpleType visit and remove the types found in the parameters


        main.traverseBody(cu,main,importList,importedClasses);
    }
}
