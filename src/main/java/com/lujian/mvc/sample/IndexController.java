package com.lujian.mvc.sample;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lujian.mvc.annotation.Controller;
import com.lujian.mvc.annotation.RequestMapping;
import com.lujian.mvc.annotation.RequestParam;

/**
 * @author lj167323@alibaba-inc.com
 */
@Controller
public class IndexController {

    @RequestMapping("/index")
    public void index(HttpServletRequest request, HttpServletResponse response,
        @RequestParam("name") String name) throws IOException {
        response.getWriter().write(name);
    }

}
