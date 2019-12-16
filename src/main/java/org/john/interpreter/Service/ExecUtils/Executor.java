package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.dto.Wrapper;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Executor {

    private static LexicalAnalysis lexicalAnalysis;
    private static GramParser gramParser;

    public static String readCodeFile(InputStream stream) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        while ((line = br.readLine()) != null) {
            sbuf.append(line);
            sbuf.append("\n");//添加换行符
        }
        if (sbuf.indexOf("\n") != -1)
            sbuf.deleteCharAt(sbuf.lastIndexOf("\n"));//删除最后一个换行符
        return sbuf.toString();
    }

    /* all the analysis set here */
    public static Wrapper analyze(String pro, String scans) {
        if (!pro.endsWith("\n"))
            pro += "\n";
        List<LexiNode> lexiNodes = LexicalAnalysis.lexicalScan(pro);
        GramParser gramParser = new GramParser();
        ASTNode astNode = gramParser.LLParse(LexicalAnalysis.preprocess(lexiNodes));

        StringBuilder lexiResult = new StringBuilder();
        for (LexiNode node : lexiNodes) {
            lexiResult.append(node).append("\n");
        }
        if (lexiResult.lastIndexOf("\n") == lexiResult.length() - 1)
            lexiResult.deleteCharAt(lexiResult.length() - 1);

        Translator t = new Translator();
        Wrapper wrapper;
        try { //TODO 考虑把语法分析也加入这里
            if (astNode != null) {
                t.setScanList(new LinkedList<>(splitScanString(scans)));
                t.translate(astNode);
                // 语义分析前 不能执行
                astNode.addNullTips();
                astNode.setParentNull();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wrapper = new Wrapper(lexiResult.toString(), astNode, gramParser.getErrorStack(), t.getMessages(), t.getPrintList());
        }

        return wrapper;
    }

    private static void testProgram() {
        try {
            //获取文件路径
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            System.out.println(prefix);
            FileInputStream fis = new FileInputStream(prefix + "/Grammar_Test.txt");

            /* 写入Txt文件 */
            File write = new File(prefix + "/My.txt"); // 相对路径，如果没有则要建立一个新的output.txt文件
            BufferedWriter out = new BufferedWriter(new FileWriter(write));

            // 程序文件的每个程序都用 "-----" 来分隔
            String[] pros = readCodeFile(fis).split("-----");
            int index = 0;
            List<LexiNode> lexiNodes = null;

//            for (String pro : pros) {
//                out.write("\n--- " + index++ + " ---\n");
//                //进行词法分析得到分析节点集合
//                lexiNodes = LexicalAnalysis.lexicalScan(pro + "\0");
//
//                for (LexiNode node : lexiNodes) {
//                    out.write(node.toString());
//                    out.write("\n");
//                }
//            }

            Wrapper w = analyze(pros[0], "12");
            for (String msg : w.getMessages()) {
                out.write(msg + "\n");
                System.out.println(msg);
            }

            System.out.println("\nprint 输出信息如下：");
            for (String m : w.getOutputList()) {
                System.out.println(m);
            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

//            GramParser gramParser = new GramParser();
//            gramParser.LLParse(LexicalAnalysis.preprocess(lexiNodes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 后端调试时使用
    private static void testOnBack() {
        try {
            //获取文件路径
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            System.out.println(prefix);
            FileInputStream fis = new FileInputStream(prefix + "/Grammar_Test.txt");
            FileInputStream scanInputs = new FileInputStream(prefix + "/scan_input.txt");

            /* 写入Txt文件 */
            File write = new File(prefix + "/My.txt"); // 相对路径，如果没有则要建立一个新的output.txt文件
            BufferedWriter out = new BufferedWriter(new FileWriter(write));

            // 程序文件的每个程序都用 "-----" 来分隔
//            String[] pros = readCodeFile(fis).split("-----");

            List<LexiNode> lexiNodes = LexicalAnalysis.lexicalScan(readCodeFile(fis));

            GramParser parser = new GramParser();
            ASTNode astNode = parser.LLParse(LexicalAnalysis.preprocess(lexiNodes));

            Translator t = new Translator();
            LinkedList<String> scanList = new LinkedList<>(splitScanString(readCodeFile(scanInputs)));
            t.setScanList(scanList);  // 注入输入的数据
            t.translate(astNode);
            System.out.println("--------msg-------");
            for (String m : t.getMessages())
                System.out.println(m);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> splitScanString(String scans) {
        // 换行符 和 空格 分割
        ArrayList<String> inputs = new ArrayList<>();
        List<String> tmp = new ArrayList<>(Arrays.asList(scans.split("\n")));
        List<String> t;
        for (String input : tmp) {
            t = Arrays.asList(input.split(" "));
            for (String s : t) {
                if (s.length() != 0)
                    inputs.add(s);
            }
        }
        return inputs;
    }

    public static void main(String[] args) {
//        testProgram();
        testOnBack();
    }
}