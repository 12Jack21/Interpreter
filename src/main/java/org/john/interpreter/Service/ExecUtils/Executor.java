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
            sbuf.append("\n");//��ӻ��з�
        }
        if (sbuf.indexOf("\n") != -1)
            sbuf.deleteCharAt(sbuf.lastIndexOf("\n"));//ɾ�����һ�����з�
        // �����ļ���ÿ�������� "-----" ���ָ�
        String[] pros = sbuf.toString().split("-----\n");

        return pros;
    }

     /* all the analysis set here */
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

        Wrapper wrapper = new Wrapper(lexiResult.toString(),astNode,gramParser.getErrorStack());
        return wrapper;
    }

    private static void testProgram() {
        try {
            //��ȡ�ļ�·��
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            System.out.println(prefix);
            FileInputStream fis = new FileInputStream(prefix + "/SampleTest.txt");

            /* д��Txt�ļ� */
            File write = new File(prefix + "/MyOutput.txt"); // ���·�������û����Ҫ����һ���µ�output.txt�ļ�
            BufferedWriter out = new BufferedWriter(new FileWriter(write));

            // �����ļ���ÿ�������� "-----" ���ָ�
            String[] pros = readCodeFile(fis);
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
/*// piece one
   int a[3];
   a = 234;
   b = sdf_234df
-----
   // piece two
   {
        int a = 3<>5;
        real b=-23;
	int a[3] = { 1,3,,b,nono }
	else print    (b);
        if( a>b){ print(a        );
        }
        {
          236 * 1+ 345/(23 * 34) - 2346/ (31- 31) =xx
          }
   }
-----
   // piece three
   {
        int a scan(a);real b =0.234;
	{
	{	print(a_b_c_d_e____1) == 0126.120
        while (a >+234){
              b = b - 1 - a;
              while( b )
		{
                    real c = 123;
                    2343
		= b - 23;
                    b =
		- 14.1238675;
                    print(a)
             }
       }
   }
-----
while(-1+2*3/(4-3)+(+3)-(-2)/(4 * 4 + 2)/(4/4*3)/2.0);
-----
if(1){
  if(b)
     if(c+1){
       if(d==1)
          if(((2==4))) ;
          else break;
     }
  else continue;
}

-----
int k[3]={1,(23+d),1*5==1+1};
int asdf_____2[2];
asdf_____2[1<-1]=12357860;
-----
if (a>b==2+3<>repeat);
-----
if(a)
    b = 3;
-----*/
