package org.john.interpreter.Controller;

import org.john.interpreter.Service.CompileService;
import org.john.interpreter.Service.ExecUtils.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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

    @RequestMapping("/index/upload")
    @ResponseBody
    public String displayFile(MultipartFile file){
        String s;
        try {
            String[] pros = Executor.readCodeFile(file.getInputStream());
            s = pros[pros.length - 1];
        } catch (IOException e) {
            e.printStackTrace();
            s = e.getMessage();
        }

        return s;
    }
}
