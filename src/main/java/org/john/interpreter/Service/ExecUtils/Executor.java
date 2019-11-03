package org.john.interpreter.Service.ExecUtils;

import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.List;

public class Executor {

    private static LexicalAnalysis lexicalAnalysis;
    private static GramParser gramParser;

    private static void testProgram() {
        try {
            //��ȡ�ļ�·��
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            System.out.println(prefix);
            FileInputStream fis = new FileInputStream(prefix + "/SampleTest.txt");

            //���ڶ�ȡ����
            StringBuffer sbuf = new StringBuffer();
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            while ((line = br.readLine()) != null) {
                sbuf.append(line);
                sbuf.append("\n");//��ӻ��з�
            }
            sbuf.deleteCharAt(sbuf.lastIndexOf("\n"));//ɾ�����һ�����з�

            /* д��Txt�ļ� */
            File write = new File(prefix + "/MyOutput.txt"); // ���·�������û����Ҫ����һ���µ�output.txt�ļ�
            BufferedWriter out = new BufferedWriter(new FileWriter(write));

            // �����ļ���ÿ�������� "-----" ���ָ�
            String[] pros = sbuf.toString().split("-----\n");
            int index = 0;
            List<LexiNode> lexiNodes = null;
            for (String pro : pros) {
                out.write("\n--- " + index++ + " ---\n");
                //���дʷ������õ������ڵ㼯��
                lexiNodes = LexicalAnalysis.lexicalScan(pro + "\0");

                for (LexiNode node : lexiNodes) {
                    out.write(node.toString());
                    out.write("\n");
                }
            }
            out.flush(); // �ѻ���������ѹ���ļ�
            out.close(); // ���ǵùر��ļ�

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
