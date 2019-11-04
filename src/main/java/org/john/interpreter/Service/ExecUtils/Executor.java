package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.dto.Wrapper;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.List;

public class Executor {

    private static LexicalAnalysis lexicalAnalysis;
    private static GramParser gramParser;

    public static String[] readCodeFile(InputStream stream) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        while ((line = br.readLine()) != null) {
            sbuf.append(line);
            sbuf.append("\n");//添加换行符
        }
        if (sbuf.indexOf("\n") != -1)
            sbuf.deleteCharAt(sbuf.lastIndexOf("\n"));//删除最后一个换行符
        // 程序文件的每个程序都用 "-----" 来分隔
        String[] pros = sbuf.toString().split("-----\n");

        return pros;
    }

    public static Wrapper analyze(String pro){
        if (!pro.endsWith("\n"))
            pro += "\n";
        List<LexiNode> lexiNodes = LexicalAnalysis.lexicalScan(pro);
        GramParser gramParser = new GramParser();
        ASTNode astNode = gramParser.LLParse(LexicalAnalysis.preprocess(lexiNodes));

        StringBuilder lexiResult = new StringBuilder();
        for (LexiNode node:lexiNodes){
            lexiResult.append(node).append("\n");
        }
        if (lexiResult.lastIndexOf("\n") == lexiResult.length() - 1)
            lexiResult.deleteCharAt(lexiResult.length() - 1);

        JSONObject json = new JSONObject();
        try {
            json = new JSONObject(astNode.toJSON());
            System.out.println(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Wrapper wrapper = new Wrapper(lexiResult.toString(),astNode,gramParser.errorStack);
        return wrapper;
    }

    private static void testProgram() {
        try {
            //获取文件路径
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            System.out.println(prefix);
            FileInputStream fis = new FileInputStream(prefix + "/SampleTest.txt");

            /* 写入Txt文件 */
            File write = new File(prefix + "/MyOutput.txt"); // 相对路径，如果没有则要建立一个新的output.txt文件
            BufferedWriter out = new BufferedWriter(new FileWriter(write));

            // 程序文件的每个程序都用 "-----" 来分隔
            String[] pros = readCodeFile(fis);
            int index = 0;
            List<LexiNode> lexiNodes = null;
            for (String pro : pros) {
                out.write("\n--- " + index++ + " ---\n");
                //进行词法分析得到分析节点集合
                lexiNodes = LexicalAnalysis.lexicalScan(pro + "\0");

                for (LexiNode node : lexiNodes) {
                    out.write(node.toString());
                    out.write("\n");
                }
            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

            GramParser gramParser = new GramParser();
            gramParser.LLParse(LexicalAnalysis.preprocess(lexiNodes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        testProgram();
    }
}
