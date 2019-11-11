package org.john.interpreter.Controller;

import org.john.interpreter.Service.CompileService;
import org.john.interpreter.Service.ExecUtils.ASTNode;
import org.john.interpreter.Service.ExecUtils.Executor;
import org.john.interpreter.Service.ExecUtils.GramParser;
import org.john.interpreter.dto.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;

@Controller
public class CompileController {

    @ResponseBody
    @RequestMapping("/index_")
    public String index(){
        return "index";
    }


    @RequestMapping("/index")
    public String indexPage(){
        return "index";
    }

    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    @ResponseBody
    public String displayFile(@RequestParam("code") MultipartFile file){
        String s;
        try {
//            String[] pros = Executor.readCodeFile(file.getInputStream());
//            s = pros[0];
            s = Executor.readCodeFile(file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            s = e.getMessage();
        }
        return s;
    }

    @ResponseBody
    @RequestMapping(value = "/analyze",method = RequestMethod.POST)
    public Wrapper analyzeResult(@RequestParam(value = "codes") String codes,@RequestParam("index")int index){
        Wrapper wrapper = Executor.analyze(codes.split("-----")[index - 1]);
        System.out.println(wrapper);
        return wrapper;
    }
}
