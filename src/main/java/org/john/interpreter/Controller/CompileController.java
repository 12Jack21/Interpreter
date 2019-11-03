package org.john.interpreter.Controller;

import org.john.interpreter.Service.CompileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
